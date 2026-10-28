package com.example.primeiroapp2.presentation.tile

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.tiles.TileBuilders
import com.example.primeiroapp2.presentation.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.io.File

@RunWith(AndroidJUnit4::class)
class TileRecoveryTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun config()=TileConfig(BusLine("942455","01","Pacaembu x UFSCar Norte"),1,BusStop(2,"Ponto de teste",38.5,-120.2,99))
    private fun result(now: Instant)=TileResultado(TileEstado.CHEGADA,now.epochSecond,"Estimativa pelo percurso",120.0,600.0,101,"2035","16:15",now.epochSecond,mapa=TileMapa("_p~iF~ps|U_ulLnnqC_mqNvxq`@",listOf(ApiStop(99,"Ponto",38.5,-120.2)),RastreamentoCorreto.Coordenada(40.7,-120.95)))
    @Test fun preferenciasPreservamConfiguracaoERejeitamResultadoDeOutroPonto() {
        val store=MeuPontoStore(context); val original=store.config(); val oldResult=store.resultado()
        try {
            val c=config(); store.salvar(c); store.registrar(c,result(Instant.now()))
            assertEquals(c,store.config()); assertEquals("2035",store.resultado()?.veiculoId)
            val other=c.copy(ponto=c.ponto.copy(stopId=100)); store.salvar(other)
            store.registrar(c,result(Instant.now())); assertNull(store.resultado())
        } finally { if(original!=null) { store.salvar(original); if(oldResult!=null) store.registrar(original,oldResult) } else context.getSharedPreferences("meu_ponto_tile",0).edit().clear().commit() }
    }
    @Test fun mapaRenderizaOnibusSomenteEnquantoGpsRecente() {
        val now=Instant.now(); val c=config(); val r=result(now)
        val fresh=MeuPontoMapaRenderer.bitmap(c,r,336,112,agora=now)
        val stale=MeuPontoMapaRenderer.bitmap(c,r,336,112,agora=now.plusSeconds(121))
        fun green(bitmap: android.graphics.Bitmap): Int { var total=0; for(x in 0 until bitmap.width) for(y in 0 until bitmap.height) if(bitmap.getPixel(x,y)==Color.rgb(105,232,140)) total++; return total }
        assertTrue(green(fresh)>0); assertEquals(0,green(stale))
        assertFalse(fresh.sameAs(stale))
        File(context.getExternalFilesDir(null),"mapa-recuperado.png").outputStream().use { fresh.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        fresh.recycle(); stale.recycle()
    }
    @Test fun tileGeraTimelineComExpiracaoEImagensPng() {
        val store=MeuPontoStore(context); val original=store.config(); val oldResult=store.resultado()
        try {
            val now=Instant.now(); store.salvar(config()); store.registrar(config(),result(now))
            val service=MeuPontoTileService()
            ContextWrapper::class.java.getDeclaredMethod("attachBaseContext",Context::class.java).apply { isAccessible=true }.invoke(service,context)
            val tile=MeuPontoTileService::class.java.getDeclaredMethod("renderizar").apply { isAccessible=true }.invoke(service) as TileBuilders.Tile
            assertEquals(2,tile.tileTimeline!!.timelineEntries.size)
            assertTrue(tile.tileTimeline!!.timelineEntries.any { it.validity!=null })
            for(id in listOf("mapa","mapa-sem-gps")) {
                val file=File(context.cacheDir,"mapas-tile/${tile.resourcesVersion}-$id.png")
                assertTrue(file.exists()); assertTrue(file.length()>100)
            }
        } finally { if(original!=null) { store.salvar(original); if(oldResult!=null) store.registrar(original,oldResult) } else context.getSharedPreferences("meu_ponto_tile",0).edit().clear().commit() }
    }
}
