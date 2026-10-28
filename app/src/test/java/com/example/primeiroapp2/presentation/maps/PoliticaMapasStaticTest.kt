package com.example.primeiroapp2.presentation.maps
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
class PoliticaMapasStaticTest {
    private val agora=Instant.parse("2026-10-28T12:00:00Z")
    @Test fun reservaAntesDaRequisicaoERespeitaIntervalo() {
        val r=PoliticaMapasStatic.reservar(ContagemMapa(),agora)!!
        assertEquals(1,r.diaUsado);assertEquals(1,r.mesUsado)
        assertNull(PoliticaMapasStatic.reservar(r,agora.plusSeconds(59)))
        assertEquals(2,PoliticaMapasStatic.reservar(r,agora.plusSeconds(60))!!.diaUsado)
    }
    @Test fun limiteDoDiaBloqueiaNovosDownloads() {
        assertNull(PoliticaMapasStatic.reservar(ContagemMapa("2026-10-28","2026-10",250,250),agora))
    }
    @Test fun limiteMensalNaoResetaNaViradaDoDia() {
        assertNull(PoliticaMapasStatic.reservar(ContagemMapa("2026-10-27","2026-10",100,8000),agora))
    }
    @Test fun novaDataResetaSomenteContadorCorreto() {
        val r=PoliticaMapasStatic.reservar(ContagemMapa("2026-10-27","2026-10",250,500),agora)!!
        assertEquals(1,r.diaUsado);assertEquals(501,r.mesUsado)
        val novoMes=PoliticaMapasStatic.reservar(r,Instant.parse("2026-11-01T00:00:00Z"))!!
        assertEquals(1,novoMes.diaUsado);assertEquals(1,novoMes.mesUsado)
    }
    @Test fun relogioVoltandoNaoLiberaNovaCota() {
        val r=PoliticaMapasStatic.reservar(ContagemMapa(),agora)!!
        assertNull(PoliticaMapasStatic.reservar(r,agora.minusSeconds(120)))
        assertNull(PoliticaMapasStatic.reservar(r,agora.minusSeconds(86400)))
    }
}
