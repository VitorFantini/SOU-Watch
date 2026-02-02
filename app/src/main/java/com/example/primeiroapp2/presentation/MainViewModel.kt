package com.example.primeiroapp2.presentation

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeiroapp2.presentation.data.LineRepository
import com.example.primeiroapp2.presentation.data.BusDatabase
import com.example.primeiroapp2.presentation.data.BusLine
import com.example.primeiroapp2.presentation.data.BusVehicle
import com.example.primeiroapp2.presentation.data.BusStop
import com.example.primeiroapp2.presentation.data.BusRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// --- CLASSES DE DADOS DEFINIDAS AQUI ---
data class BusSchedule(val tripId: String, val horaSaida: String)

// --- ESTADO ---
data class BusUiState(
    val favoritos: Set<String> = emptySet(),
    val linhasDisponiveis: List<BusLine> = emptyList(),
    val horariosDisponiveis: List<BusSchedule> = emptyList(),
    val pontosDisponiveis: List<BusStop> = emptyList(),
    val linhaSelecionada: BusLine? = null,
    val sentidoSelecionado: Int? = null,
    val horarioSelecionado: BusSchedule? = null,
    val pontoSelecionado: BusStop? = null,
    val veiculoMaisProximo: BusVehicle? = null,
    val distanciaAtePonto: String = "--",
    val tempoEstimado: String = "--",
    val status: String = "Carregando...",
    val isErro: Boolean = false,
    val mapsRouteUrl: String? = null // CAMPO PARA O LINK DO MAPA
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf(BusUiState())
        private set

    private val prefs = application.getSharedPreferences("bus_prefs", Context.MODE_PRIVATE)

    init {
        carregarFavoritos()
        carregarLinhas()
    }

    private fun carregarFavoritos() {
        val salvos = prefs.getStringSet("favoritos", emptySet()) ?: emptySet()
        uiState = uiState.copy(favoritos = salvos)
    }

    fun toggleFavorito(ponto: BusStop) {
        val atuais = uiState.favoritos.toMutableSet()
        if (atuais.contains(ponto.nome)) {
            atuais.remove(ponto.nome)
        } else {
            atuais.add(ponto.nome)
        }
        prefs.edit().putStringSet("favoritos", atuais).apply()
        uiState = uiState.copy(favoritos = atuais)
        reordenarPontos()
    }

    private fun reordenarPontos() {
        val listaAtual = uiState.pontosDisponiveis
        val favoritos = uiState.favoritos
        val listaOrdenada = listaAtual.sortedWith(compareBy(
            { !favoritos.contains(it.nome) },
            { it.sequencia }
        ))
        uiState = uiState.copy(pontosDisponiveis = listaOrdenada)
    }

    private fun carregarLinhas() {
        uiState = uiState.copy(
            linhasDisponiveis = LineRepository.supportedLines,
            status = "Escolha sua linha"
        )
    }

    fun selecionarLinha(linha: BusLine) {
        uiState = uiState.copy(
            linhaSelecionada = linha,
            sentidoSelecionado = null,
            status = "Escolha o sentido",
            horariosDisponiveis = emptyList(),
            pontosDisponiveis = emptyList()
        )
    }

    fun selecionarSentido(sentidoId: Int) {
        val horariosReais = BusDatabase.getSchedulesForLine(uiState.linhaSelecionada!!.id, sentidoId)
        uiState = uiState.copy(
            sentidoSelecionado = sentidoId,
            horariosDisponiveis = horariosReais,
            status = "Selecione o horário"
        )
    }

    fun selecionarHorario(horario: BusSchedule) {
        val pontosReais = BusDatabase.getStopsForLine(
            uiState.linhaSelecionada!!.id,
            uiState.sentidoSelecionado ?: 0,
            horario.horaSaida
        )

        uiState = uiState.copy(
            horarioSelecionado = horario,
            pontosDisponiveis = pontosReais,
            status = "Segure para fixar"
        )
        reordenarPontos()
    }

    fun selecionarPonto(ponto: BusStop) {
        uiState = uiState.copy(
            pontoSelecionado = ponto,
            status = "Localizando ônibus..."
        )
        iniciarRastreamentoGPS()
    }

    private fun iniciarRastreamentoGPS() {
        viewModelScope.launch {
            while (isActive && uiState.pontoSelecionado != null) {
                try {
                    val pontoAlvo = uiState.pontoSelecionado!!
                    val bus = BusRepository.buscarVeiculoEmCascata(uiState.linhaSelecionada?.id ?: "")

                    var mapsUrl: String? = null

                    if (bus != null) {
                        // 1. GERA O URL DO MAPA (Sempre que o ônibus é encontrado)
                        mapsUrl = BusRepository.buildGoogleMapsUrl(
                            origLat = bus.lat,
                            origLng = bus.lng,
                            destLat = pontoAlvo.lat,
                            destLng = pontoAlvo.lng
                        )

                        // 2. BUSCA ROTA E TEMPO (Google Directions API com Fallback)
                        val rotaResult = BusRepository.buscarRotaETempo(
                            origLat = bus.lat,
                            origLng = bus.lng,
                            destLat = pontoAlvo.lat,
                            destLng = pontoAlvo.lng
                        )

                        if (rotaResult != null) {
                            // SUCESSO: Dados reais do Google Directions
                            uiState = uiState.copy(
                                veiculoMaisProximo = bus,
                                distanciaAtePonto = rotaResult.distanceText,
                                tempoEstimado = rotaResult.durationText,
                                status = "Chegando em:",
                                mapsRouteUrl = mapsUrl
                            )
                        } else {
                            // FALLBACK: Google Maps falhou, usa a simulação local
                            val distanciaMetrosSimulada = calcularDistancia(bus.lat, bus.lng, pontoAlvo.lat, pontoAlvo.lng)
                            val minutosSimulados = (distanciaMetrosSimulada / 300).toInt()
                            val textoTempoSimulado = if (minutosSimulados < 1) "Chegando" else "~ $minutosSimulados min"

                            val textoDistanciaSimulada = if (distanciaMetrosSimulada > 1000) {
                                "${String.format("%.1f", distanciaMetrosSimulada / 1000)} km"
                            } else {
                                "${distanciaMetrosSimulada.toInt()} m"
                            }

                            uiState = uiState.copy(
                                veiculoMaisProximo = bus,
                                distanciaAtePonto = textoDistanciaSimulada,
                                tempoEstimado = textoTempoSimulado,
                                status = "Chegando em (Simulado):",
                                mapsRouteUrl = mapsUrl // URL ainda é válido para abrir o mapa
                            )
                        }
                    } else {
                        // Ônibus fora de sinal GPS (Mobilibus falhou)
                        uiState = uiState.copy(
                            veiculoMaisProximo = null,
                            distanciaAtePonto = "--",
                            tempoEstimado = "--",
                            status = "Ônibus fora de sinal GPS",
                            mapsRouteUrl = null
                        )
                    }
                } catch (e: Exception) {
                    uiState = uiState.copy(status = "Erro conexão/lógica: ${e.message}", mapsRouteUrl = null)
                    e.printStackTrace()
                }
                delay(10000)
            }
        }
    }

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val loc1 = Location("").apply { latitude = lat1; longitude = lon1 }
        val loc2 = Location("").apply { latitude = lat2; longitude = lon2 }
        return loc1.distanceTo(loc2)
    }

    fun voltar() {
        val state = uiState
        when {
            state.pontoSelecionado != null -> uiState = state.copy(pontoSelecionado = null, mapsRouteUrl = null)
            state.horarioSelecionado != null -> uiState = state.copy(horarioSelecionado = null, pontosDisponiveis = emptyList(), mapsRouteUrl = null)
            state.sentidoSelecionado != null -> uiState = state.copy(sentidoSelecionado = null, horariosDisponiveis = emptyList(), mapsRouteUrl = null)
            state.linhaSelecionada != null -> resetar()
        }
    }

    fun resetar() {
        uiState = BusUiState(
            linhasDisponiveis = LineRepository.supportedLines,
            status = "Escolha sua linha",
            favoritos = uiState.favoritos
        )
    }
}