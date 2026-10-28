package com.example.primeiroapp2.presentation

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.primeiroapp2.presentation.data.*
import com.example.primeiroapp2.presentation.tile.*
import kotlinx.coroutines.*
import java.time.*
import kotlin.math.*

data class BusSchedule(val tripId: String, val horaSaida: String, val apiTripId: Long? = null)
enum class StatusAproximacao { ANALISANDO, ANTES_PONTO, CHEGANDO, APOS_PONTO, PASSOU }
data class BusUiState(
    val favoritos: Set<String> = emptySet(),
    val percursos: Map<Int,String> = emptyMap(),
    val linhasDisponiveis: List<BusLine> = LineRepository.supportedLines,
    val horariosDisponiveis: List<BusSchedule> = emptyList(),
    val pontosDisponiveis: List<BusStop> = emptyList(),
    val minutosPorPonto: Map<Int,Int> = emptyMap(),
    val previsoesValidasAte: Instant? = null,
    val statusPrevisoes: String = "",
    val checagensHorarios: Map<String,ChecagemHorario> = emptyMap(),
    val carregandoHorarios: Boolean = false, val carregandoPontos: Boolean = false,
    val linhaSelecionada: BusLine? = null, val sentidoSelecionado: Int? = null,
    val horarioSelecionado: BusSchedule? = null, val pontoSelecionado: BusStop? = null,
    val veiculoMaisProximo: BusVehicle? = null, val distanciaAtePonto: String = "--",
    val tempoEstimado: String = "--", val status: String = "Escolha sua linha",
    val isErro: Boolean = false, val mapsRouteUrl: String? = null,
    val mapaAcompanhamento: TileMapa? = null,
    val metrosRestantes: Double? = null,
    val gpsValidoAte: Instant? = null,
    val statusAproximacao: StatusAproximacao = StatusAproximacao.ANALISANDO
)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs=application.getSharedPreferences("bus_prefs",Context.MODE_PRIVATE)
    var uiState by mutableStateOf(BusUiState(favoritos=prefs.getStringSet("favoritos",emptySet())?.toSet().orEmpty()))
        private set
    private var visivel=false
    private var trabalho: Job?=null
    private var viagemAtual: ViagemConfirmada?=null
    private var estimativasPontos: EstimativasPontos?=null
    private var veiculoIdFixado: String?=null
    private var observador=ObservadorPassagem()
    fun definirVisivel(valor: Boolean) {
        if(visivel==valor) return
        visivel=valor; trabalho?.cancel()
        if(valor) retomar() else uiState=uiState.copy(minutosPorPonto=emptyMap(),previsoesValidasAte=null,
            gpsValidoAte=null,veiculoMaisProximo=null,mapaAcompanhamento=uiState.mapaAcompanhamento?.copy(onibus=null))
    }
    private fun retomar() {
        trabalho?.cancel()
        if(!visivel) return
        when {
            uiState.linhaSelecionada!=null && uiState.sentidoSelecionado==null -> carregarSentidos()
            uiState.pontoSelecionado!=null -> rastrear()
            uiState.horarioSelecionado!=null -> if(viagemAtual==null) carregarPontos() else trabalho=viewModelScope.launch { atualizarListaPontos() }
            uiState.sentidoSelecionado!=null && uiState.horarioSelecionado==null -> carregarHorarios()
        }
    }
    private fun limparVinculo() { viagemAtual=null; estimativasPontos=null; veiculoIdFixado=null; observador=ObservadorPassagem() }
    fun toggleFavorito(ponto: BusStop) {
        val favoritos=uiState.favoritos.toMutableSet()
        val chave=chaveFavorito(ponto)
        if(!favoritos.remove(chave)) favoritos.add(chave)
        android.widget.Toast.makeText(getApplication(),if(chave in favoritos) "Ponto favoritado" else "Favorito removido",android.widget.Toast.LENGTH_SHORT).show()
        prefs.edit().putStringSet("favoritos",favoritos).apply()
        uiState=uiState.copy(favoritos=favoritos)
        reordenarPontos()
    }
    fun chaveFavorito(ponto: BusStop)=IdentidadeFavorito.chave(uiState.linhaSelecionada!!.id,uiState.sentidoSelecionado!!,ponto)
    private fun reordenarPontos() { uiState=uiState.copy(pontosDisponiveis=uiState.pontosDisponiveis.sortedWith(compareBy({chaveFavorito(it) !in uiState.favoritos},{it.sequencia}))) }
    fun selecionarLinha(linha: BusLine) {
        trabalho?.cancel(); limparVinculo()
        uiState=BusUiState(favoritos=uiState.favoritos,linhaSelecionada=linha,status="Escolha o sentido")
        retomar()
    }
    private fun carregarSentidos() {
        val linha=uiState.linhaSelecionada ?: return
        trabalho=viewModelScope.launch {
            try {
                val tabela=BusRepository.tabela(linha.id)
                uiState=uiState.copy(percursos=tabela.trips.orEmpty().groupBy { it.directionId }.mapValues { (_,trips) -> trips.map { it.tripDesc }.distinct().joinToString(" / ") })
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { android.util.Log.w("SouWatch","Destinos indisponíveis",e) }
        }
    }
    fun selecionarSentido(sentidoId: Int) {
        trabalho?.cancel(); limparVinculo()
        uiState=uiState.copy(sentidoSelecionado=sentidoId,horarioSelecionado=null,pontoSelecionado=null,horariosDisponiveis=emptyList(),checagensHorarios=emptyMap(),carregandoHorarios=true,status="Carregando horários")
        retomar()
    }
    private fun carregarHorarios() {
        val linha=uiState.linhaSelecionada ?: return; val sentido=uiState.sentidoSelecionado ?: return
        trabalho=viewModelScope.launch {
            while(isActive) {
                try {
                    val agora=Instant.now()
                    val tabela=BusRepository.tabela(linha.id)
                    val horarios=VerificacaoHorarios.filtrar(RastreamentoCorreto.horariosDoDia(tabela,sentido,agora.atZone(RastreamentoCorreto.fuso).toLocalDate()),agora)
                    uiState=uiState.copy(horariosDisponiveis=horarios,carregandoHorarios=false,isErro=false,status=if(horarios.isEmpty()) "Não há partidas confirmadas para hoje." else "Selecione o horário",checagensHorarios=emptyMap())
                    val checagens=VerificacaoHorarios.consultar(horarios,Instant.now())
                    uiState=uiState.copy(checagensHorarios=checagens)
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) { uiState=uiState.copy(carregandoHorarios=false,isErro=true,checagensHorarios=emptyMap(),status=mensagemConsulta(e)) }
                delay(20_000)
            }
        }
    }
    fun selecionarHorario(horario: BusSchedule) {
        trabalho?.cancel(); limparVinculo()
        uiState=uiState.copy(horarioSelecionado=horario,pontosDisponiveis=emptyList(),minutosPorPonto=emptyMap(),previsoesValidasAte=null,statusPrevisoes="",carregandoPontos=true,status="Carregando pontos")
        retomar()
    }
    private fun carregarPontos() {
        val linha=uiState.linhaSelecionada ?: return; val sentido=uiState.sentidoSelecionado ?: return; val h=uiState.horarioSelecionado ?: return
        trabalho=viewModelScope.launch {
            try {
                val viagem=BusRepository.confirmarViagem(linha.id,sentido,h)
                viagemAtual=viagem
                estimativasPontos=withContext(Dispatchers.Default) { EstimativasPontos(viagem.detalhes) }
                uiState=uiState.copy(pontosDisponiveis=RastreamentoCorreto.pontosDoTrajeto(viagem.detalhes),carregandoPontos=false,isErro=false,status="Segure para fixar")
                val migrados=IdentidadeFavorito.migrar(uiState.favoritos,linha.id,sentido,uiState.pontosDisponiveis)
                prefs.edit().putStringSet("favoritos",migrados).apply()
                uiState=uiState.copy(favoritos=migrados)
                reordenarPontos()
                atualizarListaPontos()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { uiState=uiState.copy(carregandoPontos=false,isErro=true,status=mensagemConsulta(e)) }
        }
    }
    private suspend fun atualizarListaPontos() {
        val viagem=viagemAtual ?: return
        while(currentCoroutineContext().isActive) {
            try {
                require(viagem.data==LocalDate.now(RastreamentoCorreto.fuso))
                val bus=BusRepository.buscarVeiculoCorreto(viagem,veiculoIdFixado)
                if(bus!=null) veiculoIdFixado=bus.vehicleId
                val agora=Instant.now()
                val previsoes=withContext(Dispatchers.Default) { estimativasPontos?.calcular(bus,agora).orEmpty() }
                val idade=bus?.let { RastreamentoCorreto.idadeGps(it.positionTime,agora) }
                val validade=if(previsoes.isNotEmpty() && idade!=null) agora.plusSeconds(121-idade) else null
                val mensagem=when {
                    bus==null -> "Sem ônibus informado para esta saída"
                    !RastreamentoCorreto.gpsValido(bus,agora) -> "GPS antigo · sem estimativas"
                    previsoes.isEmpty() -> "Sem estimativa para os pontos adiante"
                    else -> "Ônibus ${bus.vehicleId} · minutos estimados"
                }
                uiState=uiState.copy(minutosPorPonto=previsoes,previsoesValidasAte=validade,statusPrevisoes=mensagem)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                uiState=uiState.copy(minutosPorPonto=emptyMap(),previsoesValidasAte=null,statusPrevisoes=mensagemConsulta(e))
            }
            delay(20_000)
        }
    }
    fun selecionarPonto(ponto: BusStop) {
        trabalho?.cancel(); observador=ObservadorPassagem()
        uiState=uiState.copy(pontoSelecionado=ponto,veiculoMaisProximo=null,mapsRouteUrl=null,status="Localizando ônibus...",statusAproximacao=StatusAproximacao.ANALISANDO,
            metrosRestantes=null,gpsValidoAte=null,mapaAcompanhamento=viagemAtual?.detalhes?.let { TileMapa(it.shape,it.stops) })
        retomar()
    }
    private fun rastrear() {
        val linha=uiState.linhaSelecionada ?: return; val sentido=uiState.sentidoSelecionado ?: return
        val h=uiState.horarioSelecionado ?: return; val ponto=uiState.pontoSelecionado ?: return
        trabalho=viewModelScope.launch {
            while(isActive) {
                try {
                    val viagem=viagemAtual ?: BusRepository.confirmarViagem(linha.id,sentido,h).also { viagemAtual=it }
                    require(viagem.data==LocalDate.now(RastreamentoCorreto.fuso)) { "O dia mudou. Selecione o horário novamente" }
                    require(RastreamentoCorreto.atendePonto(viagem.detalhes,ponto)) { "Esta viagem não atende o ponto" }
                    if(uiState.mapaAcompanhamento==null) uiState=uiState.copy(mapaAcompanhamento=TileMapa(viagem.detalhes.shape,viagem.detalhes.stops))
                    val bus=BusRepository.buscarVeiculoCorreto(viagem,veiculoIdFixado)
                    val agora=Instant.now()
                    if(bus==null) semPrevisao(if(veiculoIdFixado==null) "Nenhum ônibus informado para esta saída" else "Ônibus $veiculoIdFixado sem sinal; aguardando o mesmo veículo")
                    else {
                        veiculoIdFixado=bus.vehicleId
                        if(!RastreamentoCorreto.gpsValido(bus,agora)) semPrevisao("Ônibus ${bus.vehicleId}: GPS antigo ou inválido")
                        else {
                            val pos=RastreamentoCorreto.posicao(bus,ponto,viagem.detalhes)
                            val situacao=pos?.let { observador.observar(it,bus.positionTime!!,agora) }
                            val aproximacao=when(situacao) {
                                SituacaoPonto.ANTES -> StatusAproximacao.ANTES_PONTO
                                SituacaoPonto.APROXIMANDO -> StatusAproximacao.CHEGANDO
                                SituacaoPonto.APOS -> StatusAproximacao.APOS_PONTO
                                SituacaoPonto.PASSOU -> StatusAproximacao.PASSOU
                                null -> StatusAproximacao.ANALISANDO
                            }
                            val metros=pos?.restante?.let { abs(it) } ?: RastreamentoCorreto.distancia(bus.lat,bus.lng,ponto.lat,ponto.lng)
                            val tempo=if(pos!=null && pos.restante< -80) "após seu ponto" else "~${max(1,ceil(metros/300).toInt())} min · ${if(pos==null) "linha reta" else "estimado"}"
                            uiState=uiState.copy(veiculoMaisProximo=bus,distanciaAtePonto=if(metros>=1000) "%.1f km".format(metros/1000) else "${metros.toInt()} m",tempoEstimado=tempo,status="Ônibus ${bus.vehicleId} · saída ${bus.startTime}",isErro=false,mapsRouteUrl=BusRepository.buildGoogleMapsUrl(bus.lat,bus.lng),statusAproximacao=aproximacao,
                                metrosRestantes=pos?.restante,gpsValidoAte=agora.plusSeconds(121-RastreamentoCorreto.idadeGps(bus.positionTime,agora)!!),
                                mapaAcompanhamento=TileMapa(viagem.detalhes.shape,viagem.detalhes.stops,RastreamentoCorreto.Coordenada(bus.lat,bus.lng)))
                        }
                    }
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) { semPrevisao(mensagemConsulta(e)) }
                delay(20_000)
            }
        }
    }
    private fun semPrevisao(texto: String) { uiState=uiState.copy(veiculoMaisProximo=null,distanciaAtePonto="--",tempoEstimado="--",mapsRouteUrl=null,status=texto,statusAproximacao=StatusAproximacao.ANALISANDO,metrosRestantes=null,gpsValidoAte=null,
        mapaAcompanhamento=uiState.mapaAcompanhamento?.copy(onibus=null)) }
    fun abrirMeuPonto(config: TileConfig, resultado: TileResultado?) {
        val atual=resultado?.atual(Instant.now())
        selecionarLinha(config.linha)
        if(atual?.estado==TileEstado.CHEGADA && atual.tripId!=null && atual.saida!=null) {
            uiState=uiState.copy(sentidoSelecionado=config.sentido,horarioSelecionado=BusSchedule("${atual.tripId}:${atual.saida}",atual.saida,atual.tripId),pontoSelecionado=config.ponto,status="Localizando ônibus...")
            veiculoIdFixado=atual.veiculoId
            retomar()
        } else selecionarSentido(config.sentido)
    }
    fun voltar() {
        trabalho?.cancel()
        uiState=when {
            uiState.pontoSelecionado!=null -> uiState.copy(pontoSelecionado=null,veiculoMaisProximo=null,mapsRouteUrl=null)
            uiState.horarioSelecionado!=null -> { limparVinculo(); uiState.copy(horarioSelecionado=null,pontosDisponiveis=emptyList()) }
            uiState.sentidoSelecionado!=null -> uiState.copy(sentidoSelecionado=null,horariosDisponiveis=emptyList())
            else -> BusUiState(favoritos=uiState.favoritos)
        }
        retomar()
    }
    fun tentarNovamente() { retomar() }
    fun resetar() { trabalho?.cancel(); limparVinculo(); uiState=BusUiState(favoritos=uiState.favoritos) }
}
