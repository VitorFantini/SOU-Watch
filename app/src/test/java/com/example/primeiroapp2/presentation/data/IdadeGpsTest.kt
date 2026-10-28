package com.example.primeiroapp2.presentation.data
import org.junit.Assert.*
import org.junit.Test
class IdadeGpsTest {
    @Test fun mostraSegundosSemConfundirComTempoDeChegada() {
        assertEquals("Posição há 0 s",IdadeGps.texto(0))
        assertEquals("Posição há 45 s",IdadeGps.texto(45))
        assertEquals("Posição há 120 s",IdadeGps.texto(120))
    }
    @Test fun distinguePosicaoVencidaEHorarioIndisponivel() {
        assertEquals("GPS desatualizado",IdadeGps.texto(121))
        assertEquals("Idade do GPS indisponível",IdadeGps.texto(null))
        assertEquals("Idade do GPS indisponível",IdadeGps.texto(-1))
    }
}
