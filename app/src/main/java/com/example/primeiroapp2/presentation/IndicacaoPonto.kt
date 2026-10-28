package com.example.primeiroapp2.presentation

/** Mesma tolerância de passagem do rastreamento; proximidade usa distância pelo trajeto. */
enum class IndicacaoPonto(val texto: String) {
    JA_PASSOU("Já passou"), CHEGANDO("Está chegando"), AINDA_NAO_PASSOU("Ainda não passou"), INCERTO("Posição incerta");
    companion object {
        const val LIMITE_CHEGANDO_METROS=600.0
        fun classificar(restante: Double?): IndicacaoPonto = when {
            restante==null || !restante.isFinite() -> INCERTO
            restante < -80 -> JA_PASSOU
            restante <= LIMITE_CHEGANDO_METROS -> CHEGANDO
            else -> AINDA_NAO_PASSOU
        }
    }
}
