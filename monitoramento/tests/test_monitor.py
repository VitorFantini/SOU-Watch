import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from datetime import datetime
from unittest.mock import patch
from zoneinfo import ZoneInfo
import zlib

spec=importlib.util.spec_from_file_location('monitor',Path(__file__).parents[1]/'monitor.py')
m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
TEMP_ROOT=(Path(__file__).parents[1]/'.test-tmp').resolve()
assert TEMP_ROOT.is_relative_to(Path(__file__).parents[1].resolve())
TEMP_ROOT.mkdir(exist_ok=True)
ZONE=ZoneInfo('America/Sao_Paulo')
NOW=datetime(2026,10,23,16,30,tzinfo=ZONE).timestamp()

def timetable():
    return {'timetable': {'exceptions': [], 'trips':[{'tripId':101,'seq':1,'directionId':0},{'tripId':102,'seq':2,'directionId':1}], 'directions':[{'directionId':0,'services':[{'days':[False,True,True,True,True,True,False], 'start':'2026-01-01','end':'2026-12-31','departures':[{'seq':1,'dep':'16:15'}]}]}]}}

def reference(id,source,stamp=NOW,eta=5,stop='99',vehicle='2035'):
    return {'id':id,'source':source,'captured':stamp,'eta_minutes':eta,'line':'01','vehicle_id':vehicle,'departure':'16:15','stop_id':stop,'lat':None,'lng':None,'note':''}

class MonitorTests(unittest.TestCase):
    def test_service_day_direction_and_seq(self):
        self.assertEqual({'101':(0,{975})},m.active_trips(timetable(),NOW,ZONE))
    def test_sunday_without_service(self):
        sunday=datetime(2026,10,25,16,30,tzinfo=ZONE).timestamp()
        self.assertEqual({},m.active_trips(timetable(),sunday,ZONE))
    def test_special_calendar_is_not_assumed_normal(self):
        t=timetable(); t['timetable']['exceptions']=[{'date':'2026-10-23'}]
        with self.assertRaises(ValueError): m.active_trips(t,NOW,ZONE)
    def test_malformed_schema(self):
        with self.assertRaises(ValueError): m.active_trips({},NOW,ZONE)
    def test_gps_midnight(self):
        now=datetime(2026,10,24,0,0,20,tzinfo=ZONE).timestamp()
        self.assertEqual(30,m.gps_age('23:59:50',now,ZONE))
        self.assertIsNone(m.gps_age(None,now,ZONE))
    def test_invalid_coordinates(self):
        for lat,lng in [(float('nan'),0),(91,0),(0,181),(True,0),('22',0)]: self.assertFalse(m.valid_coord(lat,lng))
    def test_reference_pairs_are_one_to_one(self):
        rows=[reference(1,'sou'),reference(2,'souwatch',NOW+5),reference(3,'souwatch',NOW+10)]
        self.assertEqual(1,len(m.pair_references(rows)))
    def test_wrong_stop_vehicle_or_time_not_paired(self):
        for other in [reference(2,'souwatch',stop='88'),reference(2,'souwatch',vehicle='9999'),reference(2,'souwatch',NOW+121)]:
            self.assertEqual([],m.pair_references([reference(1,'sou'),other]))
    def test_field_error_uses_observed_arrival(self):
        rows=[reference(1,'sou',eta=5),reference(2,'souwatch',eta=7),reference(3,'campo',NOW+360,eta=None)]
        result=m.field_errors(rows)
        self.assertEqual([-1,1],[r['error_minutes'] for r in result])
    def test_no_field_data_no_accuracy_claim(self):
        self.assertEqual([],m.field_errors([reference(1,'sou')]))
    def test_empty_report_has_no_fake_success(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as tmp:
            out=Path(tmp); db=m.connect_db(out/'test.db')
            cfg={'timezone':'America/Sao_Paulo','interval_seconds':60,'routes':{'01':'942455'}}
            report=m.report(db,cfg,out,'2026-10-23')
            self.assertEqual(0,report['cycles_completed']); self.assertIsNone(report['request_success_percent']); self.assertFalse(report['application_ui_automatically_tested'])
            self.assertTrue((out/'relatorios/2026-10-23.html').exists()); db.close()
    def test_http_raw_auditable_and_empty_vehicles_are_success(self):
        class Response:
            status=200
            def __enter__(self): return self
            def __exit__(self,*args): pass
            def read(self,limit): return b'[]'
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as tmp:
            cfg={'timezone':'America/Sao_Paulo','request_gap_seconds':0,'request_timeout_seconds':1}
            monitor=m.Monitor(cfg,Path(tmp))
            with patch.object(m.urllib.request,'urlopen',return_value=Response()):
                payload,id=monitor.request(1,'01','vehicles',{'trip_id':'101'})
            self.assertEqual([],payload)
            row=monitor.db.execute('SELECT * FROM requests WHERE id=?',(id,)).fetchone()
            self.assertEqual(1,row['ok']); self.assertEqual(b'[]',zlib.decompress(row['payload_zlib'])); self.assertEqual(m.hashlib.sha256(b'[]').hexdigest(),row['sha256'])
            monitor.db.close()
    def test_invalid_json_is_recorded_as_failure(self):
        class Response:
            status=200
            def __enter__(self): return self
            def __exit__(self,*args): pass
            def read(self,limit): return b'<html>error</html>'
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as tmp:
            monitor=m.Monitor({'timezone':'America/Sao_Paulo','request_gap_seconds':0,'request_timeout_seconds':1},Path(tmp))
            with patch.object(m.urllib.request,'urlopen',return_value=Response()): payload,id=monitor.request(1,'01','vehicles',{})
            self.assertIsNone(payload); self.assertEqual(0,monitor.db.execute('SELECT ok FROM requests WHERE id=?',(id,)).fetchone()[0]); monitor.db.close()

    def test_app_build_failure_is_infrastructure_not_fake_test_failure(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as tmp:
            out=Path(tmp); (out/'monitor').mkdir()
            db=m.connect_db(out/'test.db')
            with patch.object(m,'HERE',out/'monitor'), patch.object(m.subprocess,'run',return_value=type('Result',(),{'returncode':1})()):
                m.test_app_if_due(db,{'app_test_interval_seconds':1800},out)
            row=db.execute('SELECT status,tests,failures FROM app_tests').fetchone()
            self.assertEqual(('infrastructure_error',0,0),tuple(row)); db.close()
    def test_app_results_are_counted_only_when_fresh(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as tmp:
            out=Path(tmp); (out/'monitor').mkdir()
            reports=out/'app/build/test-results/testDebugUnitTest'; reports.mkdir(parents=True)
            (reports/'TEST-example.xml').write_text('<testsuite tests="17" failures="0" errors="0" skipped="0"/>',encoding='utf-8')
            db=m.connect_db(out/'test.db')
            with patch.object(m,'HERE',out/'monitor'), patch.object(m.subprocess,'run',return_value=type('Result',(),{'returncode':0})()):
                m.test_app_if_due(db,{'app_test_interval_seconds':1800},out)
                m.test_app_if_due(db,{'app_test_interval_seconds':1800},out)
            row=db.execute('SELECT status,tests,failures FROM app_tests').fetchone()
            self.assertEqual(('passed',17,0),tuple(row))
            self.assertEqual(1,db.execute('SELECT count(*) FROM app_tests').fetchone()[0]); db.close()

    def test_report_pairs_across_midnight(self):
        with tempfile.TemporaryDirectory(dir=TEMP_ROOT) as tmp:
            out=Path(tmp); db=m.connect_db(out/'test.db')
            midnight=datetime(2026,10,24,0,0,tzinfo=ZONE).timestamp()
            for r in [reference(1,'sou',midnight-30),reference(2,'souwatch',midnight+30)]:
                db.execute('INSERT INTO references_manual(id,captured,source,line,vehicle_id,departure,stop_id,eta_minutes) VALUES(?,?,?,?,?,?,?,?)',(r['id'],r['captured'],r['source'],r['line'],r['vehicle_id'],r['departure'],r['stop_id'],r['eta_minutes']))
            db.commit()
            report=m.report(db,{'timezone':'America/Sao_Paulo','interval_seconds':60,'routes':{'01':'942455'}},out,'2026-10-23')
            self.assertEqual(1,report['official_watch_pairs']); db.close()

if __name__=='__main__': unittest.main()
