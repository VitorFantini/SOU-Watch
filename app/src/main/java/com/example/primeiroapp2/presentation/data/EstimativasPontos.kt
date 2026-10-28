package com.example.primeiroapp2.presentation.data

import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max

/** Projeta as paradas uma vez e usa uma única posição do mesmo ônibus para toda a lista. */
class EstimativasPontos(detalhes: TripDetails) {
    private val caminho=runCatching { RastreamentoCorreto.decodificar(detalhes.shape.orEmpty()) }.getOrDefault(emptyList())
    private val paradas=RastreamentoCorreto.pontosDoTrajeto(detalhes).mapNotNull { ponto ->
        runCatching { RastreamentoCorreto.projetar(RastreamentoCorreto.Coordenada(ponto.lat,ponto.lng),caminho) }
            .getOrNull()?.takeIf { !it.ambigua && it.afastamento<=150 }?.let { ponto.sequencia to it.progresso }
    }.toMap()
    fun calcular(bus: BusVehicle?, agora: Instant): Map<Int,Int> {
        if(bus==null || !RastreamentoCorreto.gpsValido(bus,agora)) return emptyMap()
        val pos=runCatching { RastreamentoCorreto.projetar(RastreamentoCorreto.Coordenada(bus.lat,bus.lng),caminho) }.getOrNull() ?: return emptyMap()
        if(pos.ambigua || pos.afastamento>150) return emptyMap()
        return paradas.mapNotNull { (sequencia,progresso) ->
            val restante=progresso-pos.progresso
            if(restante>=0) sequencia to max(1,ceil(restante/300).toInt()) else null
        }.toMap()
    }
}
