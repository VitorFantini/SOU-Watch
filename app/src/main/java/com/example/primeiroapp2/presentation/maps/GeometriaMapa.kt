package com.example.primeiroapp2.presentation.maps

import com.example.primeiroapp2.presentation.data.RastreamentoCorreto.Coordenada
import kotlin.math.*

data class ViewportMapa(val centro: Coordenada,val zoom: Int,val largura: Int,val altura: Int) {
    fun pixel(p: Coordenada): Pair<Float,Float> {
        val mundo=256.0*2.0.pow(zoom)
        var dx=mercatorX(p.lng)-mercatorX(centro.lng)
        if(dx>0.5) dx-=1.0
        if(dx< -0.5) dx+=1.0
        return (largura/2.0+dx*mundo).toFloat() to (altura/2.0+(mercatorY(p.lat)-mercatorY(centro.lat))*mundo).toFloat()
    }
    companion object {
        fun mercatorX(lng: Double)=(lng+180.0)/360.0
        fun mercatorY(lat: Double): Double {
            val r=Math.toRadians(lat.coerceIn(-85.05112878,85.05112878))
            return (1.0-ln(tan(r)+1/cos(r))/Math.PI)/2.0
        }
        fun enquadrar(pontos: List<Coordenada>,largura: Int=336,altura: Int=128,margem: Int=32): ViewportMapa {
            require(pontos.isNotEmpty() && largura>=96 && altura>=96)
            require(margem>=0 && margem*2<min(largura,altura))
            require(pontos.all { it.lat.isFinite() && it.lng.isFinite() && it.lat in -90.0..90.0 && it.lng in -180.0..180.0 })
            val xs=pontos.map { mercatorX(it.lng) };val ys=pontos.map { mercatorY(it.lat) }
            val x=(xs.min()+xs.max())/2;val y=(ys.min()+ys.max())/2
            val centro=Coordenada(Math.toDegrees(atan(sinh(Math.PI*(1-2*y)))),x*360-180)
            val dx=(xs.max()-xs.min()).coerceAtLeast(1e-7);val dy=(ys.max()-ys.min()).coerceAtLeast(1e-7)
            val escala=min((largura-2*margem)/dx,(altura-2*margem)/dy)
            val zoom=floor(log2(escala/256)).toInt().coerceIn(1,17)
            return ViewportMapa(centro,zoom,largura,altura)
        }
    }
}

/** Inclui as curvas entre as posições, sem afastar para o restante da linha. */
fun pontosDoEnquadramento(ponto: Coordenada, onibus: Coordenada?, caminho: List<Coordenada>, aproximar: Boolean): List<Coordenada> {
    val extremos=listOfNotNull(ponto,onibus)
    // No modo Trajeto, mudar de ponto ou mover o ônibus não altera a câmera.
    if(!aproximar) return caminho.takeIf { it.size>=2 } ?: extremos
    if(onibus==null || caminho.size<2) return extremos
    fun indice(local: Coordenada): Int = caminho.indices.minBy { i ->
        val a=caminho[i];val b=caminho[(i+1).coerceAtMost(caminho.lastIndex)]
        val c=cos(Math.toRadians(local.lat))
        val x=(b.lng-a.lng)*c;val y=b.lat-a.lat
        val px=(local.lng-a.lng)*c;val py=local.lat-a.lat
        val t=if(x*x+y*y==0.0) 0.0 else ((px*x+py*y)/(x*x+y*y)).coerceIn(0.0,1.0)
        (px-t*x).pow(2)+(py-t*y).pow(2)
    }
    val a=indice(ponto);val b=indice(onibus)
    // Apenas vértices internos: as extremidades podem ficar no meio de segmentos longos.
    return extremos+caminho.subList(min(a,b)+1,(max(a,b)+1).coerceAtMost(caminho.size))
}
