package com.example.primeiroapp2.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object BusUtils {

    fun abrirGoogleMaps(context: Context, latOrigem: Double, lngOrigem: Double, latDestino: Double, lngDestino: Double) {
        try {
            // Cria uma Intent de rota: saddr (Source Address) -> daddr (Destination Address)
            val uriString = "http://maps.google.com/maps?saddr=$latOrigem,$lngOrigem&daddr=$latDestino,$lngDestino&mode=d"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))

            // Tenta forçar o Google Maps para melhor experiência
            intent.setPackage("com.google.android.apps.maps")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Google Maps não instalado", Toast.LENGTH_SHORT).show()
            // Tenta abrir no navegador como fallback
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$latOrigem,$lngOrigem&destination=$latDestino,$lngDestino"))
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(browserIntent)
        }
    }
}