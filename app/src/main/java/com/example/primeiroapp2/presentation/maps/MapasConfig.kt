package com.example.primeiroapp2.presentation.maps

import android.content.Context
import com.example.primeiroapp2.BuildConfig
import com.example.primeiroapp2.R
import java.time.Instant

object MapasConfig {
    fun chaveAndroid(context: Context)=context.getString(R.string.maps_android_api_key).trim()
    fun chaveStatic()=BuildConfig.MAPS_STATIC_API_KEY.trim()
    @Synchronized fun reservarStatic(context: Context,agora: Instant): Boolean {
        val prefs=context.applicationContext.getSharedPreferences("google_maps_uso",Context.MODE_PRIVATE)
        val atual=ContagemMapa(prefs.getString("dia","").orEmpty(),prefs.getString("mes","").orEmpty(),prefs.getInt("dia_usado",0),prefs.getInt("mes_usado",0),prefs.getLong("ultima",0))
        val novo=PoliticaMapasStatic.reservar(atual,agora) ?: return false
        return prefs.edit().putString("dia",novo.dia).putString("mes",novo.mes).putInt("dia_usado",novo.diaUsado)
            .putInt("mes_usado",novo.mesUsado).putLong("ultima",novo.ultimaTentativa).commit()
    }
}
