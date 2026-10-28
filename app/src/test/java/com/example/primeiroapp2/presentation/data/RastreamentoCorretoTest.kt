package com.example.primeiroapp2.presentation.data

import com.example.primeiroapp2.presentation.BusSchedule
import com.example.primeiroapp2.presentation.tile.*
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.util.TimeZone
import java.time.*

class RastreamentoCorretoTest {
    private lateinit var fusoOriginal: TimeZone
    @Before fun configurarFusoDosCenarios() {
        fusoOriginal = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    }
    @After fun restaurarFuso() { TimeZone.setDefault(fusoOriginal) }
    @Test fun acompanhaFusoConfiguradoNoRelogio() {
        assertEquals(ZoneId.of("America/Sao_Paulo"), RastreamentoCorreto.fuso)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Manaus"))
        assertEquals(ZoneId.of("America/Manaus"), RastreamentoCorreto.fuso)
        val horarios = listOf("14:00", "15:00", "15:30", "16:00", "17:00").map { BusSchedule(it,it) }
        assertEquals(listOf("14:00", "15:00", "15:30", "16:00", "17:00"), VerificacaoHorarios.filtrar(horarios, agora).map { it.horaSaida })
        assertEquals(30L, RastreamentoCorreto.idadeGps("15:29:30", agora))
    }

    private val hoje=LocalDate.of(2026,10,23)
    private val agora=Instant.parse("2026-10-23T19:30:00Z")
    private fun tabela()=TimetableDetails(
        listOf(TripData(101,"Centro",0,1),TripData(102,"Bairro",1,2)),
        listOf(DirectionData(0,listOf(ServiceData(List(7){true},"2026-01-01","2026-12-31",listOf(DepartureData("16:15",1),DepartureData("17:00",1))))),DirectionData(1,listOf(ServiceData(List(7){true},null,null,listOf(DepartureData("16:15",2)))))),emptyList())
    private fun bus(id: String="2035",saida: String="16:15",gps: String="16:29:30")=BusVehicle(lat=-22.0,lng=-47.9,vehicleId=id,startTime=saida,positionTime=gps)
    @Test fun horariosRespeitamDiaSentidoEIdentidade() {
        val h=RastreamentoCorreto.horariosDoDia(tabela(),0,hoje)
        assertEquals(listOf(101L,101L),h.map { it.apiTripId })
        assertEquals(2,h.map { it.tripId }.distinct().size)
        assertTrue(RastreamentoCorreto.horariosDoDia(tabela(),0,hoje.plusYears(1)).isEmpty())
    }
    @Test fun calendarioEspecialNaoInventaPartida() {
        val t=tabela().copy(exceptions=listOf(JsonParser.parseString("{\"date\":\"2026-10-23\"}")))
        assertThrows(IllegalArgumentException::class.java) { RastreamentoCorreto.horariosDoDia(t,0,hoje) }
    }
    @Test fun calendarioForaDoDiaNaoBloqueia() {
        val t=tabela().copy(exceptions=listOf(JsonParser.parseString("{\"date\":\"2026-10-22\"}")))
        assertEquals(2,RastreamentoCorreto.horariosDoDia(t,0,hoje).size)
    }
    @Test fun viagemCorrespondeAoSentidoEHorario() {
        assertEquals(102L,RastreamentoCorreto.encontrarViagem(tabela(),1,"16:15 (Bairro)",hoje,102).tripId)
        assertThrows(IllegalArgumentException::class.java) { RastreamentoCorreto.encontrarViagem(tabela(),0,"16:15",hoje,102) }
    }
    @Test fun preservaSomenteDuasSaidasAnteriores() {
        val horarios=listOf("14:00","15:00","16:00","16:15","16:30","17:00").map { BusSchedule(it,it) }
        assertEquals(listOf("16:00","16:15","16:30","17:00"),VerificacaoHorarios.filtrar(horarios,agora).map { it.horaSaida })
    }
    @Test fun naoTrocaVeiculoFixado() {
        assertNull(RastreamentoCorreto.selecionarVeiculo(listOf(bus("9999")),"16:15","2035"))
        assertNull(RastreamentoCorreto.selecionarVeiculo(listOf(bus(saida="17:00")),"16:15",null))
        assertEquals("2035",RastreamentoCorreto.selecionarVeiculo(listOf(bus()),"16:15","2035")?.vehicleId)
    }
    @Test fun duplicidadeEhInconclusiva() {
        assertThrows(IllegalArgumentException::class.java) { RastreamentoCorreto.selecionarVeiculo(listOf(bus(),bus("9999")),"16:15",null) }
    }
    @Test fun gpsExpiraEm120Segundos() {
        assertTrue(RastreamentoCorreto.gpsValido(bus(gps="16:28:00"),agora))
        assertFalse(RastreamentoCorreto.gpsValido(bus(gps="16:27:59"),agora))
        assertFalse(RastreamentoCorreto.gpsValido(bus().copy(lat=Double.NaN),agora))
        assertNull(RastreamentoCorreto.idadeGps("inválido",agora))
    }
    @Test fun gpsDaVesperaNaViradaDoDia() {
        assertEquals(30L,RastreamentoCorreto.idadeGps("23:59:50",Instant.parse("2026-10-24T03:00:20Z")))
    }
    @Test fun decodificaTrajetoERejeitaTruncamento() {
        val p=RastreamentoCorreto.decodificar("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3,p.size); assertEquals(38.5,p[0].lat,0.00001); assertEquals(-126.453,p[2].lng,0.00001)
        assertThrows(IllegalArgumentException::class.java) { RastreamentoCorreto.decodificar("_") }
    }
    @Test fun projecaoUsaDistanciaAoLongoDaRota() {
        val p=RastreamentoCorreto.projetar(RastreamentoCorreto.Coordenada(0.0,0.005),listOf(RastreamentoCorreto.Coordenada(0.0,0.0),RastreamentoCorreto.Coordenada(0.0,0.01)))
        assertEquals(556.0,p.progresso,2.0); assertEquals(0.0,p.afastamento,1.0); assertFalse(p.ambigua)
    }
    @Test fun trajetoSobrepostoEhAmbiguo() {
        val a=RastreamentoCorreto.Coordenada(0.0,0.0); val b=RastreamentoCorreto.Coordenada(0.0,0.01)
        assertTrue(RastreamentoCorreto.projetar(RastreamentoCorreto.Coordenada(0.0,0.002),listOf(a,b,a)).ambigua)
    }
    @Test fun primeiraAmostraDepoisNaoConfirmaPassagem() {
        val obs=ObservadorPassagem()
        assertEquals(SituacaoPonto.APOS,obs.observar(RastreamentoCorreto.Posicao(1000.0,-100.0),"16:29:30",agora))
    }
    @Test fun confirmaPassagemComAmostrasDistintasPlausíveis() {
        val obs=ObservadorPassagem()
        assertEquals(SituacaoPonto.ANTES,obs.observar(RastreamentoCorreto.Posicao(800.0,100.0),"16:29:30",agora))
        assertEquals(SituacaoPonto.PASSOU,obs.observar(RastreamentoCorreto.Posicao(1000.0,-100.0),"16:29:50",agora.plusSeconds(20)))
    }
    @Test fun saltoGpsNaoConfirmaPassagem() {
        val obs=ObservadorPassagem()
        obs.observar(RastreamentoCorreto.Posicao(100.0,100.0),"16:29:30",agora)
        assertEquals(SituacaoPonto.APOS,obs.observar(RastreamentoCorreto.Posicao(9000.0,-8800.0),"16:29:31",agora.plusSeconds(1)))
    }
    @Test fun tileExpiradoPreservaTrajetoERemovePrevisao() {
        val mapa=TileMapa("abc",emptyList(),RastreamentoCorreto.Coordenada(-22.0,-47.9))
        val r=TileResultado(TileEstado.CHEGADA,agora.epochSecond,"ok",60.0,300.0,101,"2035","16:15",agora.epochSecond-121,mapa=mapa).atual(agora)
        assertEquals(TileEstado.ANTIGO,r.estado); assertNull(r.segundos); assertNull(r.mapa?.onibus); assertEquals("abc",r.mapa?.shape)
    }
}
