package com.example.primeiroapp2.presentation.tile

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.tiles.TileBuilders
import com.example.primeiroapp2.presentation.data.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class EstresseTileTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun detalhes()=InstrumentationRegistry.getInstrumentation().context.assets.open("trajeto-preview.json").bufferedReader().use { Gson().fromJson(it,TripDetails::class.java) }
    private fun preservar(block: () -> Unit) {
        val prefs=context.getSharedPreferences("meu_ponto_tile",0);val antes=prefs.all.toMap()
        try { block() } finally {
            val editor=prefs.edit().clear()
            for((key,value) in antes) when(value) { is String -> editor.putString(key,value);is Long -> editor.putLong(key,value);is Int -> editor.putInt(key,value);is Boolean -> editor.putBoolean(key,value);is Float -> editor.putFloat(key,value) }
            editor.commit()
        }
    }
    @Test fun duzentasRenderizacoesDeTileSemCrashComGpsExpiradoEFalhas()=preservar {
        val detalhes=detalhes();val pontos=RastreamentoCorreto.pontosDoTrajeto(detalhes)
        val caminho=RastreamentoCorreto.decodificar(detalhes.shape!!)
        val store=MeuPontoStore(context)
        val service=MeuPontoTileService()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext",Context::class.java).apply { isAccessible=true }.invoke(service,context)
        val render=MeuPontoTileService::class.java.getDeclaredMethod("renderizar").apply { isAccessible=true }
        val tempos=mutableListOf<Long>();val memoria=mutableListOf<Int>()
        val cpu=Process.getElapsedCpuTime()
        repeat(200) { i ->
            val ponto=pontos[i%pontos.size];val config=TileConfig(LineRepository.supportedLines[i%3],i%2,ponto)
            val agora=Instant.now();val estado=TileEstado.values()[i%TileEstado.values().size]
            val resultado=TileResultado(estado,agora.epochSecond,"Teste sem rede",60.0,300.0,101,"TESTE","12:00",agora.epochSecond,
                mapa=TileMapa(detalhes.shape,detalhes.stops,caminho[(i*13)%caminho.size]))
            store.salvar(config);store.registrar(config,resultado)
            val inicio=SystemClock.elapsedRealtimeNanos()
            val tile=render.invoke(service) as TileBuilders.Tile
            tempos.add((SystemClock.elapsedRealtimeNanos()-inicio)/1000)
            assertTrue(tile.tileTimeline!!.timelineEntries.isNotEmpty())
            for(id in listOf("mapa","mapa-sem-gps")) {
                val file=File(context.cacheDir,"mapas-tile/${tile.resourcesVersion}-$id.png")
                assertTrue(file.exists())
            }
            if(i%20==0) { val m=Debug.MemoryInfo();Debug.getMemoryInfo(m);memoria.add(m.totalPss) }
        }
        tempos.sort()
        val folder=File(context.getExternalFilesDir(null),"estresse").apply { mkdirs() }
        File(folder,"tile.json").writeText(Gson().toJson(mapOf("renders" to 200,"medianUs" to tempos[100],"p95Us" to tempos[189],"maxUs" to tempos.last(),"cpuMs" to Process.getElapsedCpuTime()-cpu,"pssKbSamples" to memoria,"dataSource" to "trajeto registrado; posicoes simuladas; sem rede")))
    }
    @Test fun cemBitmapsRemovemOnibusExatamenteNaExpiracao() {
        val d=detalhes();val pontos=RastreamentoCorreto.pontosDoTrajeto(d);val caminho=RastreamentoCorreto.decodificar(d.shape!!)
        val agora=Instant.now()
        repeat(50) { i ->
            val ponto=pontos[i%pontos.size];val c=TileConfig(LineRepository.supportedLines.first(),0,ponto)
            val r=TileResultado(TileEstado.CHEGADA,agora.epochSecond,"teste",60.0,300.0,101,"TESTE","12:00",agora.epochSecond,
                mapa=TileMapa(d.shape,d.stops,caminho[(i*17)%caminho.size]))
            for(expirou in listOf(false,true)) {
                val bitmap=MeuPontoMapaRenderer.bitmap(c,r,336,128,aproximar=true,agora=if(expirou) Instant.ofEpochSecond(r.validoAte()) else agora)
                try {
                    var verde=0
                    for(y in 0 until bitmap.height) for(x in 0 until bitmap.width) if(bitmap.getPixel(x,y)==Color.rgb(105,232,140)) verde++
                    if(expirou) assertEquals(0,verde) else assertTrue("Onibus ausente na amostra $i",verde>0)
                } finally { bitmap.recycle() }
            }
        }
    }
    @Test fun preferenciasComJsonTruncadoNaoDerrubamRecuperacao()=preservar {
        val prefs=context.getSharedPreferences("meu_ponto_tile",0)
        for(json in listOf("{","[","null","texto invalido")) {
            prefs.edit().putString("config",json).putString("resultado",json).commit()
            assertNull(MeuPontoStore(context).config());assertNull(MeuPontoStore(context).resultado())
        }
    }
    @Test fun configuracaoParcialNaoDeveSerAceitaComoPontoValido()=preservar {
        context.getSharedPreferences("meu_ponto_tile",0).edit().putString("config","{}").commit()
        assertNull("Configuracao incompleta deve ser descartada antes de acessar linha ou ponto",MeuPontoStore(context).config())
    }
    @Test fun configuracoesSemCamposObrigatoriosOuCoordenadasInvalidasSaoRejeitadas()=preservar {
        val d=detalhes();val ponto=RastreamentoCorreto.pontosDoTrajeto(d).first()
        val valida=TileConfig(LineRepository.supportedLines.first(),0,ponto)
        val prefs=context.getSharedPreferences("meu_ponto_tile",0)
        val gson=Gson()
        for(campo in listOf("linha","sentido","ponto")) {
            val obj=gson.toJsonTree(valida).asJsonObject;obj.remove(campo)
            prefs.edit().putString("config",obj.toString()).commit()
            assertNull("Campo ausente aceito: $campo",MeuPontoStore(context).config())
        }
        for(campo in listOf("nome","sequencia","stopId","lat","lng")) {
            val obj=gson.toJsonTree(valida).asJsonObject;obj.getAsJsonObject("ponto").remove(campo)
            prefs.edit().putString("config",obj.toString()).commit()
            assertNull("Campo de ponto ausente aceito: $campo",MeuPontoStore(context).config())
        }
        val invalida=valida.copy(ponto=ponto.copy(lat=91.0))
        prefs.edit().putString("config",gson.toJson(invalida)).commit()
        assertNull(MeuPontoStore(context).config())
        MeuPontoStore(context).salvar(valida)
        assertEquals(valida,MeuPontoStore(context).config())
    }
    @Test fun resultadoParcialOuEstadoDesconhecidoNaoDerrubamTile()=preservar {
        val d=detalhes();val c=TileConfig(LineRepository.supportedLines.first(),0,RastreamentoCorreto.pontosDoTrajeto(d).first())
        val store=MeuPontoStore(context);store.salvar(c)
        for(json in listOf("{}","{\"estado\":\"DESCONHECIDO\",\"consultadoEm\":1,\"mensagem\":\"teste\"}","{\"estado\":\"ERRO\",\"mensagem\":\"teste\"}")) {
            context.getSharedPreferences("meu_ponto_tile",0).edit().putString("resultado",json).commit()
            assertNull(store.resultado())
        }
    }
    @Test fun tileComConfiguracaoParcialOfereceConfigurarEmVezDeQuebrar()=preservar {
        context.getSharedPreferences("meu_ponto_tile",0).edit().putString("config","{}").putString("resultado","{}").commit()
        val service=MeuPontoTileService()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext",Context::class.java).apply { isAccessible=true }.invoke(service,context)
        val tile=MeuPontoTileService::class.java.getDeclaredMethod("renderizar").apply { isAccessible=true }.invoke(service) as TileBuilders.Tile
        assertEquals("mapa-vazio",tile.resourcesVersion)
        assertEquals(1,tile.tileTimeline!!.timelineEntries.size)
        val layout=tile.tileTimeline!!.timelineEntries.first().layout!!.toByteArray().toString(Charsets.UTF_8)
        assertTrue(layout.contains("Escolha seu ponto"))
        assertTrue(layout.contains("Configurar"))
    }
}
