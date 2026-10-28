package com.example.primeiroapp2.presentation.maps

import java.time.Instant
import java.time.ZoneOffset

data class ContagemMapa(val dia: String="",val mes: String="",val diaUsado: Int=0,val mesUsado: Int=0,val ultimaTentativa: Long=0)
object PoliticaMapasStatic {
    const val LIMITE_DIA=250
    const val LIMITE_MES=8000
    const val INTERVALO_SEGUNDOS=60
    fun reservar(atual: ContagemMapa,agora: Instant): ContagemMapa? {
        val dia=agora.atZone(ZoneOffset.UTC).toLocalDate().toString();val mes=dia.take(7)
        if(atual.dia>dia || agora.epochSecond-atual.ultimaTentativa<INTERVALO_SEGUNDOS) return null
        val usadosDia=if(atual.dia==dia) atual.diaUsado else 0
        val usadosMes=if(atual.mes==mes) atual.mesUsado else 0
        if(usadosDia>=LIMITE_DIA || usadosMes>=LIMITE_MES) return null
        return ContagemMapa(dia,mes,usadosDia+1,usadosMes+1,agora.epochSecond)
    }
}
