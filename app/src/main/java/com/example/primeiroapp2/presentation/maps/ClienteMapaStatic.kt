package com.example.primeiroapp2.presentation.maps

import android.content.Context
import android.graphics.*
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto.Coordenada
import com.example.primeiroapp2.presentation.tile.TileConfig
import com.example.primeiroapp2.presentation.tile.TileMapa
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Apenas a imagem usada na entrega atual do Tile permanece em memória; nunca vai para o cache em disco. */
object ClienteMapaStatic {
    private val mutex=Mutex()
    private val http=OkHttpClient.Builder().callTimeout(12,TimeUnit.SECONDS).cache(null).build()
    private data class Exibicao(val id: String,val viewport: ViewportMapa,val bitmap: Bitmap,val expira: Instant)
    private var exibicao: Exibicao?=null
    data class Imagem(val png: ByteArray,val semGps: ByteArray)
    suspend fun imagem(context: Context,config: TileConfig,mapa: TileMapa?,agora: Instant): Imagem?=withContext(Dispatchers.IO) {
        mutex.withLock {
            if(exibicao?.expira?.let { agora>=it }==true) { exibicao?.bitmap?.recycle();exibicao=null }
            val chave=MapasConfig.chaveStatic()
            if(chave.isBlank() || mapa==null) return@withLock null
            val id="${config.chave}:${mapa.shape}"
            var base=exibicao?.takeIf { it.id==id && agora<it.expira && !it.bitmap.isRecycled }
            if(base==null) {
                if(!MapasConfig.reservarStatic(context,agora)) return@withLock null
                try {
                    val ponto=Coordenada(config.ponto.lat,config.ponto.lng)
                    val caminho=runCatching { RastreamentoCorreto.decodificar(mapa.shape.orEmpty()) }.getOrDefault(emptyList())
                    val viewport=ViewportMapa.enquadrar(caminho+mapa.pontos.map { Coordenada(it.lat,it.lng) }+ponto)
                    val url=HttpUrl.Builder().scheme("https").host("maps.googleapis.com").addPathSegments("maps/api/staticmap")
                        .addQueryParameter("center","${viewport.centro.lat},${viewport.centro.lng}")
                        .addQueryParameter("zoom",viewport.zoom.toString()).addQueryParameter("size","${viewport.largura}x${viewport.altura}")
                        .addQueryParameter("scale","1").addQueryParameter("format","png").addQueryParameter("maptype","roadmap")
                        .addQueryParameter("style","element:geometry|color:0x17212b")
                        .addQueryParameter("style","element:labels.text.fill|color:0xbec9d4")
                        .addQueryParameter("style","element:labels.text.stroke|color:0x17212b")
                        .addQueryParameter("style","feature:poi|visibility:off")
                        .addQueryParameter("style","feature:transit|visibility:off")
                        .addQueryParameter("style","feature:road|element:geometry|color:0x435363")
                        .addQueryParameter("style","feature:water|element:geometry|color:0x0b293b")
                        .addQueryParameter("markers","size:small|color:0xFFC64B|${ponto.lat},${ponto.lng}")
                        .apply { mapa.shape?.takeIf { it.isNotBlank() }?.let { addQueryParameter("path","color:0x8AB4F8FF|weight:2|enc:$it") } }
                        .addQueryParameter("key",chave).build()
                    if(url.toString().length>16000) return@withLock null
                    val bytes=http.newCall(Request.Builder().url(url).header("Cache-Control","no-store").build()).execute().use { response ->
                        if(!response.isSuccessful || response.header("Content-Type").orEmpty().startsWith("image/").not() || response.header("X-Staticmap-API-Warning")!=null) return@withLock null
                        val body=response.body() ?: return@withLock null
                        if(body.contentLength()>2_000_000) return@withLock null
                        body.byteStream().use { stream ->
                            val output=java.io.ByteArrayOutputStream()
                            val buffer=ByteArray(8192)
                            while(true) {
                                val n=stream.read(buffer);if(n<0) break
                                if(output.size()+n>2_000_000) return@withLock null
                                output.write(buffer,0,n)
                            }
                            output.toByteArray()
                        }
                    }
                    val options=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
                    if(options.outWidth!=viewport.largura || options.outHeight!=viewport.altura) return@withLock null
                    val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size) ?: return@withLock null
                    exibicao?.bitmap?.recycle()
                    base=Exibicao(id,viewport,bitmap,agora.plusSeconds(90));exibicao=base
                } catch(e: CancellationException) { throw e }
                catch(_: Exception) { return@withLock null }
            }
            val atual=requireNotNull(base)
            fun png(onibus: Coordenada?): ByteArray {
                val bitmap=atual.bitmap.copy(Bitmap.Config.ARGB_8888,true)
                try {
                    // Atribuição original permanece intocada na faixa inferior da imagem.
                    onibus?.let {
                        val (x,y)=atual.viewport.pixel(it)
                        if(x in 10f..(bitmap.width-10f) && y in 12f..(bitmap.height-32f)) {
                            val canvas=Canvas(bitmap);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
                            paint.color=Color.BLACK;canvas.drawRoundRect(x-9,y-11,x+9,y+11,3f,3f,paint)
                            paint.color=Color.rgb(105,232,140);canvas.drawRoundRect(x-7,y-9,x+7,y+8,2f,2f,paint)
                            canvas.drawCircle(x-4,y+9,1.5f,paint);canvas.drawCircle(x+4,y+9,1.5f,paint)
                            paint.color=Color.BLACK;canvas.drawRect(x-4,y-6,x+4,y-1,paint)
                        }
                    }
                    return java.io.ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it);it.toByteArray() }
                } finally { bitmap.recycle() }
            }
            Imagem(png(mapa.onibus),png(null))
        }
    }
}
