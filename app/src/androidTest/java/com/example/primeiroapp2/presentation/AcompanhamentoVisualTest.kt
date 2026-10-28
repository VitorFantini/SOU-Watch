package com.example.primeiroapp2.presentation

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.primeiroapp2.presentation.data.*
import com.example.primeiroapp2.presentation.theme.Primeiroapp2Theme
import com.example.primeiroapp2.presentation.tile.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max

/** Prévia visual com posições SIMULADAS sobre um trajeto registrado; não consulta veículos reais. */
@RunWith(AndroidJUnit4::class)
class AcompanhamentoVisualTest {
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private fun cenario(restante: Double): BusUiState {
        val detalhes=inst.context.assets.open("trajeto-preview.json").bufferedReader().use { Gson().fromJson(it,TripDetails::class.java) }
        val ponto=RastreamentoCorreto.pontosDoTrajeto(detalhes)[10]
        val caminho=RastreamentoCorreto.decodificar(detalhes.shape!!)
        val alvo=RastreamentoCorreto.projetar(RastreamentoCorreto.Coordenada(ponto.lat,ponto.lng),caminho).progresso-restante
        var percorrido=0.0
        var coordenada=caminho.first()
        for((a,b) in caminho.zipWithNext()) {
            val tamanho=RastreamentoCorreto.distancia(a.lat,a.lng,b.lat,b.lng)
            if(percorrido+tamanho>=alvo) {
                val t=if(tamanho==0.0) 0.0 else ((alvo-percorrido)/tamanho).coerceIn(0.0,1.0)
                coordenada=RastreamentoCorreto.Coordenada(a.lat+(b.lat-a.lat)*t,a.lng+(b.lng-a.lng)*t)
                break
            }
            percorrido+=tamanho
        }
        val agora=Instant.now()
        val bus=BusVehicle(lat=coordenada.lat,lng=coordenada.lng,vehicleId="2072",startTime="13:45",
            positionTime=agora.atZone(RastreamentoCorreto.fuso).toLocalTime().withNano(0).toString())
        return BusUiState(linhaSelecionada=LineRepository.supportedLines.first(),sentidoSelecionado=1,
            horarioSelecionado=BusSchedule("preview","13:45"),pontoSelecionado=ponto,veiculoMaisProximo=bus,
            distanciaAtePonto=if(kotlin.math.abs(restante)>=1000) "1.0 km" else "${kotlin.math.abs(restante).toInt()} m",
            tempoEstimado=if(restante< -80) "após seu ponto" else "~${max(1,ceil(restante/300).toInt())} min · estimado",
            metrosRestantes=restante,gpsValidoAte=agora.plusSeconds(120),
            mapaAcompanhamento=TileMapa(detalhes.shape,detalhes.stops,coordenada),mapsRouteUrl="geo:0,0?q=${bus.lat},${bus.lng}")
    }
    private fun capturar(nome: String): android.graphics.Bitmap {
        val bitmap=inst.uiAutomation.takeScreenshot()
        assertNotNull(bitmap)
        val folder=File(inst.targetContext.getExternalFilesDir(null),"preview-acompanhamento").apply { mkdirs() }
        File(folder,"$nome.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        return bitmap
    }
    private fun pixels(bitmap: android.graphics.Bitmap,cor: Int): Int {
        var n=0
        for(y in 0 until bitmap.height) for(x in 0 until bitmap.width) if(bitmap.getPixel(x,y)==cor) n++
        return n
    }
    @Test fun renderizaEstadosMapaEExpiraGpsSemConsultarRede() {
        val cenarios=listOf(Triple("ainda-nao-passou",1000.0,0xFFA5D6A7.toInt()),Triple("esta-chegando",500.0,0xFF00E676.toInt()),Triple("ja-passou",-150.0,0xFFFF6B6B.toInt()))
        val apenas=InstrumentationRegistry.getArguments().getString("cenario")
        for((nome,restante,cor) in cenarios.filter { apenas==null || it.first==apenas }) {
            val activity=inst.startActivitySync(Intent(inst.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            try {
                val state=cenario(restante)
                inst.runOnMainSync { activity.setContent { Primeiroapp2Theme { TelaAcompanhamento(state,{}, {}) } } }
                inst.waitForIdleSync();SystemClock.sleep(900)
                val topo=capturar(nome)
                assertTrue("Cor do estado $nome ausente",pixels(topo,cor)>0)
                topo.recycle()
                inst.uiAutomation.executeShellCommand("input swipe 240 390 240 190 450").close()
                SystemClock.sleep(800)
                capturar("$nome-mapa").recycle()
            } finally { inst.runOnMainSync { activity.finish() } }
        }
        val activity=inst.startActivitySync(Intent(inst.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            val state=cenario(500.0).copy(gpsValidoAte=Instant.now().plusSeconds(2))
            inst.runOnMainSync { activity.setContent { Primeiroapp2Theme { TelaAcompanhamento(state,{}, {}) } } }
            SystemClock.sleep(3200)
            val expirado=capturar("gps-expirado")
            assertEquals(0,pixels(expirado,0xFF69E88C.toInt()))
            assertEquals(0,pixels(expirado,0xFF00E676.toInt()))
            expirado.recycle()
        } finally { inst.runOnMainSync { activity.finish() } }
    }
}
