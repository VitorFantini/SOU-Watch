package com.example.primeiroapp2.presentation.maps

import com.example.primeiroapp2.presentation.data.RastreamentoCorreto.Coordenada
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random

class EstresseMapasTest {
    @Test fun cincoMilPosicoesDoTileMantemOnibusEPontoNoMapa() {
        val random=Random(151)
        repeat(5000) { i ->
            val ponto=Coordenada(-22.05+random.nextDouble()*.1,-47.95+random.nextDouble()*.1)
            val bus=if(i%10==0) ponto else Coordenada(ponto.lat+random.nextDouble(-.04,.04),ponto.lng+random.nextDouble(-.04,.04))
            val viewport=ViewportMapa.enquadrar(listOf(ponto,bus))
            for(pos in listOf(ponto,bus)) {
                val (x,y)=viewport.pixel(pos)
                assertTrue("x fora da margem na amostra $i",x in 31.9f..304.1f)
                assertTrue("y fora da margem na amostra $i",y in 31.9f..96.1f)
            }
            assertTrue(viewport.zoom in 1..17)
        }
    }
    @Test fun quarentaDiasDeTentativasNaoUltrapassamIntervaloNemCotas() {
        val inicio=Instant.parse("2026-10-01T00:00:00Z")
        var contador=ContagemMapa();var anterior:Instant?=null
        val dias=mutableMapOf<String,Int>();val meses=mutableMapOf<String,Int>()
        repeat(40*24*60*3) { i ->
            val agora=inicio.plusSeconds(i*20L)
            PoliticaMapasStatic.reservar(contador,agora)?.let { nova ->
                anterior?.let { assertTrue(agora.epochSecond-it.epochSecond>=60) }
                val dia=agora.atZone(ZoneOffset.UTC).toLocalDate().toString();val mes=dia.take(7)
                dias[dia]=(dias[dia] ?: 0)+1;meses[mes]=(meses[mes] ?: 0)+1
                assertTrue(dias.getValue(dia)<=250);assertTrue(meses.getValue(mes)<=8000)
                contador=nova;anterior=agora
            }
        }
        assertEquals(40,dias.size)
        assertTrue(dias.values.all { it==250 })
        assertEquals(7750,meses["2026-10"])
        assertEquals(2250,meses["2026-11"])
    }
}
