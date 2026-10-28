package com.example.primeiroapp2.presentation.data
object IdentidadeFavorito {
    fun chave(linha: String, sentido: Int, ponto: BusStop) = "v2:$linha:$sentido:${ponto.stopId ?: "${ponto.lat},${ponto.lng}"}"
    // Um nome antigo ambíguo não pode escolher silenciosamente uma parada.
    fun migrar(antigos: Set<String>, linha: String, sentido: Int, pontos: List<BusStop>): Set<String> =
        antigos.flatMap { nome ->
            val candidatos=pontos.filter { it.nome==nome }
            if(!nome.startsWith("v2:") && candidatos.size==1) listOf(chave(linha,sentido,candidatos.single())) else listOf(nome)
        }.toSet()
}
