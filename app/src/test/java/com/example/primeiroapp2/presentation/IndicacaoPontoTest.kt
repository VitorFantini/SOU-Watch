package com.example.primeiroapp2.presentation
import org.junit.Assert.*
import org.junit.Test
class IndicacaoPontoTest {
    @Test fun soUsaChegandoPertoDoPonto() {
        assertEquals(IndicacaoPonto.AINDA_NAO_PASSOU,IndicacaoPonto.classificar(601.0))
        assertEquals(IndicacaoPonto.CHEGANDO,IndicacaoPonto.classificar(600.0))
        assertEquals(IndicacaoPonto.CHEGANDO,IndicacaoPonto.classificar(0.0))
    }
    @Test fun passagemMantemToleranciaEspacial() {
        assertEquals(IndicacaoPonto.CHEGANDO,IndicacaoPonto.classificar(-80.0))
        assertEquals(IndicacaoPonto.JA_PASSOU,IndicacaoPonto.classificar(-81.0))
    }
    @Test fun semProjecaoConfiavelFicaNeutro() {
        assertEquals(IndicacaoPonto.INCERTO,IndicacaoPonto.classificar(null))
        assertEquals(IndicacaoPonto.INCERTO,IndicacaoPonto.classificar(Double.NaN))
        assertEquals(IndicacaoPonto.INCERTO,IndicacaoPonto.classificar(Double.POSITIVE_INFINITY))
    }
}
