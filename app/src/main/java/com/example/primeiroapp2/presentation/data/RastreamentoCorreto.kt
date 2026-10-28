package com.example.primeiroapp2.presentation.data

import com.example.primeiroapp2.presentation.BusSchedule
import java.time.*
import kotlin.math.*

object RastreamentoCorreto {
    // Read the watch timezone on each access, including after a system timezone change.
    val fuso: ZoneId get() = ZoneId.systemDefault()
    fun minutos(horario: String?): Int? {
        val m = Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?(?:\\s|$)").find(horario.orEmpty()) ?: return null
        val h = m.groupValues[1].toInt(); val min = m.groupValues[2].toInt()
        return if (h in 0..47 && min in 0..59) h * 60 + min else null
    }
    private fun partidasDoDia(tabela: TimetableDetails, sentido: Int, hoje: LocalDate): List<DepartureData> {
        require(tabela.exceptions.orEmpty().none { runCatching { LocalDate.parse(it.asJsonObject["date"].asString) }.getOrNull().let { date -> date == null || date == hoje } }) { "Calendário especial hoje: saída não confirmada" }
        return tabela.directions.orEmpty().filter { it.directionId == sentido }.flatMap { it.services }
            .filter { it.days.size == 7 && it.days[hoje.dayOfWeek.value % 7] && (it.start == null || it.start <= hoje.toString()) && (it.end == null || it.end >= hoje.toString()) }
            .flatMap { it.departures }
    }
    fun horariosDoDia(tabela: TimetableDetails, sentido: Int, hoje: LocalDate): List<BusSchedule> =
        partidasDoDia(tabela, sentido, hoje).flatMap { dep ->
            tabela.trips.orEmpty().filter { it.directionId == sentido && it.seq == dep.seq }.map { trip ->
                BusSchedule("${trip.tripId}:${dep.dep}", "${dep.dep} ( ${trip.tripDesc} )", trip.tripId)
            }
        }.distinctBy { it.tripId }.sortedBy { minutos(it.horaSaida) ?: Int.MAX_VALUE }
    fun encontrarViagem(tabela: TimetableDetails, sentido: Int, horario: String, hoje: LocalDate, tripIdEsperado: Long? = null): TripData {
        val alvo = requireNotNull(minutos(horario)) { "Horário inválido" }
        val seqs = partidasDoDia(tabela, sentido, hoje).filter { minutos(it.dep) == alvo }.map { it.seq }.toSet()
        val trips = tabela.trips.orEmpty().filter { it.directionId == sentido && it.seq in seqs && (tripIdEsperado == null || it.tripId == tripIdEsperado) }.distinctBy { it.tripId }
        require(trips.size == 1) { if (trips.isEmpty()) "Saída não confirmada para hoje" else "Mais de um trajeto para esta saída" }
        return trips.single()
    }
    fun selecionarVeiculo(veiculos: List<BusVehicle>, horario: String, fixado: String?): BusVehicle? {
        val alvo = requireNotNull(minutos(horario)) { "Horário inválido" }
        val candidatos = veiculos.filter { !it.vehicleId.isNullOrBlank() && minutos(it.startTime) == alvo && (fixado == null || it.vehicleId == fixado) }
        require(candidatos.size <= 1) { "Mais de um ônibus para esta saída; busca inconclusiva" }
        return candidatos.singleOrNull()
    }
    fun idadeGps(horario: String?, agora: Instant): Long? = runCatching {
        var instante = agora.atZone(fuso).toLocalDate().atTime(LocalTime.parse(horario)).atZone(fuso).toInstant()
        if (instante > agora.plusSeconds(60)) instante = instante.minusSeconds(86400)
        max(0L, Duration.between(instante, agora).seconds)
    }.getOrNull()
    fun gpsValido(bus: BusVehicle, agora: Instant) = coordenadasValidas(bus.lat, bus.lng) && idadeGps(bus.positionTime, agora)?.let { it <= 120 } == true
    fun coordenadasValidas(lat: Double, lng: Double) = lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0
    fun distancia(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val a = Math.toRadians(lat1); val b = Math.toRadians(lat2)
        val x = sin((b-a)/2).pow(2) + cos(a)*cos(b)*sin(Math.toRadians(lng2-lng1)/2).pow(2)
        return 12742000 * asin(sqrt(x.coerceIn(0.0, 1.0)))
    }
    fun pontosDoTrajeto(detalhes: TripDetails) = detalhes.stops.mapIndexed { i, p -> BusStop(i+1, p.name, p.lat, p.lng, p.stopId) }
    fun atendePonto(detalhes: TripDetails, ponto: BusStop) = detalhes.stops.any {
        if (ponto.stopId != null) it.stopId == ponto.stopId else distancia(it.lat,it.lng,ponto.lat,ponto.lng) <= 100
    }
    data class Coordenada(val lat: Double, val lng: Double)
    data class Projecao(val afastamento: Double, val progresso: Double, val ambigua: Boolean)
    data class Posicao(val progresso: Double, val restante: Double)
    fun decodificar(encoded: String): List<Coordenada> {
        var index=0; var lat=0; var lng=0
        fun delta(): Int {
            var value=0; var shift=0; var b: Int
            do {
                require(index < encoded.length && shift <= 30)
                b=encoded[index++].code-63; require(b in 0..63)
                value=value or ((b and 31) shl shift); shift+=5
            } while (b>=32)
            return if (value and 1 != 0) (value shr 1).inv() else value shr 1
        }
        return buildList { while(index<encoded.length) { lat+=delta(); lng+=delta(); add(Coordenada(lat/1e5,lng/1e5)) } }
    }
    fun projetar(ponto: Coordenada, caminho: List<Coordenada>): Projecao {
        require(caminho.size>=2)
        val escala=cos(Math.toRadians(ponto.lat)); var total=0.0
        val candidatos=caminho.zipWithNext().map { (a,b) ->
            val x=(a.lng-ponto.lng)*escala; val y=a.lat-ponto.lat
            val dx=(b.lng-a.lng)*escala; val dy=b.lat-a.lat; val div=dx*dx+dy*dy
            val t=if(div==0.0) 0.0 else (-(x*dx+y*dy)/div).coerceIn(0.0,1.0)
            val metros=distancia(a.lat,a.lng,b.lat,b.lng)
            val result=distancia(ponto.lat,ponto.lng,a.lat+(b.lat-a.lat)*t,a.lng+(b.lng-a.lng)*t) to (total+t*metros)
            total+=metros; result
        }
        val melhor=candidatos.minBy { it.first }
        val proximos=candidatos.filter { it.first<=melhor.first+25 && it.first<=150 }
        val ambigua=proximos.isNotEmpty() && proximos.maxOf { it.second }-proximos.minOf { it.second }>300
        return Projecao(melhor.first,melhor.second,ambigua)
    }
    fun posicao(bus: BusVehicle, ponto: BusStop, detalhes: TripDetails): Posicao? = runCatching {
        require(coordenadasValidas(bus.lat,bus.lng))
        val caminho=decodificar(requireNotNull(detalhes.shape))
        val b=projetar(Coordenada(bus.lat,bus.lng),caminho); val p=projetar(Coordenada(ponto.lat,ponto.lng),caminho)
        require(!b.ambigua && !p.ambigua && b.afastamento<=150 && p.afastamento<=150)
        Posicao(b.progresso,p.progresso-b.progresso)
    }.getOrNull()
}
enum class SituacaoPonto { ANTES, APROXIMANDO, APOS, PASSOU }
class ObservadorPassagem {
    private var anterior: RastreamentoCorreto.Posicao?=null
    private var instante: Instant?=null
    private var gps: String?=null
    private var passou=false
    private var aproximando=false
    fun observar(posicao: RastreamentoCorreto.Posicao, horarioGps: String, agora: Instant): SituacaoPonto {
        if(horarioGps!=gps) {
            val antes=anterior; val segundos=instante?.let { Duration.between(it,agora).seconds } ?: -1
            val delta=posicao.progresso-(antes?.progresso ?: posicao.progresso)
            val valido=antes!=null && segundos in 1..180 && abs(delta)<=25*segundos+100
            passou=passou || (valido && antes!!.restante>=-80 && posicao.restante< -80 && delta>=30)
            aproximando=valido && delta>=30 && posicao.restante>=-80
            anterior=posicao; instante=agora; gps=horarioGps
        }
        return if(posicao.restante< -80) { if(passou) SituacaoPonto.PASSOU else SituacaoPonto.APOS } else if(aproximando) SituacaoPonto.APROXIMANDO else SituacaoPonto.ANTES
    }
}
object VerificacaoHorarios {
    fun filtrar(horarios: List<BusSchedule>, agora: Instant): List<BusSchedule> {
        val local=agora.atZone(RastreamentoCorreto.fuso); val minuto=local.hour*60+local.minute
        val lista=horarios.mapNotNull { h -> RastreamentoCorreto.minutos(h.horaSaida)?.let { h to it } }.sortedBy { it.second }
        return (lista.filter { it.second<minuto }.takeLast(2)+lista.filter { it.second>=minuto }).map { it.first }
    }
    suspend fun consultar(horarios: List<BusSchedule>, agora: Instant): Map<String, ChecagemHorario> {
        val result=mutableMapOf<String,ChecagemHorario>()
        for((trip,partidas) in horarios.groupBy { it.apiTripId }) {
            try {
                val vehicles=BusRepository.apiMobilibus.buscarVeiculoPorTripId(tripId=requireNotNull(trip).toString())
                partidas.forEach { h ->
                    result[h.tripId]=try {
                        val v=RastreamentoCorreto.selecionarVeiculo(vehicles,h.horaSaida,null)
                        when { v==null -> ChecagemHorario(EstadoHorario.SEM_VEICULO,mensagem="Sem ônibus informado")
                            !RastreamentoCorreto.gpsValido(v,agora) -> ChecagemHorario(EstadoHorario.GPS_ANTIGO,mensagem="GPS antigo ou inválido")
                            else -> ChecagemHorario(EstadoHorario.LOCALIZADO,v,"Ônibus ${v.vehicleId}") }
                    } catch(e: IllegalArgumentException) { ChecagemHorario(EstadoHorario.INCONCLUSIVO,mensagem=e.message.orEmpty()) }
                }
            } catch(e: kotlinx.coroutines.CancellationException) { throw e }
            catch(e: Exception) { partidas.forEach { result[it.tripId]=ChecagemHorario(EstadoHorario.INCONCLUSIVO,mensagem="Consulta indisponível") } }
        }
        return result
    }
}
