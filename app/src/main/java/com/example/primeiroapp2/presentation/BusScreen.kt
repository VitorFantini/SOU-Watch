package com.example.primeiroapp2.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.primeiroapp2.presentation.data.BusLine
import com.example.primeiroapp2.presentation.data.BusStop
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler // <--- NOVO IMPORT NECESSÁRIO
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity

@Composable
fun BusScreen(
    viewModel: MainViewModel = viewModel()
) {
    val state = viewModel.uiState


    val successColor = Color(0xFF4CAF50)
    val regularColor = Color.LightGray


    val context = LocalContext.current

    // =================================================================
    // 1.  BOTÃO VOLTAR DO SISTEMA (BackHandler)
    // =================================================================

    // Define se o BackHandler deve estar ativo (ativo se NÃO estiver na primeira tela)
    val isFirstScreen = state.linhaSelecionada == null

    BackHandler(enabled = !isFirstScreen) {
        // Se a função de voltar do sistema for chamada e NÃO estiver na primeira tela,
        // chamamos a função voltar do ViewModel para retornar ao estado anterior.
        viewModel.voltar()
    }
    // =================================================================

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            // TELA 1: LINHAS
            state.linhaSelecionada == null -> {
                ListaDeLinhas(state.linhasDisponiveis, { viewModel.selecionarLinha(it) })
            }

            // TELA 1.5: SENTIDO
            state.sentidoSelecionado == null -> {
                TelaEscolhaSentido(
                    { viewModel.selecionarSentido(0) },
                    { viewModel.selecionarSentido(1) },
                    { viewModel.voltar() }
                )
            }

            // TELA 2: HORÁRIOS
            state.horarioSelecionado == null -> {
                if (state.horariosDisponiveis.isEmpty()) {
                    TelaAviso("Sem horários", "Tente outro sentido")
                    Button(onClick = { viewModel.voltar() }, modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)) { Text("<") }
                } else {
                    ListaDeHorarios(state.horariosDisponiveis, state.linhaSelecionada!!, if(state.sentidoSelecionado == 0) "Ida" else "Volta", { viewModel.selecionarHorario(it) }, { viewModel.voltar() })
                }
            }

            // TELA 3: PONTOS
            state.pontoSelecionado == null -> {
                ListaDePontos(
                    state.pontosDisponiveis,
                    state.favoritos,
                    { viewModel.selecionarPonto(it) },
                    { viewModel.toggleFavorito(it) },
                    { viewModel.voltar() }
                )
            }

            // TELA 4: RASTREAMENTO
            else -> {

                // 1. Lógica para abrir o Google Maps
                val mapsUrl = state.mapsRouteUrl
                fun openMaps() {
                    if (mapsUrl != null) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(context, intent, null)
                        } catch (e: Exception) {
                            Log.e("MAPS_INTENT", "Falha ao abrir o Maps: ${e.message}")
                        }
                    }
                }

                // 2. Indicador de Progresso Circular
                CircularProgressIndicator(
                    progress = if (state.veiculoMaisProximo != null) 1f else 0.5f,
                    modifier = Modifier.fillMaxSize(),
                    indicatorColor = if (state.veiculoMaisProximo != null) successColor else Color.Yellow,
                    trackColor = Color.DarkGray,
                    strokeWidth = 4.dp
                )

                // 3. Conteúdo Central (Dados de ETA)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(15.dp)
                ) {

                    // --- NOME DO PONTO (CLICÁVEL) ---
                    Text(
                        text = state.pontoSelecionado!!.nome,
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                            .clickable(
                                enabled = mapsUrl != null
                            ) {
                                openMaps()
                            },
                        color = if (mapsUrl != null) successColor else regularColor,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "Bus",
                        tint = if (state.veiculoMaisProximo != null) successColor else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )

                    if (state.veiculoMaisProximo != null) {
                        // DISTÂNCIA (GRANDE)
                        Text(
                            text = state.distanciaAtePonto,
                            color = successColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // TEMPO ESTIMADO
                        Text(
                            text = state.tempoEstimado,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )

                        // BLOCO DE DEBUG DA LOCALIZAÇÃO DO ÔNIBUS
                        val busLat = state.veiculoMaisProximo!!.lat
                        val busLng = state.veiculoMaisProximo!!.lng

                        Text(
                            text = "Lat: ${String.format("%.4f", busLat)}",
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Lng: ${String.format("%.4f", busLng)}",
                            fontSize = 8.sp,
                            color = Color.Gray
                        )

                        // Dica visual
                        if (mapsUrl != null) {
                            Text(
                                text = "Toque no ponto para rota",
                                color = Color.Yellow,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                    } else {
                        // Status de Busca
                        Text(state.status, color = Color.Yellow, fontSize = 12.sp, modifier = Modifier.padding(top=5.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // BOTÃO ENCERRAR
                    Button(
                        onClick = { viewModel.resetar() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F)),
                        modifier = Modifier.height(35.dp).width(100.dp).clip(RoundedCornerShape(50))
                    ) {
                        Text("Encerrar", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}


// --- FUNÇÕES AUXILIARES (COMPONENTS) ---

@Composable
fun TelaEscolhaSentido(onIdaClick: () -> Unit, onVoltaClick: () -> Unit, onVoltar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Qual direção?", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
        Button(onClick = onIdaClick, modifier = Modifier.fillMaxWidth(0.9f), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1976D2))) { Text("IDA (Bairro/UFSCar)") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onVoltaClick, modifier = Modifier.fillMaxWidth(0.9f), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))) { Text("VOLTA (Centro)") }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onVoltar, modifier = Modifier.size(30.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) { Text("<") }
    }
}

@Composable
fun TelaAviso(titulo: String, subtitulo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text(text = titulo, fontSize = 18.sp, color = Color.White)
        Text(text = subtitulo, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ListaDeLinhas(linhas: List<BusLine>, onLinhaClick: (BusLine) -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), anchorType = ScalingLazyListAnchorType.ItemStart) {
        item { Text("Selecione a Linha", color = Color(0xFFFFC107), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        items(linhas) { linha ->
            Button(onClick = { onLinhaClick(linha) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2D2D2D)), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(linha.numero, fontSize = 20.sp, color = Color(0xFF2196F3), modifier = Modifier.padding(end = 12.dp))
                    Text(linha.nome, fontSize = 12.sp, color = Color.White, maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun ListaDeHorarios(horarios: List<BusSchedule>, linha: BusLine, sentido: String, onHorarioClick: (BusSchedule) -> Unit, onVoltar: () -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), anchorType = ScalingLazyListAnchorType.ItemStart) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onVoltar, modifier = Modifier.size(24.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) { Text("<", fontSize = 10.sp) }
                Text("Linha ${linha.numero} - $sentido", color = Color.Gray, fontSize = 12.sp)
            }
        }
        items(horarios) { horario ->
            Button(onClick = { onHorarioClick(horario) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(horario.horaSaida, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListaDePontos(pontos: List<BusStop>, favoritos: Set<String>, onPontoClick: (BusStop) -> Unit, onPontoLongClick: (BusStop) -> Unit, onVoltar: () -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), anchorType = ScalingLazyListAnchorType.ItemStart) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onVoltar, modifier = Modifier.size(24.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) { Text("<", fontSize = 10.sp) }
                Text("Segure para fixar", color = Color.Gray, fontSize = 10.sp)
            }
        }
        items(pontos) { ponto ->
            val isFavorito = favoritos.contains(ponto.nome)
            Box(
                modifier = Modifier.fillMaxWidth().height(55.dp).padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isFavorito) Color(0xFF1A237E) else Color(0xFF2D2D2D))
                    .combinedClickable(onClick = { onPontoClick(ponto) }, onLongClick = { onPontoLongClick(ponto) })
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${ponto.sequencia}.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(end = 5.dp))
                    if (isFavorito) Icon(Icons.Default.PushPin, "Fixo", tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                    else Icon(Icons.Default.LocationOn, "Ponto", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(ponto.nome, fontSize = 12.sp, color = if (isFavorito) Color(0xFFBBDEFB) else Color.White, maxLines = 2)
                }
            }
        }
    }
}