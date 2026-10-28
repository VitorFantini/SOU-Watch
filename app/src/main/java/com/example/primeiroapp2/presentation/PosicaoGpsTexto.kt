package com.example.primeiroapp2.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material.Text
import com.example.primeiroapp2.presentation.data.IdadeGps
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.Instant

/** Só este texto se atualiza; não consulta a rede nem recria o mapa. */
@Composable fun PosicaoGpsTexto(horarioGps: String?=null,gpsEm: Instant?=null) {
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var agora by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(lifecycle,horarioGps,gpsEm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(isActive) { agora=Instant.now();delay(1000) }
        }
    }
    val idade=gpsEm?.let { Duration.between(it,agora).seconds.coerceAtLeast(0) }
        ?: RastreamentoCorreto.idadeGps(horarioGps,agora)
    Text(IdadeGps.texto(idade),fontSize=9.sp,textAlign=TextAlign.Center,
        color=if(idade!=null && idade in 60..120) Color(0xFFFFD166) else Color.LightGray)
}
