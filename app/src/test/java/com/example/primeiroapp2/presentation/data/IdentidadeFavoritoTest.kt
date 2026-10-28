package com.example.primeiroapp2.presentation.data
import org.junit.Assert.*
import org.junit.Test
class IdentidadeFavoritoTest {
    private val a=BusStop(1,"Biblioteca",-22.0,-47.0,1)
    private val b=a.copy(sequencia=2,stopId=2)
    @Test fun nomesIguaisNaoCompartilhamFavorito() {
        assertNotEquals(IdentidadeFavorito.chave("01",0,a),IdentidadeFavorito.chave("01",0,b))
        assertNotEquals(IdentidadeFavorito.chave("01",0,a),IdentidadeFavorito.chave("01",1,a))
        assertNotEquals(IdentidadeFavorito.chave("01",0,a),IdentidadeFavorito.chave("06",0,a))
    }
    @Test fun migracaoNaoEscolheParadaAmbigua() {
        val result=IdentidadeFavorito.migrar(setOf("Biblioteca"),"01",0,listOf(a,b))
        assertFalse(IdentidadeFavorito.chave("01",0,a) in result)
        assertFalse(IdentidadeFavorito.chave("01",0,b) in result)
    }
    @Test fun migracaoPreservaOutrosEConverteNomeUnico() {
        val result=IdentidadeFavorito.migrar(setOf("Biblioteca","Outro"),"01",0,listOf(a))
        assertEquals(setOf(IdentidadeFavorito.chave("01",0,a),"Outro"),result)
    }
}
