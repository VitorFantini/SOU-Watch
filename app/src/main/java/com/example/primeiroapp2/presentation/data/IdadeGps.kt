package com.example.primeiroapp2.presentation.data

object IdadeGps {
    fun texto(segundos: Long?): String = when {
        segundos==null || segundos<0 -> "Idade do GPS indisponível"
        segundos>120 -> "GPS desatualizado"
        else -> "Posição há $segundos s"
    }
}
