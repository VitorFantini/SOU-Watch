package com.example.primeiroapp2.presentation.tile

import android.content.Context
import com.example.primeiroapp2.presentation.data.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.time.Instant
import kotlin.math.*

data class TileConfig(val linha: BusLine,val sentido: Int,val ponto: BusStop) { val chave get()="${linha.id}:$sentido:${ponto.stopId}" }
enum class TileEstado { CHEGADA, PROGRAMADO, SEM_GPS, INCERTO, ERRO, ANTIGO }
data class TileMapa(val shape: String?,val pontos: List<ApiStop>,val onibus: RastreamentoCorreto.Coordenada?=null)
data class TileResultado(
    val estado: TileEstado,val consultadoEm: Long,val mensagem: String,
    val segundos: Double?=null,val metros: Double?=null,val tripId: Long?=null,
    val veiculoId: String?=null,val saida: String?=null,val gpsEm: Long?=null,
    val proximaSaida: String?=null,val proximaSaidaEm: Long?=null,val mapa: TileMapa?=null
) {
    fun validoAte(): Long = when(estado) {
        TileEstado.CHEGADA -> (gpsEm ?: consultadoEm)+120
        TileEstado.PROGRAMADO -> proximaSaidaEm?.plus(60) ?: (consultadoEm+120)
        else -> consultadoEm+120
    }
    fun atual(agora: Instant): TileResultado = if(agora.epochSecond<validoAte()) this else copy(estado=TileEstado.ANTIGO,mensagem="Abra ou toque em Atualizar",segundos=null,metros=null,gpsEm=null,veiculoId=null,mapa=mapa?.copy(onibus=null))
    fun titulo() = when(estado) {
        TileEstado.CHEGADA -> "~${max(1,ceil((segundos ?: 0.0)/60).toInt())} min"
        TileEstado.PROGRAMADO -> "Saída ${proximaSaida.orEmpty()}"
        TileEstado.SEM_GPS -> "Sem GPS"
        TileEstado.INCERTO -> "Posição incerta"
        TileEstado.ERRO -> "Sem atualização"
        TileEstado.ANTIGO -> "Atualize"
    }
}
class MeuPontoStore(context: Context) {
    private val prefs=context.applicationContext.getSharedPreferences("meu_ponto_tile",Context.MODE_PRIVATE)
    private val gson=Gson()
    fun config(): TileConfig?=runCatching { gson.fromJson(prefs.getString("config",null),TileConfig::class.java) }.getOrNull()
    fun resultado(): TileResultado?=runCatching { gson.fromJson(prefs.getString("resultado",null),TileResultado::class.java) }.getOrNull()
    fun salvar(config: TileConfig) { prefs.edit().putString("config",gson.toJson(config)).remove("resultado").remove("tentativa").apply() }
    fun registrar(config: TileConfig,resultado: TileResultado) { if(config().let { it?.chave }==config.chave) prefs.edit().putString("resultado",gson.toJson(resultado)).apply() }
    fun tentativa()=prefs.getLong("tentativa",0L)
    fun tentativa(agora: Instant) { prefs.edit().putLong("tentativa",agora.epochSecond).apply() }
}
class MeuPontoEngine {
    suspend fun buscar(config: TileConfig,agora: Instant=Instant.now()): TileResultado {
        require(config.ponto.stopId!=null) { "Configure novamente o ponto" }
        val tabela=BusRepository.tabela(config.linha.id)
        val hoje=agora.atZone(RastreamentoCorreto.fuso).toLocalDate()
        val horarios=RastreamentoCorreto.horariosDoDia(tabela,config.sentido,hoje)
        val minuto=agora.atZone(RastreamentoCorreto.fuso).let { it.hour*60+it.minute }
        val candidatos=mutableListOf<TileResultado>(); val mapas=mutableMapOf<Long,TileMapa>(); val compativeis=mutableSetOf<Long>()
        var falhas=0; var antigos=0; var incertos=0; var passaram=0
        for((trip,partidas) in horarios.groupBy { it.apiTripId }) {
            try {
                val id=requireNotNull(trip)
                val detalhes=BusRepository.detalhes(id)
                if(!RastreamentoCorreto.atendePonto(detalhes,config.ponto)) continue
                compativeis.add(id); mapas[id]=TileMapa(detalhes.shape,detalhes.stops)
                val vehicles=BusRepository.apiMobilibus.buscarVeiculoPorTripId(tripId=id.toString())
                for(h in partidas.filter { (RastreamentoCorreto.minutos(it.horaSaida) ?: Int.MAX_VALUE)<=minuto }) {
                    val bus=try { RastreamentoCorreto.selecionarVeiculo(vehicles,h.horaSaida,null) } catch(e: IllegalArgumentException) { incertos++; null } ?: continue
                    val idade=RastreamentoCorreto.idadeGps(bus.positionTime,agora)
                    if(idade==null || idade>120) { antigos++; continue }
                    val pos=RastreamentoCorreto.posicao(bus,config.ponto,detalhes)
                    if(pos==null) { incertos++; continue }
                    if(pos.restante< -80) { passaram++; continue }
                    val metros=max(0.0,pos.restante)
                    candidatos.add(TileResultado(TileEstado.CHEGADA,agora.epochSecond,"Estimativa pelo percurso",metros/300*60,metros,id,bus.vehicleId,bus.startTime,agora.epochSecond-idade,mapa=mapas[id]?.copy(onibus=RastreamentoCorreto.Coordenada(bus.lat,bus.lng))))
                }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { falhas++ }
        }
        candidatos.minByOrNull { it.segundos ?: Double.MAX_VALUE }?.let { return if(falhas>0) it.copy(mensagem="Estimativa · consulta parcial") else it }
        val motivo=when { falhas>0 -> "Parte da consulta indisponível"; incertos>0 -> "GPS sem previsão pelo percurso"; antigos>0 -> "Aguardando GPS recente"; passaram>0 -> "Veículo informado após o ponto"; else -> "Sem veículo informado para o ponto" }
        val futura=horarios.firstOrNull { it.apiTripId in compativeis && (RastreamentoCorreto.minutos(it.horaSaida) ?: -1)>minuto }
        if(futura!=null) return TileResultado(TileEstado.PROGRAMADO,agora.epochSecond,"Do terminal · $motivo",tripId=futura.apiTripId,proximaSaida=futura.horaSaida.substringBefore(" "),proximaSaidaEm=hoje.atStartOfDay(RastreamentoCorreto.fuso).toEpochSecond()+RastreamentoCorreto.minutos(futura.horaSaida)!!*60,mapa=mapas[futura.apiTripId])
        return TileResultado(if(falhas>0) TileEstado.ERRO else if(incertos>0) TileEstado.INCERTO else TileEstado.SEM_GPS,agora.epochSecond,motivo,mapa=mapas.values.firstOrNull())
    }
}
object MeuPontoAtualizador {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var job: Job?=null
    private var chave: String?=null
    @Volatile var carregando=false
        private set
    @Synchronized fun atualizar(context: Context): Job? {
        val app=context.applicationContext; val store=MeuPontoStore(app); val config=store.config() ?: return null
        if(job?.isActive==true && chave==config.chave) return job
        if(chave!=config.chave) job?.cancel()
        val agora=Instant.now()
        if(agora.epochSecond-store.tentativa() in 0..19) return job
        chave=config.chave; store.tentativa(agora); carregando=true
        return scope.launch {
            try {
                val result=try { withTimeout(30_000) { MeuPontoEngine().buscar(config) } }
                catch(e: TimeoutCancellationException) { TileResultado(TileEstado.ERRO,Instant.now().epochSecond,"Consulta demorou. Toque em Atualizar",mapa=store.resultado()?.mapa?.copy(onibus=null)) }
                catch(e: CancellationException) { throw e }
                catch(e: Exception) { TileResultado(TileEstado.ERRO,Instant.now().epochSecond,mensagemConsulta(e),mapa=store.resultado()?.mapa?.copy(onibus=null)) }
                store.registrar(config,result)
            } finally { if(chave==config.chave) carregando=false; solicitar(app) }
        }.also { job=it }
    }
    suspend fun aguardar() { val atual=job; withTimeoutOrNull(6_000) { atual?.join() } }
    fun solicitar(context: Context) { androidx.wear.tiles.TileService.getUpdater(context).requestUpdate(MeuPontoTileService::class.java) }
}
