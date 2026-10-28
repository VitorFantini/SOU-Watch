package com.example.primeiroapp2.presentation.maps
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto.Coordenada
import org.junit.Assert.*
import org.junit.Test
class GeometriaMapaTest {
    @Test fun pontoCentralCaiNoCentroDaImagem() {
        val v=ViewportMapa.enquadrar(listOf(Coordenada(-22.0,-47.9)))
        val (x,y)=v.pixel(v.centro)
        assertEquals(168.0,x.toDouble(),.01);assertEquals(64.0,y.toDouble(),.01)
    }
    @Test fun enquadraPontoEOnibusSemTrocarNorteOuLeste() {
        val sul=Coordenada(-22.04,-47.91);val norte=Coordenada(-21.98,-47.88)
        val v=ViewportMapa.enquadrar(listOf(sul,norte))
        val a=v.pixel(sul);val b=v.pixel(norte)
        assertTrue(a.first<b.first);assertTrue(a.second>b.second)
        for(p in listOf(a,b)) { assertTrue(p.first in 0f..336f);assertTrue(p.second in 0f..128f) }
    }
    @Test fun zoomAproximaPontoIsoladoELimitaExtremos() {
        assertEquals(17,ViewportMapa.enquadrar(listOf(Coordenada(-22.0,-47.9))).zoom)
        assertTrue(ViewportMapa.mercatorY(90.0).isFinite())
        assertTrue(ViewportMapa.mercatorY(-90.0).isFinite())
        assertThrows(IllegalArgumentException::class.java) { ViewportMapa.enquadrar(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { ViewportMapa.enquadrar(listOf(Coordenada(Double.NaN,0.0))) }
    }
    @Test fun enquadramentoIncluiCurvaMasNaoRestanteDaLinha() {
        val bus=Coordenada(-22.0,-47.90)
        val curva=Coordenada(-21.99,-47.89)
        val ponto=Coordenada(-22.0,-47.88)
        val longe=Coordenada(-22.10,-47.8)
        val trecho=pontosDoEnquadramento(ponto,bus,listOf(bus,curva,ponto,longe),true)
        assertTrue(trecho.containsAll(listOf(bus,curva,ponto)))
        assertFalse(trecho.contains(longe))
        val viewport=ViewportMapa.enquadrar(trecho,204,104)
        assertTrue(viewport.zoom in 10..16)
        trecho.forEach { val (x,y)=viewport.pixel(it);assertTrue(x in 32f..172f);assertTrue(y in 32f..72f) }
    }
    @Test fun semGpsFocaNoPontoETrajetoCompletoIncluiOnibus() {
        val ponto=Coordenada(-22.0,-47.9);val bus=Coordenada(-22.01,-47.91)
        val caminho=listOf(Coordenada(-22.02,-47.92),ponto)
        assertEquals(listOf(ponto),pontosDoEnquadramento(ponto,null,caminho,true))
        assertEquals(caminho,pontosDoEnquadramento(ponto,bus,caminho,false))
    }
    @Test fun todosOsPontosMantemTrajetoEOnibusDentroDoPreviewMaisProximo() {
        val caminho=(0..40).map { i -> Coordenada(-22.03+i*.001,-47.90+kotlin.math.sin(i*.2)*.01) }
        val bus=Coordenada(-22.015,-47.895)
        caminho.forEach { ponto ->
            val pontos=pontosDoEnquadramento(ponto,bus,caminho,false)
            val normal=ViewportMapa.enquadrar(pontos,204,204)
            val perto=ViewportMapa.enquadrar(pontos,204,204,margem=24)
            assertTrue(perto.zoom>=normal.zoom)
            assertTrue(perto.zoom>=10)
            pontos.forEach {
                val (x,y)=perto.pixel(it)
                assertTrue(x in 23.9f..180.1f)
                assertTrue(y in 23.9f..180.1f)
            }
        }
    }
    @Test fun trajetoCompletoNaoMudaZoomNemCentroAoTrocarPontoOuOnibus() {
        val caminho=listOf(Coordenada(-22.04,-47.92),Coordenada(-22.0,-47.88))
        val a=pontosDoEnquadramento(caminho.first(),caminho.last(),caminho,false)
        val b=pontosDoEnquadramento(Coordenada(-22.1,-48.0),Coordenada(-22.2,-48.1),caminho,false)
        assertEquals(ViewportMapa.enquadrar(a),ViewportMapa.enquadrar(b))
    }
}
