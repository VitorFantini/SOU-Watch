"""Read-only, auditable SOU feed monitor. Python 3.10+, standard library only."""
from __future__ import annotations
import argparse
import csv
import hashlib
import html
import json
import logging
import math
import os
from pathlib import Path
import signal
import sqlite3
import statistics
import subprocess
import xml.etree.ElementTree as ET
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zlib
from datetime import datetime, timedelta, timezone, time as daytime
from zoneinfo import ZoneInfo

HERE = Path(__file__).resolve().parent
BASE = "https://ss7u5urlxs.singularcdn.net.br/api/"
HEADERS = {"User-Agent": "Dart/3.9 (dart:io)", "x-mob-app_name": "SOU", "x-mob-project-id": "317"}
SCHEMA = """
CREATE TABLE IF NOT EXISTS cycles(id INTEGER PRIMARY KEY, started REAL NOT NULL, ended REAL, status TEXT);
CREATE TABLE IF NOT EXISTS requests(id INTEGER PRIMARY KEY, cycle INTEGER, captured REAL NOT NULL, line TEXT, endpoint TEXT, url TEXT, http_status INTEGER, elapsed_ms REAL, ok INTEGER NOT NULL, error TEXT, sha256 TEXT, payload_zlib BLOB);
CREATE TABLE IF NOT EXISTS route_checks(id INTEGER PRIMARY KEY, cycle INTEGER, captured REAL NOT NULL, line TEXT, status TEXT, trips INTEGER, message TEXT);
CREATE TABLE IF NOT EXISTS vehicles(id INTEGER PRIMARY KEY, request_id INTEGER, captured REAL NOT NULL, line TEXT, trip_id TEXT, direction INTEGER, vehicle_id TEXT, departure TEXT, gps_time TEXT, gps_age REAL, lat REAL, lng REAL, state TEXT, association TEXT);
CREATE TABLE IF NOT EXISTS references_manual(id INTEGER PRIMARY KEY, captured REAL NOT NULL, source TEXT NOT NULL, line TEXT, vehicle_id TEXT, departure TEXT, stop_id TEXT, eta_minutes REAL, lat REAL, lng REAL, note TEXT);
CREATE TABLE IF NOT EXISTS app_tests(id INTEGER PRIMARY KEY, started REAL NOT NULL, ended REAL NOT NULL, status TEXT, tests INTEGER, failures INTEGER, skipped INTEGER, log_path TEXT, detail TEXT);
CREATE INDEX IF NOT EXISTS req_time ON requests(captured);
CREATE INDEX IF NOT EXISTS gps_time ON vehicles(captured);
CREATE INDEX IF NOT EXISTS ref_time ON references_manual(captured);
"""

def connect_db(path: Path):
    db = sqlite3.connect(path, timeout=30)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA journal_mode=WAL")
    db.executescript(SCHEMA)
    return db


def minute(value):
    try:
        parts = str(value).split(" ")[0].split(":")
        hour, minutes = int(parts[0]), int(parts[1])
        return hour * 60 + minutes if 0 <= hour < 48 and 0 <= minutes < 60 else None
    except (ValueError, IndexError, TypeError):
        return None


def gps_age(value, now, zone):
    try:
        local = datetime.fromtimestamp(now, zone)
        parsed = daytime.fromisoformat(value)
        if parsed.tzinfo is not None:
            return None
        stamp = datetime.combine(local.date(), parsed, zone)
        if stamp.timestamp() > now + 60:
            stamp -= timedelta(days=1)
        return max(0, now - stamp.timestamp())
    except (TypeError, ValueError):
        return None


def valid_coord(lat, lng):
    return (isinstance(lat, (float, int)) and not isinstance(lat, bool)
            and isinstance(lng, (float, int)) and not isinstance(lng, bool)
            and math.isfinite(lat) and math.isfinite(lng) and -90 <= lat <= 90 and -180 <= lng <= 180)


def active_trips(payload, now, zone):
    """Match service day, direction and departure seq, as in the Android app."""
    table = payload.get("timetable")
    if not isinstance(table, dict) or not isinstance(table.get("trips"), list) or not isinstance(table.get("directions"), list):
        raise ValueError("Tabela sem trips/directions válidos")
    today = datetime.fromtimestamp(now, zone).date()
    if table.get("exceptions"):
        for item in table["exceptions"]:
            if not isinstance(item, dict) or not item.get("date") or item["date"] == str(today):
                raise ValueError("Calendário especial: associação não confirmada")
    # API uses Sunday at index 0.
    weekday = (today.weekday() + 1) % 7
    departures = {}
    for direction in table["directions"]:
        for service in direction.get("services", []):
            days = service.get("days", [])
            if len(days) != 7 or not days[weekday]:
                continue
            if service.get("start") and service["start"] > str(today):
                continue
            if service.get("end") and service["end"] < str(today):
                continue
            for dep in service.get("departures", []):
                m = minute(dep.get("dep"))
                if m is not None:
                    departures.setdefault((direction.get("directionId"), dep.get("seq")), set()).add(m)
    result = {}
    for trip in table["trips"]:
        key = (trip.get("directionId"), trip.get("seq"))
        if departures.get(key):
            result[str(trip["tripId"])] = (trip.get("directionId"), departures[key])
    return result


class Monitor:
    def __init__(self, config, out):
        self.cfg, self.out = config, out
        self.zone = ZoneInfo(config["timezone"])
        self.db = connect_db(out / "dados.sqlite3")
        self.cache = {}
        self.last_request = 0.0
        self.backoff_until = 0.0
        self.failures = 0

    def request(self, cycle, line, endpoint, params):
        delay = max(self.cfg["request_gap_seconds"] - (time.monotonic() - self.last_request), self.backoff_until - time.monotonic())
        if delay > 0:
            time.sleep(delay)
        url = BASE + endpoint + "?" + urllib.parse.urlencode({"project_id": "317", **params})
        stamp = time.time()
        start = time.monotonic()
        raw, code, error, payload = b"", None, None, None
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=self.cfg["request_timeout_seconds"]) as response:
                code = response.status
                raw = response.read(4_000_001)
            if len(raw) > 4_000_000:
                raise ValueError("Resposta excedeu limite de 4 MB")
            payload = json.loads(raw)
            if endpoint == "timetable":
                if not isinstance(payload, dict):
                    raise ValueError("Tabela não é objeto JSON")
                if not isinstance(payload.get("timetable"), dict):
                    raise ValueError("Resposta sem tabela horária")
            elif not isinstance(payload, list):
                raise ValueError("Resposta de veículos não é lista JSON")
        except urllib.error.HTTPError as exc:
            code = exc.code
            raw = exc.read(4_000_000)
            error = f"HTTP {code}"
        except Exception as exc:
            error = f"{type(exc).__name__}: {exc}"
        elapsed = (time.monotonic() - start) * 1000
        self.last_request = time.monotonic()
        ok = error is None
        if not ok:
            self.failures += 1
            self.backoff_until = time.monotonic() + min(300, 2 ** min(self.failures, 8))
            logging.warning("%s %s: %s", line, endpoint, error)
        else:
            self.failures, self.backoff_until = 0, 0
        cur = self.db.execute("INSERT INTO requests(cycle,captured,line,endpoint,url,http_status,elapsed_ms,ok,error,sha256,payload_zlib) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                              (cycle, stamp, line, endpoint, url, code, elapsed, int(ok), error,
                               hashlib.sha256(raw).hexdigest(), zlib.compress(raw)))
        self.db.commit()
        return (payload if ok else None), cur.lastrowid

    def cycle(self):
        start = time.time()
        cycle = self.db.execute("INSERT INTO cycles(started,status) VALUES(?,?)", (start, "running")).lastrowid
        self.db.commit()
        status = "ok"
        try:
            for line, route in self.cfg["routes"].items():
                now = time.time()
                cached = self.cache.get(line)
                current_day = datetime.fromtimestamp(now, self.zone).date()
                if cached and cached[2] == current_day and now - cached[0] < self.cfg["timetable_cache_seconds"]:
                    payload = cached[1]
                else:
                    payload, _ = self.request(cycle, line, "timetable", {"v": "2", "route_id": route})
                    if payload is not None:
                        self.cache[line] = (time.time(), payload, current_day)
                if payload is None:
                    self.db.execute("INSERT INTO route_checks(cycle,captured,line,status,trips,message) VALUES(?,?,?,?,?,?)", (cycle, now, line, "consulta_falhou", 0, "Sem tabela válida"))
                    status = "partial"
                    continue
                try:
                    trips = active_trips(payload, time.time(), self.zone)
                except Exception as exc:
                    self.db.execute("INSERT INTO route_checks(cycle,captured,line,status,trips,message) VALUES(?,?,?,?,?,?)", (cycle, now, line, "calendario_ou_schema", 0, str(exc)))
                    status = "partial"
                    continue
                self.db.execute("INSERT INTO route_checks(cycle,captured,line,status,trips,message) VALUES(?,?,?,?,?,?)", (cycle, now, line, "ok" if trips else "sem_servico_programado", len(trips), ""))
                for trip_id, (direction, departures) in trips.items():
                    vehicles, request_id = self.request(cycle, line, "vehicles", {"v": "1", "trip_id": trip_id})
                    if vehicles is None:
                        status = "partial"
                        continue
                    captured = time.time()
                    # Empty successful list is deliberately NOT recorded as an API failure.
                    for vehicle in vehicles:
                        if not isinstance(vehicle, dict):
                            status = "partial"
                            self.db.execute("INSERT INTO route_checks(cycle,captured,line,status,trips,message) VALUES(?,?,?,?,?,?)", (cycle, captured, line, "schema_veiculo", 1, "Item de veículo não é objeto"))
                            continue
                        lat = vehicle.get("lat", vehicle.get("latitude"))
                        lng = vehicle.get("lng", vehicle.get("longitude", vehicle.get("lon")))
                        age = gps_age(vehicle.get("positionTime"), captured, self.zone)
                        state = "gps_invalido" if not valid_coord(lat, lng) else "gps_sem_hora" if age is None else "gps_antigo" if age > self.cfg["gps_max_age_seconds"] else "gps_recente"
                        association = "associado" if vehicle.get("vehicleId") is not None and minute(vehicle.get("startTime")) in departures else "sem_associacao"
                        self.db.execute("INSERT INTO vehicles(request_id,captured,line,trip_id,direction,vehicle_id,departure,gps_time,gps_age,lat,lng,state,association) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", (request_id, captured, line, trip_id, direction, str(vehicle.get("vehicleId", "")), vehicle.get("startTime"), vehicle.get("positionTime"), age, lat if valid_coord(lat,lng) else None, lng if valid_coord(lat,lng) else None, state, association))
                self.db.commit()
        except BaseException:
            status = "interrupted"
            raise
        finally:
            self.db.execute("UPDATE cycles SET ended=?,status=? WHERE id=?", (time.time(), status, cycle))
            self.db.commit()
        logging.info("Ciclo %s concluído: %s", cycle, status)
        return cycle


def test_app_if_due(db, cfg, out):
    interval = cfg.get("app_test_interval_seconds", 0)
    if not interval:
        return
    latest = db.execute("SELECT MAX(started) FROM app_tests").fetchone()[0]
    if latest is not None and time.time() - latest < interval:
        return
    started = time.time()
    log = out / f"testes-app-{int(started)}.log"
    status, tests, failures, skipped, detail = "infrastructure_error", 0, 0, 0, ""
    try:
        env = os.environ.copy()
        jdk = Path.home()/".jdks/jbr-21.0.11"
        if not env.get("JAVA_HOME") and jdk.exists():
            env["JAVA_HOME"] = str(jdk)
        # Gradle is a trusted Windows batch entrypoint; keep arguments constant.
        command = subprocess.list2cmdline([str(HERE.parent/"gradlew.bat"), ":app:testDebugUnitTest", "--offline", "--rerun-tasks", "--console=plain"])
        # Child output goes to a persistent log, not into RAM.
        with log.open("w", encoding="utf-8") as output:
            result = subprocess.run(command, shell=True, cwd=HERE.parent, env=env, stdout=output, stderr=subprocess.STDOUT, timeout=600)
        suites = []
        for path in (HERE.parent/"app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"):
            if path.stat().st_mtime >= started - 2:
                suites.append(ET.parse(path).getroot())
        tests = sum(int(x.get("tests", 0)) for x in suites)
        failures = sum(int(x.get("failures", 0))+int(x.get("errors", 0)) for x in suites)
        skipped = sum(int(x.get("skipped", 0)) for x in suites)
        if failures:
            status = "failed"
            detail = "Teste JVM reprovado; consulte o XML e o log"
        elif result.returncode != 0:
            status = "infrastructure_error"
            detail = f"Gradle terminou com código {result.returncode}, sem falha de teste comprovada; consulte o log"
        else:
            status = "passed" if tests > 0 else "no_fresh_results"
            detail = "Testes JVM da lógica Kotlin; não exercitam tela, rede real ou bateria"
    except Exception as exc:
        detail = f"{type(exc).__name__}: {exc}"
    db.execute("INSERT INTO app_tests(started,ended,status,tests,failures,skipped,log_path,detail) VALUES(?,?,?,?,?,?,?,?)", (started,time.time(),status,tests,failures,skipped,str(log),detail))
    db.commit()
    logging.info("Testes do app: %s; %s testes; %s falhas", status, tests, failures)


def percentile(values, p):
    return sorted(values)[max(0, math.ceil(len(values) * p) - 1)] if values else None


def pair_references(rows, tolerance=120):
    """One-to-one pairs; same line, vehicle, departure, stop, and close timestamps."""
    official = [r for r in rows if r["source"] == "sou"]
    watch = [r for r in rows if r["source"] == "souwatch"]
    candidates = []
    for a in official:
        for b in watch:
            if all(a[k] == b[k] for k in ("line", "vehicle_id", "departure", "stop_id")) and abs(a["captured"] - b["captured"]) <= tolerance:
                candidates.append((abs(a["captured"]-b["captured"]), a, b))
    used_a, used_b, pairs = set(), set(), []
    for _, a, b in sorted(candidates, key=lambda item: (item[0], item[1]["id"], item[2]["id"])):
        if a["id"] not in used_a and b["id"] not in used_b:
            used_a.add(a["id"]); used_b.add(b["id"]); pairs.append((a, b))
    return pairs


def field_errors(rows):
    """Compare predicted arrival timestamps with the first later recorded actual passage."""
    arrivals = [r for r in rows if r["source"] == "campo"]
    result = []
    for pred in rows:
        if pred["source"] not in ("sou", "souwatch") or pred["eta_minutes"] is None:
            continue
        matches = [a for a in arrivals if all(a[k] == pred[k] for k in ("line", "vehicle_id", "departure", "stop_id")) and pred["captured"] <= a["captured"] <= pred["captured"] + 4*3600]
        if matches:
            actual = min(matches, key=lambda a: a["captured"])
            error = pred["captured"] + pred["eta_minutes"]*60 - actual["captured"]
            result.append({"source": pred["source"], "prediction_id": pred["id"], "arrival_id": actual["id"], "error_minutes": error/60})
    return result


def report(db, cfg, out, day):
    zone = ZoneInfo(cfg["timezone"])
    start = datetime.fromisoformat(day).replace(tzinfo=zone)
    end = start + timedelta(days=1)
    lo, hi = start.timestamp(), end.timestamp()
    req = [dict(r) for r in db.execute("SELECT * FROM requests WHERE captured>=? AND captured<? ORDER BY captured", (lo,hi))]
    obs = [dict(r) for r in db.execute("SELECT * FROM vehicles WHERE captured>=? AND captured<? ORDER BY captured", (lo,hi))]
    cycles = [dict(r) for r in db.execute("SELECT * FROM cycles WHERE started>=? AND started<? ORDER BY started", (lo,hi))]
    refs = [dict(r) for r in db.execute("SELECT * FROM references_manual WHERE captured>=? AND captured<? ORDER BY captured", (lo,hi))]
    checks = [dict(r) for r in db.execute("SELECT * FROM route_checks WHERE captured>=? AND captured<? ORDER BY captured", (lo,hi))]
    # Include cross-midnight predictions/arrivals in matching, attributing errors to prediction day.
    all_refs = [dict(r) for r in db.execute("SELECT * FROM references_manual WHERE captured>=? AND captured<? ORDER BY captured", (lo-4*3600,hi+4*3600))]
    pairs = [(a,b) for a,b in pair_references(all_refs) if lo<=a["captured"]<hi]
    errors = [e for e in field_errors(all_refs) if any(r["id"]==e["prediction_id"] for r in refs)]
    app_tests = [dict(r) for r in db.execute("SELECT * FROM app_tests WHERE started>=? AND started<? ORDER BY started", (lo,hi))]
    latencies = [r["elapsed_ms"] for r in req if r["ok"]]
    ok = sum(r["ok"] for r in req)
    completed = [c for c in cycles if c["ended"] is not None]
    expected = max(0, math.ceil((min(time.time(),hi)-lo)/cfg["interval_seconds"]))
    slots = len({int((c["started"]-lo)//cfg["interval_seconds"]) for c in completed})
    summary = {
        "date": day, "timezone": cfg["timezone"], "generated_at": datetime.now(zone).isoformat(),
        "cycles_started": len(cycles), "cycles_completed": len(completed), "expected_slots_so_far": expected,
        "coverage_percent": round(100*slots/expected,2) if expected else None,
        "requests": len(req), "requests_ok": ok, "request_success_percent": round(100*ok/len(req),2) if req else None,
        "latency_success_median_ms": statistics.median(latencies) if latencies else None,
        "latency_success_p95_ms": percentile(latencies,.95), "vehicle_samples": len(obs),
        "gps_recent": sum(o["state"]=="gps_recente" for o in obs),
        "gps_recent_percent": round(100*sum(o["state"]=="gps_recente" for o in obs)/len(obs),2) if obs else None,
        "manual_references": len(refs), "official_watch_pairs": len(pairs),
        "field_errors": errors,
        "application_ui_automatically_tested": False, "app_test_runs": app_tests,
        "limitations": ["A coleta automática mede a fonte Mobilibus, não comprova a interface do aplicativo nem consumo de bateria.", "Lista vazia não significa falha nem cancelamento de viagem.", "SOU e SouWatch podem compartilhar a mesma fonte: concordância não é verdade de campo.", "Horários de GPS sem data usam interpretação local e hipótese de meia-noite, como no app.", "Múltiplas previsões de uma viagem não são amostras estatisticamente independentes."]
    }
    folder = out / "relatorios"; folder.mkdir(exist_ok=True)
    stem = folder / day
    def atomic(path, data):
        temp = path.with_suffix(path.suffix+".tmp"); temp.write_text(data,encoding="utf-8"); temp.replace(path)
    atomic(stem.with_suffix(".json"), json.dumps(summary,ensure_ascii=False,indent=2))
    def export_csv(name, rows, columns):
        path=folder/f"{day}-{name}.csv"; temp=path.with_suffix(".tmp")
        with temp.open("w",newline="",encoding="utf-8-sig") as f:
            writer=csv.DictWriter(f,fieldnames=columns,extrasaction="ignore"); writer.writeheader(); writer.writerows(rows)
        temp.replace(path)
    export_csv("requisicoes",req,["id","cycle","captured","line","endpoint","http_status","elapsed_ms","ok","error","sha256"])
    export_csv("gps",obs,["id","request_id","captured","line","trip_id","direction","vehicle_id","departure","gps_time","gps_age","lat","lng","state","association"])
    export_csv("referencias",refs,["id","captured","source","line","vehicle_id","departure","stop_id","eta_minutes","lat","lng","note"])
    export_csv("erros-campo",errors,["source","prediction_id","arrival_id","error_minutes"])
    esc=lambda v: html.escape(str(v))
    fmt=lambda v: "Sem amostra" if v is None else f"{v:.1f}"
    def table(headers, rows):
        return "<table><tr>"+"".join(f"<th>{esc(h)}</th>" for h in headers)+"</tr>"+"".join("<tr>"+"".join(f"<td>{esc(v)}</td>" for v in row)+"</tr>" for row in rows)+"</table>"
    route_rows=[]
    for line in cfg["routes"]:
        r=[x for x in req if x["line"]==line]; o=[x for x in obs if x["line"]==line]; c=[x for x in checks if x["line"]==line]
        route_rows.append([line,len(r),sum(x["ok"] for x in r),len(o),sum(x["state"]=="gps_recente" for x in o),sum(x["association"]!="associado" for x in o),sum(x["status"]!="ok" for x in c)])
    pair_rows=[]
    for a,b in pairs:
        diff="Sem ETA em ambos"
        if a["eta_minutes"] is not None and b["eta_minutes"] is not None:
            diff=f"{b['eta_minutes']-a['eta_minutes']:+.1f} min"
        pair_rows.append([a["line"],a["vehicle_id"],a["departure"],a["stop_id"],a["eta_minutes"],b["eta_minutes"],round(abs(a["captured"]-b["captured"])),diff])
    field_rows=[]
    for source in ("sou","souwatch"):
        es=[e["error_minutes"] for e in errors if e["source"]==source]
        field_rows.append([source,len(es),fmt(statistics.mean(abs(e) for e in es) if es else None),fmt(statistics.mean(es) if es else None)])
    app_rows = [[datetime.fromtimestamp(t["started"],zone).strftime("%H:%M:%S"), t["status"], t["tests"], t["failures"], t["skipped"], t["detail"]] for t in app_tests]
    page=f'''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>SouWatch — {day}</title>
<style>body{{font:16px system-ui;max-width:1150px;margin:40px auto;padding:0 20px;background:#101820;color:#e5edf4}}h1,h2{{color:#54cfff}}table{{border-collapse:collapse;width:100%;margin:20px 0}}th,td{{text-align:left;padding:10px;border-bottom:1px solid #344455}}a{{color:#54cfff}}.note{{background:#233240;padding:15px;border-radius:8px}}small{{color:#b4c5d3}}</style>
<h1>SouWatch — relatório de {day}</h1><small>Fuso {esc(cfg['timezone'])}. Gerado {esc(summary['generated_at'])}.</small>
<p class="note">Este relatório distingue a saúde da fonte de dados dos testes do aplicativo. Não há automação de interface ou medição de bateria nesta coleta.</p>
<h2>Cobertura real da coleta</h2><p>{len(completed)} ciclos concluídos; {expected} intervalos esperados até agora no dia. Cobertura: {fmt(summary['coverage_percent'])}%. O tempo antes de iniciar, desligamento e suspensão do computador aparecem como falta de cobertura.</p>
<p>Consultas válidas: {ok}/{len(req)} ({fmt(summary['request_success_percent'])}%). Latência mediana: {fmt(summary['latency_success_median_ms'])} ms; p95: {fmt(summary['latency_success_p95_ms'])} ms. GPS recente: {summary['gps_recent']}/{len(obs)} amostras. Estes percentuais descrevem apenas as observações coletadas.</p>
<h2>Linhas acompanhadas</h2>{table(['Linha','Consultas','Válidas','Amostras GPS','Recentes','Sem associação','Checagens não OK'],route_rows)}
<p>Checagens não OK incluem ausência de serviço programado e calendário especial; consulte o banco para distinguir os motivos. Uma resposta vazia bem-sucedida não é falha da API.</p>
<h2>Testes automáticos do projeto Android</h2><p>Execuções periódicas dos testes JVM já existentes no projeto. Falha de infraestrutura é distinguida de teste reprovado. Não são testes contínuos de interface no relógio.</p>
{table(['Hora','Resultado','Testes','Falhas','Ignorados','Detalhe'],app_rows)}
<h2>Referência do app SOU</h2><p>{len(pairs)} pares manuais, com mesma linha, veículo, saída e ponto, e distância temporal de até 120 segundos. Cada observação é usada em no máximo um par. Não houve leitura automática do aplicativo SOU.</p>
{table(['Linha','Ônibus','Saída','Ponto','ETA SOU','ETA SouWatch','Separação (s)','Diferença'],pair_rows)}
<h2>Chegada observada no ponto</h2><p>Erro = chegada prevista menos passagem observada. Positivo: previsão mais tarde. Negativo: previsão mais cedo. N conta previsões associadas, não viagens independentes.</p>
{table(['Fonte','N previsões','Erro absoluto médio (min)','Viés (min)'],field_rows)}
<h2>Limites</h2><ul>{''.join('<li>'+esc(x)+'</li>' for x in summary['limitations'])}</ul>
<h2>Dados para auditoria</h2><p><a href="{day}.json">Resumo JSON</a> · <a href="{day}-requisicoes.csv">Consultas CSV</a> · <a href="{day}-gps.csv">GPS CSV</a> · <a href="{day}-referencias.csv">Referências CSV</a> · <a href="{day}-erros-campo.csv">Erros de campo CSV</a></p><p>Respostas brutas compactadas e SHA-256 no banco dados.sqlite3. Identificadores e horários UTC (Unix) nos CSVs preservam a rastreabilidade.</p></html>'''
    atomic(stem.with_suffix(".html"),page)
    atomic(folder/"ultimo.html",page)
    return summary


def load_config(path):
    cfg=json.loads(path.read_text(encoding="utf-8-sig"))
    ZoneInfo(cfg["timezone"])
    if cfg["interval_seconds"]<30 or cfg["request_gap_seconds"]<.5 or not 1<=cfg["request_timeout_seconds"]<=60:
        raise ValueError("Intervalo mínimo 30s, espaçamento mínimo 0,5s; timeout 1–60s")
    if cfg.get("app_test_interval_seconds",0) and cfg["app_test_interval_seconds"]<1800:
        raise ValueError("Intervalo mínimo dos testes Android: 1800 segundos")
    if not cfg["routes"] or len(cfg["routes"])>10:
        raise ValueError("Configure entre 1 e 10 linhas")
    return cfg


class SingleInstance:
    def __init__(self,path): self.path=path; self.file=None
    def __enter__(self):
        self.file=self.path.open("a+b")
        if self.file.seek(0,2)==0: self.file.write(b"0"); self.file.flush()
        self.file.seek(0)
        try:
            if os.name=="nt":
                import msvcrt
                msvcrt.locking(self.file.fileno(),msvcrt.LK_NBLCK,1)
            else:
                import fcntl
                fcntl.flock(self.file.fileno(),fcntl.LOCK_EX|fcntl.LOCK_NB)
        except OSError:
            self.file.close(); raise RuntimeError("Já existe um monitor nesta pasta de saída")
        return self
    def __exit__(self,*args): self.file.close()


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config",type=Path,default=HERE/"config.json")
    parser.add_argument("--output",type=Path,default=HERE/"saida")
    sub=parser.add_subparsers(dest="command",required=True)
    sub.add_parser("start"); sub.add_parser("stop")
    run=sub.add_parser("run"); run.add_argument("--once",action="store_true"); run.add_argument("--hours",type=float)
    rep=sub.add_parser("report"); rep.add_argument("--date")
    ref=sub.add_parser("reference")
    ref.add_argument("--source",choices=["sou","souwatch","campo"],required=True)
    for flag in ("line","vehicle","departure","stop"): ref.add_argument("--"+flag,required=True)
    ref.add_argument("--eta",type=float); ref.add_argument("--at",help="ISO 8601 com fuso; omitir usa agora")
    ref.add_argument("--lat",type=float); ref.add_argument("--lng",type=float); ref.add_argument("--note",default="")
    capture=sub.add_parser("capture-sou"); capture.add_argument("--serial",required=True)
    raw=sub.add_parser("extract"); raw.add_argument("--request",type=int,required=True)
    args=parser.parse_args(); cfg=load_config(args.config); zone=ZoneInfo(cfg["timezone"])
    args.output.mkdir(parents=True,exist_ok=True)
    if args.command=="capture-sou":
        adb=Path(os.environ.get("LOCALAPPDATA", ""))/"Android/Sdk/platform-tools/adb.exe"
        if not adb.exists(): parser.error("ADB não encontrado no SDK local")
        def adb_text(*parts):
            return subprocess.run([str(adb),"-s",args.serial,*parts],check=True,capture_output=True,text=True,encoding="utf-8",errors="replace",timeout=30).stdout
        def sou_foreground():
            state=adb_text("shell","dumpsys","activity","activities")
            active="\n".join(line for line in state.splitlines() if "topResumedActivity" in line or "mResumedActivity" in line)
            if not active:
                windows=adb_text("shell","dumpsys","window")
                active="\n".join(line for line in windows.splitlines() if "mCurrentFocus=" in line or "mFocusedApp=" in line)
            if "br.com.sou.app/" not in active:
                raise RuntimeError("Abra o aplicativo SOU no celular antes de capturar; nenhuma evidência será registrada")
            return active
        foreground=sou_foreground()
        captured=datetime.now(timezone.utc)
        folder=args.output/"referencias-sou"/captured.strftime("%Y%m%dT%H%M%SZ")
        folder.mkdir(parents=True,exist_ok=False)
        picture=folder/"tela.png"
        with picture.open("wb") as output:
            subprocess.run([str(adb),"-s",args.serial,"exec-out","screencap","-p"],stdout=output,check=True,timeout=30)
        try: sou_foreground()
        except Exception:
            picture.unlink(missing_ok=True)
            raise RuntimeError("O app em primeiro plano mudou durante a captura; imagem descartada")
        try:
            adb_text("shell","uiautomator","dump","/sdcard/sou-monitor-ui.xml")
            (folder/"interface.xml").write_text(adb_text("shell","cat","/sdcard/sou-monitor-ui.xml"),encoding="utf-8")
        except Exception as exc:
            (folder/"interface-indisponivel.txt").write_text(str(exc),encoding="utf-8")
        metadata={"fonte":"Aplicativo SOU no celular", "capturado_em":captured.isoformat(),"serial":args.serial,"atividade":foreground,"sha256":hashlib.sha256(picture.read_bytes()).hexdigest(),"nota":"ETA e identificação precisam ser conferidas antes do registro manual"}
        (folder/"evidencia.json").write_text(json.dumps(metadata,ensure_ascii=False,indent=2),encoding="utf-8")
        print(folder)
        return
    if args.command=="stop":
        (args.output/"PARAR").write_text("Encerramento solicitado",encoding="utf-8")
        print("Encerramento solicitado; aguardando a operação atual terminar.")
        return
    if args.command=="start":
        args.output=args.output.resolve()
        with (args.output/"processo-stdout.log").open("a",encoding="utf-8") as stdout, (args.output/"processo-stderr.log").open("a",encoding="utf-8") as stderr:
            process=subprocess.Popen([sys.executable,"-X","utf8",str(Path(__file__).resolve()),"--config",str(args.config.resolve()),"--output",str(args.output),"run"],
                cwd=HERE, stdout=stdout, stderr=stderr, close_fds=True,
                creationflags=getattr(subprocess,"CREATE_NO_WINDOW",0)|getattr(subprocess,"CREATE_NEW_PROCESS_GROUP",0),start_new_session=(os.name!="nt"))
        time.sleep(1)
        if process.poll() is not None:
            raise RuntimeError("Monitor não iniciou; consulte processo-stderr.log (pode já estar em execução)")
        print(f"Monitor iniciado: processo {process.pid}; relatórios em {args.output/'relatorios'}")
        return
    if args.command=="extract":
        db=connect_db(args.output/"dados.sqlite3"); row=db.execute("SELECT payload_zlib,sha256 FROM requests WHERE id=?",(args.request,)).fetchone()
        if row is None: parser.error("Requisição inexistente")
        body=zlib.decompress(row[0]); assert hashlib.sha256(body).hexdigest()==row[1]
        path=args.output/f"resposta-{args.request}.json"; path.write_bytes(body); print(path); return
    if args.command in ("report","reference"):
        db=connect_db(args.output/"dados.sqlite3")
        if args.command=="reference":
            if args.line not in cfg["routes"]: parser.error("Linha não configurada")
            if minute(args.departure) is None: parser.error("Saída inválida; use HH:MM")
            if args.eta is not None and (not math.isfinite(args.eta) or args.eta<0): parser.error("ETA deve ser não negativa e finita")
            if args.source=="campo" and args.eta is not None: parser.error("Campo registra a passagem real, sem ETA")
            stamp=datetime.fromisoformat(args.at) if args.at else datetime.now(zone)
            if stamp.tzinfo is None: parser.error("Inclua o fuso no horário, por exemplo 2026-10-23T16:30:00-03:00")
            if stamp.timestamp()>time.time()+60: parser.error("A observação não pode estar no futuro")
            if (args.lat is not None or args.lng is not None) and not valid_coord(args.lat,args.lng): parser.error("Coordenadas inválidas")
            db.execute("INSERT INTO references_manual(captured,source,line,vehicle_id,departure,stop_id,eta_minutes,lat,lng,note) VALUES(?,?,?,?,?,?,?,?,?,?)",(stamp.timestamp(),args.source,args.line,args.vehicle,f"{minute(args.departure)//60:02d}:{minute(args.departure)%60:02d}",args.stop,args.eta,args.lat,args.lng,args.note)); db.commit()
            day=str(stamp.astimezone(zone).date())
        else: day=args.date or str(datetime.now(zone).date())
        if args.command=="reference": report(db,cfg,args.output,str(datetime.fromisoformat(day).date()-timedelta(days=1)))
        report(db,cfg,args.output,day); print(args.output/"relatorios"/f"{day}.html"); return
    if args.hours is not None and args.hours<=0: parser.error("Duração deve ser positiva")
    logging.basicConfig(level=logging.INFO,format="%(asctime)s %(levelname)s %(message)s",handlers=[logging.FileHandler(args.output/"monitor.log",encoding="utf-8"),logging.StreamHandler()])
    stop=args.output/"PARAR"; pid=args.output/"monitor.pid"
    with SingleInstance(args.output/"monitor.lock"):
        stop.unlink(missing_ok=True); pid.write_text(str(os.getpid()),encoding="ascii")
        monitor=Monitor(cfg,args.output); until=time.monotonic()+args.hours*3600 if args.hours else float("inf")
        last_day=str(datetime.now(zone).date())
        try:
            while time.monotonic()<until and not stop.exists():
                cycle_start=time.monotonic()
                try: monitor.cycle()
                except Exception: logging.exception("Ciclo interrompido por erro; retomará no próximo intervalo")
                if not stop.exists():
                    try: test_app_if_due(monitor.db,cfg,args.output)
                    except Exception: logging.exception("Não foi possível registrar testes do app; coleta da API continuará")
                today=str(datetime.now(zone).date())
                for day in sorted({last_day,today}): report(monitor.db,cfg,args.output,day)
                last_day=today
                if args.once: break
                while time.monotonic()<min(cycle_start+cfg["interval_seconds"],until) and not stop.exists(): time.sleep(1)
        except KeyboardInterrupt: logging.info("Encerramento solicitado")
        finally:
            report(monitor.db,cfg,args.output,last_day); monitor.db.close(); pid.unlink(missing_ok=True)
    return 0

if __name__=="__main__":
    sys.exit(main())
