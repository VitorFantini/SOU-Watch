package com.example.primeiroapp2.presentation.tile

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.renderer.TileRenderer
import androidx.wear.protolayout.ResourceBuilders
import com.example.primeiroapp2.presentation.MainActivity
import com.example.primeiroapp2.presentation.data.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TileVisualTest {
    @Test fun tileRenderizaIdadeDinamicaSemCortarCabecalho() {
        val inst=InstrumentationRegistry.getInstrumentation();val context=inst.targetContext
        val prefs=context.getSharedPreferences("meu_ponto_tile",0);val antes=prefs.all.toMap()
        val activity=inst.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            inst.runOnMainSync { activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            inst.uiAutomation.executeShellCommand("input keyevent 224").close()
            val d=inst.context.assets.open("trajeto-preview.json").bufferedReader().use { Gson().fromJson(it,TripDetails::class.java) }
            val ponto=RastreamentoCorreto.pontosDoTrajeto(d)[10]
            val config=TileConfig(LineRepository.supportedLines.first(),0,ponto)
            val now=Instant.now();val result=TileResultado(TileEstado.CHEGADA,now.epochSecond,"Estimativa",60.0,300.0,101,"TESTE","12:00",now.epochSecond-45,
                mapa=TileMapa(d.shape,d.stops,RastreamentoCorreto.Coordenada(ponto.lat-.001,ponto.lng)))
            val store=MeuPontoStore(context);store.salvar(config);store.registrar(config,result)
            val service=MeuPontoTileService()
            ContextWrapper::class.java.getDeclaredMethod("attachBaseContext",Context::class.java).apply { isAccessible=true }.invoke(service,context)
            val tile=MeuPontoTileService::class.java.getDeclaredMethod("renderizar").apply { isAccessible=true }.invoke(service) as TileBuilders.Tile
            val layout=tile.tileTimeline!!.timelineEntries.first { it.validity!=null }.layout!!
            val resources=ResourceBuilders.Resources.Builder().setVersion(tile.resourcesVersion)
            for(id in listOf("mapa","mapa-sem-gps")) {
                val bytes=File(context.cacheDir,"mapas-tile/${tile.resourcesVersion}-$id.png").readBytes()
                resources.addIdToImageMapping(id,ResourceBuilders.ImageResource.Builder().setInlineResource(ResourceBuilders.InlineImageResource.Builder().setData(bytes).setWidthPx(336).setHeightPx(128).build()).build())
            }
            lateinit var rendered:ListenableFuture<*>
            lateinit var root:FrameLayout
            inst.runOnMainSync {
                root=FrameLayout(activity);activity.setContentView(root)
                val renderer=TileRenderer(inst.context,activity.mainExecutor) { }
                rendered=renderer.inflateAsync(layout,resources.build(),root)
            }
            rendered.get(20,TimeUnit.SECONDS);SystemClock.sleep(1200)
            fun text():String {
                var value=""
                inst.runOnMainSync {
                    fun collect(view:android.view.View):String = when(view) {
                        is android.widget.TextView -> view.text.toString()
                        is android.view.ViewGroup -> (0 until view.childCount).joinToString(" ") { collect(view.getChildAt(it)) }
                        else -> ""
                    }
                    value=collect(root)
                }
                return value
            }
            val first=text()
            val folder=File(context.getExternalFilesDir(null),"preview-1.5.1").apply { mkdirs() }
            val bitmap=inst.uiAutomation.takeScreenshot()
            File(folder,"tile-idade-gps.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            assertTrue("Texto dinâmico não foi renderizado: $first, filhos=${root.childCount}, tamanho=${root.width}x${root.height}",first.contains("Posição há"))
            SystemClock.sleep(2200)
            val second=text()
            val regex=Regex("Posição há (\\d+) s")
            assertTrue(regex.find(second)!!.groupValues[1].toInt()>regex.find(first)!!.groupValues[1].toInt())
            assertTrue(second.contains("LINHA 01"))
        } finally {
            val editor=prefs.edit().clear()
            for((key,value) in antes) when(value) { is String -> editor.putString(key,value);is Long -> editor.putLong(key,value) }
            editor.commit();inst.runOnMainSync { activity.finish() }
        }
    }
}
