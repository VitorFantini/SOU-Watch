package com.example.primeiroapp2.presentation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.wear.compose.material.*
import com.example.primeiroapp2.presentation.data.*
import com.example.primeiroapp2.presentation.tile.*
import com.example.primeiroapp2.presentation.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable fun ListaRelogio(espacoSuperior: Dp=28.dp, content: ScalingLazyListScope.() -> Unit) {
    val scroll=rememberScalingLazyListState(initialCenterItemIndex=0)
    Scaffold(positionIndicator={ PositionIndicator(scalingLazyListState=scroll) }) {
        ScalingLazyColumn(state=scroll,autoCentering=null,anchorType=ScalingLazyListAnchorType.ItemStart,modifier=Modifier.fillMaxSize().background(Color.Black),
            contentPadding=PaddingValues(start=18.dp,end=18.dp,top=espacoSuperior,bottom=28.dp),
            horizontalAlignment=Alignment.CenterHorizontally,content=content)
    }
}
@Composable fun Acao(texto: String, destaque: String? = null, corDestaque: Color = TextoPrincipal, onClick: () -> Unit) {
    Box(modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).clip(RoundedCornerShape(22.dp))
        .background(Superficie).clickable(onClick=onClick).padding(horizontal=14.dp,vertical=12.dp),contentAlignment=Alignment.Center) {
        val rotulo=buildAnnotatedString {
            if(destaque!=null && texto.startsWith(destaque)) {
                withStyle(SpanStyle(color=corDestaque)) { append(destaque) }
                append(texto.substring(destaque.length))
            } else append(texto)
        }
        Text(rotulo,color=TextoPrincipal,fontSize=14.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
    }
}
@Composable fun Legenda(texto: String, cor: Color=Color.LightGray) {
    Text(texto,color=cor,fontSize=12.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(vertical=3.dp))
}
@Composable fun BusScreen(viewModel: MainViewModel=viewModel(),onConfigurarPonto: ((TileConfig)->Unit)?=null) {
    val s=viewModel.uiState
    val context=LocalContext.current
    val lifecyclePrevisoes=LocalLifecycleOwner.current.lifecycle
    var agoraPrevisoes by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(s.previsoesValidasAte,lifecyclePrevisoes) {
        lifecyclePrevisoes.repeatOnLifecycle(Lifecycle.State.STARTED) {
            agoraPrevisoes=Instant.now()
            s.previsoesValidasAte?.let { validade ->
                delay(java.time.Duration.between(Instant.now(),validade).toMillis().coerceAtLeast(0))
                agoraPrevisoes=Instant.now()
            }
        }
    }
    BackHandler(enabled=s.linhaSelecionada!=null) { viewModel.voltar() }
    when {
        s.linhaSelecionada==null -> ListaRelogio {
            item { Legenda(if(onConfigurarPonto!=null) "Configurar Meu ponto" else "SOU WATCH",if(onConfigurarPonto==null) Amarelo else TextoPrincipal) }
            if(onConfigurarPonto==null) item { Acao("Meu ponto · Tile") { context.startActivity(Intent(context,MeuPontoSetupActivity::class.java)) } }
            items(s.linhasDisponiveis) { linha -> Acao("${linha.numero} · ${linha.nome}") { viewModel.selecionarLinha(linha) } }
        }
        s.sentidoSelecionado==null -> ListaRelogio {
            item { Legenda("Linha ${s.linhaSelecionada.numero}",Amarelo) }
            item { Legenda(s.linhaSelecionada.nome,Amarelo) }
            item { Legenda("Escolha o sentido") }
            item { Acao("Ida${s.percursos[0]?.let { " · $it" } ?: ""}",destaque="Ida",corDestaque=Color(0xFF8AB4F8)) { viewModel.selecionarSentido(0) } }
            item { Acao("Volta${s.percursos[1]?.let { " · $it" } ?: ""}",destaque="Volta",corDestaque=Color(0xFF81C995)) { viewModel.selecionarSentido(1) } }
            item { Acao("Voltar") { viewModel.voltar() } }
        }
        s.horarioSelecionado==null -> {
            var agora by remember { mutableStateOf(Instant.now()) }
            val lifecycle=LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while(true) { agora=Instant.now(); delay(15_000) }
            } }
            ListaRelogio {
                item { Legenda("Linha ${s.linhaSelecionada.numero} · ${if(s.sentidoSelecionado==0) "Ida" else "Volta"}",TextoPrincipal) }
                item { Legenda("Agora ${agora.atZone(RastreamentoCorreto.fuso).format(DateTimeFormatter.ofPattern("HH:mm"))}") }
                if(s.isErro || s.horariosDisponiveis.isEmpty()) item { Legenda(s.status) }
                if(s.isErro && s.horariosDisponiveis.isNotEmpty()) item { Legenda("Lista anterior · consulta indisponível") }
                if(onConfigurarPonto!=null) item { Legenda("Escolha o percurso. O Tile acompanha as próximas saídas.") }
                items(s.horariosDisponiveis) { h ->
                    val c=s.checagensHorarios[h.tripId]
                    val estado=c?.estadoEm(agora)
                    val mensagem=if(s.isErro) "Consulta indisponível" else if(estado==EstadoHorario.GPS_ANTIGO) "GPS antigo" else c?.mensagem ?: "Consultando ônibus…"
                    val localizado=!s.isErro && estado==EstadoHorario.LOCALIZADO
                    Chip(onClick={viewModel.selecionarHorario(h)},label={Text(h.horaSaida,color=if(localizado) Color(0xFF69E88C) else TextoPrincipal)},
                        secondaryLabel={Text(mensagem,color=if(localizado) Color(0xFFB0E3BE) else Color.LightGray)},
                        icon=if(localizado) ({ Icon(Icons.Default.DirectionsBus,contentDescription="Ônibus localizado",tint=Color(0xFF69E88C),modifier=Modifier.size(22.dp)) }) else null,
                        modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),colors=ChipDefaults.chipColors(backgroundColor=if(localizado) Color(0xFF123020) else Superficie))
                }
                if(!s.carregandoHorarios) item { Acao("Atualizar") { viewModel.tentarNovamente() } }
                item { Acao("Voltar · trocar sentido") { viewModel.voltar() } }
            }
        }
        s.pontoSelecionado==null -> ListaRelogio {
            item { Legenda("Linha ${s.linhaSelecionada.numero} · ${if(s.sentidoSelecionado==0) "Ida" else "Volta"}",TextoPrincipal) }
            item { Legenda(if(onConfigurarPonto!=null) "Toque para escolher seu ponto" else "Toque para acompanhar. Segure para favoritar.") }
            if(s.pontosDisponiveis.isEmpty()) item { Legenda(s.status) }
            if(onConfigurarPonto==null && s.statusPrevisoes.isNotBlank()) item { Legenda(s.statusPrevisoes) }
            items(s.pontosDisponiveis) { ponto ->
                PontoItem(ponto,viewModel.chaveFavorito(ponto) in s.favoritos,
                    if(onConfigurarPonto==null && s.previsoesValidasAte?.isAfter(agoraPrevisoes)==true) s.minutosPorPonto[ponto.sequencia] else null,
                    { if(onConfigurarPonto!=null) onConfigurarPonto(TileConfig(s.linhaSelecionada,s.sentidoSelecionado,ponto)) else viewModel.selecionarPonto(ponto) },
                    { if(onConfigurarPonto==null) viewModel.toggleFavorito(ponto) })
            }
            if(s.isErro && !s.carregandoPontos) item { Acao("Tentar novamente") { viewModel.tentarNovamente() } }
            item { Acao("Voltar") { viewModel.voltar() } }
        }
        else -> TelaAcompanhamento(s,onVoltar={viewModel.voltar()},onEncerrar={viewModel.resetar()})
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun PontoItem(ponto: BusStop,favorito: Boolean,minutos: Int?,onClick: ()->Unit,onLongClick: ()->Unit) {
    Column(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Superficie)
        .combinedClickable(onClick=onClick,onLongClick=onLongClick,onLongClickLabel="Favoritar ou remover favorito")
        .padding(12.dp).heightIn(min=48.dp)) {
        Text("Ponto ${ponto.sequencia}${if(favorito) " · Favorito" else ""}",color=TextoPrincipal,fontSize=11.sp)
        Text(buildAnnotatedString {
            append(ponto.nome)
            if(minutos!=null) withStyle(SpanStyle(color=Color(0xFF69E88C))) { append(" · ~$minutos min") }
        },color=Color.White,fontSize=13.sp)
        Text("Parada ${ponto.stopId ?: ponto.sequencia}",color=Color.LightGray,fontSize=10.sp)
    }
}
