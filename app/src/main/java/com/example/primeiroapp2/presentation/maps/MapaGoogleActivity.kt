package com.example.primeiroapp2.presentation.maps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material.*
import com.example.primeiroapp2.presentation.BusSchedule
import com.example.primeiroapp2.presentation.data.*
import com.example.primeiroapp2.presentation.theme.*
import com.example.primeiroapp2.presentation.tile.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.time.Instant
import java.time.Duration

data class PedidoMapa(val config: TileConfig,val mapa: TileMapa?,val validoAte: Long?,val horario: BusSchedule?=null,val veiculoId: String?=null)
fun abrirMapaGoogle(context: Context,pedido: PedidoMapa) {
    context.startActivity(Intent(context,MapaGoogleActivity::class.java).putExtra("pedido",Gson().toJson(pedido)))
}
class MapaGoogleActivity: ComponentActivity() {
    private var pedido by mutableStateOf<PedidoMapa?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pedido=runCatching { Gson().fromJson(intent.getStringExtra("pedido"),PedidoMapa::class.java) }.getOrNull()
        if(pedido==null) { finish();return }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            var viagem: ViagemConfirmada?=null
            while(isActive) {
                val atual=pedido ?: break
                try {
                    if(atual.horario!=null) {
                        val v=viagem ?: BusRepository.confirmarViagem(atual.config.linha.id,atual.config.sentido,atual.horario).also { viagem=it }
                        require(v.data==java.time.LocalDate.now(RastreamentoCorreto.fuso))
                        val bus=BusRepository.buscarVeiculoCorreto(v,atual.veiculoId)
                        val agora=Instant.now();val valido=bus!=null && RastreamentoCorreto.gpsValido(bus,agora)
                        pedido=atual.copy(mapa=TileMapa(v.detalhes.shape,v.detalhes.stops,if(valido) RastreamentoCorreto.Coordenada(bus!!.lat,bus.lng) else null),
                            validoAte=if(valido) agora.plusSeconds(121-RastreamentoCorreto.idadeGps(bus!!.positionTime,agora)!!).toEpochMilli() else null,
                            veiculoId=atual.veiculoId ?: bus?.vehicleId)
                    } else {
                        MeuPontoAtualizador.atualizar(this@MapaGoogleActivity)?.join()
                        val store=MeuPontoStore(this@MapaGoogleActivity)
                        if(store.config()?.chave==atual.config.chave) {
                            val r=store.resultado()?.atual(Instant.now())
                            pedido=atual.copy(mapa=r?.mapa?.let { if(r.estado==TileEstado.CHEGADA) it else it.copy(onibus=null) },validoAte=r?.takeIf { it.estado==TileEstado.CHEGADA }?.validoAte()?.times(1000))
                        }
                    }
                } catch(e: CancellationException) { throw e }
                catch(_: Exception) { pedido=atual.copy(mapa=atual.mapa?.copy(onibus=null),validoAte=null) }
                delay(20_000)
            }
        } }
        setContent { Primeiroapp2Theme {
            val atual=pedido ?: return@Primeiroapp2Theme
            var agora by remember { mutableStateOf(Instant.now()) }
            var aproximar by remember { mutableStateOf(false) }
            var centralizar by remember { mutableStateOf(0) }
            val lifecycle=LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(atual.validoAte,lifecycle) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    agora=Instant.now()
                    atual.validoAte?.let { delay(Duration.between(Instant.now(),Instant.ofEpochMilli(it)).toMillis().coerceAtLeast(0));agora=Instant.now() }
                }
            }
            val valido=atual.mapa?.onibus!=null && atual.validoAte?.let { agora.toEpochMilli()<it }==true
            val mapa=atual.mapa?.let { if(valido) it else it.copy(onibus=null) }
            BackHandler { finish() }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                MapaComAlternativa(atual.config,mapa,Modifier.fillMaxSize(),aproximar=aproximar,interativo=true,recentralizar=centralizar)
                Text("Linha ${atual.config.linha.numero} · Ponto ${atual.config.ponto.sequencia}",color=Amarelo,fontSize=10.sp,textAlign=TextAlign.Center,
                    modifier=Modifier.align(Alignment.TopCenter).padding(top=14.dp).background(Color.Black.copy(alpha=.8f)))
                Column(Modifier.align(Alignment.BottomCenter).padding(horizontal=26.dp,vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    if(!valido) Text("Sem posição recente do ônibus",color=Color.LightGray,fontSize=9.sp,modifier=Modifier.background(Color.Black))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={finish()},modifier=Modifier.weight(1f).height(42.dp),colors=ButtonDefaults.buttonColors(backgroundColor=Superficie)) { Text("Voltar",fontSize=11.sp,color=TextoPrincipal) }
                        Button(onClick={aproximar=!aproximar;centralizar++},modifier=Modifier.weight(1f).height(42.dp),colors=ButtonDefaults.buttonColors(backgroundColor=Superficie)) { Text(if(aproximar) "Trajeto" else "Aproximar",fontSize=11.sp,color=TextoPrincipal) }
                    }
                }
            }
        } }
    }
}
