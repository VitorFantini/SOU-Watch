package com.example.primeiroapp2.presentation.data
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class EstimativasPontosTest {
    private val agora=Instant.now()
    private fun bus()=BusVehicle(lat=0.0,lng=0.005,vehicleId="2072",startTime="13:45",
        positionTime=agora.atZone(RastreamentoCorreto.fuso).toLocalTime().withNano(0).toString())
    private val pontos=listOf(ApiStop(1,"Mesmo nome",0.0,0.001),ApiStop(2,"Mesmo nome",0.0,0.004),ApiStop(3,"Adiante",0.0,0.009))
    private fun detalhes()=TripDetails(pontos,"???o}@")
    @Test fun soPontosAdianteRecebemTempoSemConfundirNomes() {
        assertEquals(mapOf(3 to 2),EstimativasPontos(detalhes()).calcular(bus(),agora))
    }
    @Test fun gpsAntigoAusenteOuInvalidoNaoProduzMinutos() {
        val estimativas=EstimativasPontos(detalhes())
        assertTrue(estimativas.calcular(bus(),agora.plusSeconds(122)).isEmpty())
        assertTrue(estimativas.calcular(null,agora).isEmpty())
        assertTrue(estimativas.calcular(bus().copy(lat=Double.NaN),agora).isEmpty())
    }
    @Test fun trajetoAusenteAmbiguoOuOnibusForaDaRotaNaoProduzemMinutos() {
        assertTrue(EstimativasPontos(detalhes().copy(shape=null)).calcular(bus(),agora).isEmpty())
        assertTrue(EstimativasPontos(detalhes().copy(shape="???o}@?n}@")).calcular(bus(),agora).isEmpty())
        assertTrue(EstimativasPontos(detalhes()).calcular(bus().copy(lat=0.01),agora).isEmpty())
    }
    @Test fun paradaForaDaRotaNaoRecebeEstimativaEmLinhaReta() {
        val detalhes=detalhes().copy(stops=pontos+ApiStop(4,"Fora da rota",0.01,0.009))
        assertEquals(mapOf(3 to 2),EstimativasPontos(detalhes).calcular(bus(),agora))
    }
}
