package com.example.primeiroapp2.presentation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material.Text
import com.example.primeiroapp2.presentation.theme.Amarelo
import com.example.primeiroapp2.presentation.tile.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable fun TelaAcompanhamento(state: BusUiState,onVoltar: ()->Unit,onEncerrar: ()->Unit) {
    val ponto=state.pontoSelecionado ?: return
    val linha=state.linhaSelecionada ?: return
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var agora by remember { mutableStateOf(Instant.now()) }
    var aproximar by remember(ponto.stopId,ponto.sequencia) { mutableStateOf(true) }
    LaunchedEffect(state.gpsValidoAte,lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            agora=Instant.now()
            state.gpsValidoAte?.let {
                delay(Duration.between(Instant.now(),it).toMillis().coerceAtLeast(0))
                agora=Instant.now()
            }
        }
    }
    val gpsRecente=state.veiculoMaisProximo!=null && state.gpsValidoAte?.isAfter(agora)==true
    val indicacao=if(gpsRecente) IndicacaoPonto.classificar(state.metrosRestantes) else IndicacaoPonto.INCERTO
    val cor=when(indicacao) {
        IndicacaoPonto.JA_PASSOU -> Color(0xFFFF6B6B)
        IndicacaoPonto.CHEGANDO -> Color(0xFF00E676)
        IndicacaoPonto.AINDA_NAO_PASSOU -> Color(0xFFA5D6A7)
        IndicacaoPonto.INCERTO -> Color.LightGray
    }
    val config=TileConfig(linha,state.sentidoSelecionado ?: 0,ponto)
    val mapa=state.mapaAcompanhamento?.let { if(gpsRecente) it else it.copy(onibus=null) }
    ListaRelogio(espacoSuperior=20.dp) {
        item {
            Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()) {
                Text("Ponto ${ponto.sequencia} · Linha ${linha.numero}",color=Amarelo,fontSize=11.sp,
                    textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
                Text(ponto.nome,color=Color.White,fontSize=12.sp,textAlign=TextAlign.Center,
                    modifier=Modifier.fillMaxWidth().padding(top=4.dp))
                if(gpsRecente) {
                    val bus=state.veiculoMaisProximo!!
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color=Color(0xFF69E88C))) { append("Ônibus ${bus.vehicleId}") }
                        append(" · saída ${bus.startTime.orEmpty()}")
                    },color=Color.LightGray,fontSize=10.sp,textAlign=TextAlign.Center,
                        modifier=Modifier.fillMaxWidth().padding(top=5.dp))
                }
                Text(if(gpsRecente) indicacao.texto else if(state.veiculoMaisProximo!=null) "GPS desatualizado. Aguardando nova posição." else state.status,
                    color=cor,fontSize=13.sp,fontWeight=FontWeight.Medium,textAlign=TextAlign.Center,
                    modifier=Modifier.fillMaxWidth().padding(top=6.dp))
                if(gpsRecente) {
                    Text(state.distanciaAtePonto,color=Color.White,fontSize=28.sp,modifier=Modifier.padding(top=3.dp))
                    Text(state.tempoEstimado,color=Color.LightGray,fontSize=12.sp,textAlign=TextAlign.Center)
                }
            }
        }
        if(mapa!=null) {
            item {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Canvas(Modifier.fillMaxWidth().height(92.dp).clipToBounds()
                        .semantics { contentDescription="Mapa esquemático: amarelo é o ponto, verde é o ônibus com GPS recente" }
                        .clickable(role=Role.Button,onClickLabel=if(aproximar) "Ver trajeto completo" else "Aproximar ônibus e ponto") { aproximar=!aproximar }) {
                        MeuPontoMapaRenderer.desenharMapa(drawContext.canvas.nativeCanvas,config,mapa,size.width,size.height,aproximar)
                    }
                    Text(if(aproximar) "Toque no mapa para ver o trajeto todo" else "Toque no mapa para aproximar",color=Color.Gray,fontSize=9.sp,textAlign=TextAlign.Center)
                    Text("Amarelo: ponto · Verde: ônibus",color=Color.LightGray,fontSize=9.sp,textAlign=TextAlign.Center)
                }
            }
        }
        if(gpsRecente && state.mapsRouteUrl!=null) item { Acao("Abrir no Maps") {
            try { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(state.mapsRouteUrl))) }
            catch(e: Exception) { Toast.makeText(context,"Nenhum aplicativo disponível para abrir o mapa",Toast.LENGTH_LONG).show() }
        } }
        item { Acao("Voltar aos pontos",onClick=onVoltar) }
        item { Acao("Encerrar",onClick=onEncerrar) }
    }
}
