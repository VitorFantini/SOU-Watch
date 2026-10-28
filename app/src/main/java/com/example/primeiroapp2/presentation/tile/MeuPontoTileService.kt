package com.example.primeiroapp2.presentation.tile

import androidx.wear.tiles.TileService
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.EventBuilders
import androidx.wear.protolayout.*
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import com.example.primeiroapp2.presentation.MainActivity
import kotlinx.coroutines.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto
import java.io.File
import java.security.MessageDigest
import com.google.gson.Gson

class MeuPontoTileService : TileService() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val principal=0xFFE8E8E8.toInt(); private val marca=0xFFFFC928.toInt(); private val cinza=0xFFADBACB.toInt()
    private fun <T> pronto(value: T): ListenableFuture<T> = CallbackToFutureAdapter.getFuture { it.set(value); "SouWatch" }
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> = CallbackToFutureAdapter.getFuture { future ->
        val task=scope.launch {
            try {
                if(requestParams.currentState.lastClickableId=="atualizar") MeuPontoAtualizador.atualizar(this@MeuPontoTileService)
                MeuPontoAtualizador.aguardar()
                future.set(renderizar())
            } catch(e: Exception) { future.setException(e) }
        }
        future.addCancellationListener({task.cancel()},java.util.concurrent.Executor { it.run() })
        "SouWatch Tile"
    }
    private fun renderizar(): TileBuilders.Tile {
        val store=MeuPontoStore(this); val config=store.config(); val agora=Instant.now(); val snapshot=store.resultado()?.atual(agora)
        val version=prepararRecursos(config,snapshot,agora)
        val timeline=TimelineBuilders.Timeline.Builder()
        if(config!=null && snapshot!=null && snapshot.estado!=TileEstado.ANTIGO) {
            val expirado=snapshot.copy(estado=TileEstado.ANTIGO,mensagem="Toque em Atualizar",mapa=snapshot.mapa?.copy(onibus=null))
            timeline.addTimelineEntry(TimelineBuilders.TimelineEntry.Builder().setLayout(layout(config,expirado,agora)).build())
            timeline.addTimelineEntry(TimelineBuilders.TimelineEntry.Builder().setValidity(TimelineBuilders.TimeInterval.Builder().setStartMillis(agora.toEpochMilli()).setEndMillis(snapshot.validoAte()*1000).build()).setLayout(layout(config,snapshot,agora)).build())
        } else timeline.addTimelineEntry(TimelineBuilders.TimelineEntry.Builder().setLayout(layout(config,snapshot,agora)).build())
        return TileBuilders.Tile.Builder().setResourcesVersion(version).setTileTimeline(timeline.build()).build()
    }
    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val version=requestParams.version
        val builder=ResourceBuilders.Resources.Builder().setVersion(version)
        if(version.matches(Regex("mapa-[a-f0-9]{64}"))) {
            for(id in listOf("mapa","mapa-sem-gps")) {
                val file=File(cacheDir,"mapas-tile/$version-$id.png")
                if(file.exists()) builder.addIdToImageMapping(id,ResourceBuilders.ImageResource.Builder().setInlineResource(ResourceBuilders.InlineImageResource.Builder().setData(file.readBytes()).setWidthPx(336).setHeightPx(112).build()).build())
            }
        }
        return pronto(builder.build())
    }
    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) { MeuPontoAtualizador.atualizar(this); MeuPontoAtualizador.solicitar(this) }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    private fun prepararRecursos(config: TileConfig?,result: TileResultado?,agora: Instant): String {
        if(config==null) return "mapa-vazio"
        val hash=MessageDigest.getInstance("SHA-256").digest(Gson().toJson(listOf("yellow-2",config,result)).toByteArray()).joinToString("") { "%02x".format(it) }
        val version="mapa-$hash"; val folder=File(cacheDir,"mapas-tile").apply { mkdirs() }
        for((id,r) in listOf("mapa" to result,"mapa-sem-gps" to result?.copy(estado=TileEstado.ANTIGO,mapa=result.mapa?.copy(onibus=null)))) {
            val file=File(folder,"$version-$id.png")
            if(!file.exists()) file.writeBytes(MeuPontoMapaRenderer.png(config,r,agora))
        }
        folder.listFiles()?.filter { it.name.startsWith("mapa-") && it.extension=="png" && !it.name.startsWith(version) && System.currentTimeMillis()-it.lastModified()>86400000 }?.forEach { it.delete() }
        return version
    }
    private fun launch(clazz: Class<*>,abrir: Boolean=false): ActionBuilders.LaunchAction {
        val activity=ActionBuilders.AndroidActivity.Builder().setPackageName(packageName).setClassName(clazz.name)
        if(abrir) activity.addKeyToExtraMapping("abrir_meu_ponto",ActionBuilders.AndroidBooleanExtra.Builder().setValue(true).build())
        return ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build()
    }
    private fun text(value: String,size: Float,color: Int=cinza,lines: Int=1)=LayoutElementBuilders.Text.Builder().setText(value).setMaxLines(lines).setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER).setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(DimensionBuilders.sp(size)).setColor(ColorBuilders.argb(color)).build()).build()
    private fun spacer(height: Float)=LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(height)).build()
    private fun button(label: String,id: String,action: ActionBuilders.Action,width: Float=82f)=LayoutElementBuilders.Box.Builder().setWidth(DimensionBuilders.dp(width)).setHeight(DimensionBuilders.dp(32f)).setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER).setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER).setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(ModifiersBuilders.Clickable.Builder().setId(id).setOnClick(action).build()).setBackground(ModifiersBuilders.Background.Builder().setColor(ColorBuilders.argb(0xFF171717.toInt())).setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(15f)).build()).build()).build()).addContent(text(label,11f,principal)).build()
    private fun layout(config: TileConfig?,result: TileResultado?,agora: Instant): LayoutElementBuilders.Layout {
        val column=LayoutElementBuilders.Column.Builder().setWidth(DimensionBuilders.dp(184f)).setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        if(config==null) {
            column.addContent(text("SOU WATCH · MEU PONTO",11f,marca)).addContent(spacer(12f)).addContent(text("Escolha seu ponto",21f,-1,2)).addContent(spacer(8f)).addContent(text("Salve linha, sentido e parada",11f,cinza,2)).addContent(spacer(12f)).addContent(button("Configurar","configurar",launch(MeuPontoSetupActivity::class.java),135f))
        } else {
            val chegada=result?.estado==TileEstado.CHEGADA
            column.addContent(text("LINHA ${config.linha.numero} · ${if(config.sentido==0) "IDA" else "VOLTA"}",11f,principal)).addContent(text("P${config.ponto.sequencia} · ${config.ponto.nome}",10f,cinza,2)).addContent(spacer(2f))
                .addContent(text(result?.titulo() ?: "Atualizando…",if(chegada) 24f else 19f,if(chegada) 0xFF69E88C.toInt() else if(result?.estado==TileEstado.PROGRAMADO) principal else cinza))
            val detalhe=when(result?.estado) { TileEstado.CHEGADA -> "${result.metros?.let { if(it>=1000) "%.1f km".format(it/1000) else "${it.toInt()} m" } ?: ""} · ônibus ${result.veiculoId}"; TileEstado.PROGRAMADO -> "Saída do terminal · chegada sem GPS"; else -> result?.mensagem ?: "Consultando a operadora" }
            column.addContent(text(detalhe,10f,cinza,2)).addContent(spacer(3f))
            val formato=DateTimeFormatter.ofPattern("HH:mm:ss").withZone(RastreamentoCorreto.fuso)
            val horario=if(MeuPontoAtualizador.carregando) "Atualizando…" else if(chegada && result?.gpsEm!=null) "GPS às ${formato.format(Instant.ofEpochSecond(result.gpsEm))} · estimativa" else result?.let { "Consulta às ${formato.format(Instant.ofEpochSecond(it.consultadoEm))}" } ?: "Aguardando atualização"
            column.addContent(text(horario,9f)).addContent(spacer(3f))
            column.addContent(LayoutElementBuilders.Image.Builder().setResourceId(if(result?.estado==TileEstado.ANTIGO) "mapa-sem-gps" else "mapa").setWidth(DimensionBuilders.dp(168f)).setHeight(DimensionBuilders.dp(40f)).setModifiers(ModifiersBuilders.Modifiers.Builder().setSemantics(ModifiersBuilders.Semantics.Builder().setContentDescription("Abrir mapa: cinza é trajeto, amarelo é seu ponto e verde é ônibus com GPS recente").build()).setClickable(ModifiersBuilders.Clickable.Builder().setId("mapa").setOnClick(launch(MeuPontoMapaActivity::class.java)).build()).build()).build())
                .addContent(text("Traçado · toque para ampliar",8f)).addContent(spacer(3f))
            column.addContent(LayoutElementBuilders.Row.Builder().setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER).addContent(button("Atualizar","atualizar",ActionBuilders.LoadAction.Builder().build())).addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(6f)).build()).addContent(button("Abrir app","abrir",launch(MainActivity::class.java,true))).build())
                .addContent(spacer(4f)).addContent(LayoutElementBuilders.Box.Builder().setWidth(DimensionBuilders.dp(160f)).setHeight(DimensionBuilders.dp(32f)).setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER).setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER).setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(ModifiersBuilders.Clickable.Builder().setId("trocar").setOnClick(launch(MeuPontoSetupActivity::class.java)).build()).build()).addContent(text("Trocar ponto",10f,principal)).build())
        }
        return LayoutElementBuilders.Layout.Builder().setRoot(LayoutElementBuilders.Box.Builder().setWidth(DimensionBuilders.expand()).setHeight(DimensionBuilders.expand()).setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER).setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER).addContent(column.build()).build()).build()
    }
}
