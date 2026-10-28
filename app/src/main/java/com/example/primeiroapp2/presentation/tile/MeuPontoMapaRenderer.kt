package com.example.primeiroapp2.presentation.tile

import android.graphics.*
import java.time.Instant
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto.Coordenada
import java.io.ByteArrayOutputStream
import kotlin.math.*

object MeuPontoMapaRenderer {
    fun bitmap(config: TileConfig,result: TileResultado?,width: Int,height: Int,aproximar: Boolean=false,agora: Instant=Instant.now()): Bitmap =
        Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).also { desenhar(Canvas(it),config,result,width.toFloat(),height.toFloat(),aproximar,agora) }
    fun png(config: TileConfig,result: TileResultado?,agora: Instant): ByteArray {
        val bitmap=bitmap(config,result,336,128,agora=agora)
        return try { ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it); it.toByteArray() } } finally { bitmap.recycle() }
    }
    fun desenhar(canvas: Canvas,config: TileConfig,result: TileResultado?,width: Float,height: Float,aproximar: Boolean=false,agora: Instant=Instant.now()) {
        val atual=result?.atual(agora)
        val mapa=atual?.mapa?.copy(onibus=atual?.takeIf { it.estado==TileEstado.CHEGADA }?.mapa?.onibus)
        desenharMapa(canvas,config,mapa,width,height,aproximar)
    }
    fun desenharMapa(canvas: Canvas,config: TileConfig,mapa: TileMapa?,width: Float,height: Float,aproximar: Boolean=false) {
        val caminho=runCatching { RastreamentoCorreto.decodificar(mapa?.shape.orEmpty()) }.getOrDefault(emptyList())
        val pontos=mapa?.pontos.orEmpty().map { Coordenada(it.lat,it.lng) }
        val selecionado=Coordenada(config.ponto.lat,config.ponto.lng)
        val bus=mapa?.onibus
        val escala=cos(Math.toRadians(selecionado.lat))
        val enquadrar=if(aproximar) listOfNotNull(selecionado,bus) else caminho+pontos+listOfNotNull(selecionado,bus)
        val minX=enquadrar.minOf { it.lng*escala }; val maxX=enquadrar.maxOf { it.lng*escala }
        val minY=enquadrar.minOf { it.lat }; val maxY=enquadrar.maxOf { it.lat }
        val centroX=(minX+maxX)/2; val centroY=(minY+maxY)/2
        val s=(width/336).coerceIn(.8f,2.5f); val pad=16*s
        val zoom=min((width-2*pad)/max(maxX-minX,160.0/111320),(height-2*pad)/max(maxY-minY,160.0/111320))
        fun pixel(p: Coordenada)=PointF((width/2+(p.lng*escala-centroX)*zoom).toFloat(),(height/2-(p.lat-centroY)*zoom).toFloat())
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.save(); canvas.clipRect(0f,0f,width,height)
        paint.color=Color.BLACK; canvas.drawRect(0f,0f,width,height,paint)
        val path=Path(); caminho.forEachIndexed { i,p -> val xy=pixel(p); if(i==0) path.moveTo(xy.x,xy.y) else path.lineTo(xy.x,xy.y) }
        paint.color=Color.rgb(150,150,150); paint.style=Paint.Style.STROKE; paint.strokeWidth=2.5f*s; paint.strokeJoin=Paint.Join.ROUND
        canvas.drawPath(path,paint); paint.style=Paint.Style.FILL; paint.color=Color.rgb(163,193,207)
        pontos.forEach { val p=pixel(it); canvas.drawCircle(p.x,p.y,2*s,paint) }
        val p=pixel(selecionado)
        paint.color=Color.rgb(255,198,75)
        val pin=Path().apply { moveTo(p.x,p.y); lineTo(p.x-6*s,p.y-10*s); lineTo(p.x+6*s,p.y-10*s); close() }
        canvas.drawPath(pin,paint); canvas.drawCircle(p.x,p.y-10*s,6*s,paint)
        paint.color=Color.BLACK; canvas.drawCircle(p.x,p.y-10*s,2*s,paint)
        bus?.let {
            val b=pixel(it)
            val x=if(hypot(b.x-p.x,b.y-p.y)<22*s) min(b.x+22*s,width-12*s) else b.x
            val y=b.y
            paint.color=Color.rgb(105,232,140); paint.strokeWidth=2*s; canvas.drawLine(b.x,b.y,x,y,paint)
            paint.color=Color.BLACK; canvas.drawRoundRect(x-10*s,y-12*s,x+10*s,y+12*s,4*s,4*s,paint)
            paint.color=Color.rgb(105,232,140); canvas.drawRoundRect(x-8*s,y-10*s,x+8*s,y+9*s,3*s,3*s,paint)
            canvas.drawCircle(x-5*s,y+10*s,2*s,paint); canvas.drawCircle(x+5*s,y+10*s,2*s,paint)
            paint.color=Color.BLACK; canvas.drawRoundRect(x-5*s,y-6*s,x+5*s,y+s,s,s,paint)
            canvas.drawCircle(x-4*s,y+5*s,1.5f*s,paint); canvas.drawCircle(x+4*s,y+5*s,1.5f*s,paint)
        }
        if(caminho.isEmpty()) { paint.color=Color.LTGRAY; paint.textSize=12*s; paint.textAlign=Paint.Align.CENTER; canvas.drawText("Traçado indisponível",width/2,height-8*s,paint) }
        canvas.restore()
    }
}
