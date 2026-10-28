package com.example.primeiroapp2.presentation

import android.app.Application
import android.net.ConnectivityManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.primeiroapp2.presentation.data.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RedeCicloVidaTest {
    private val inst get()=InstrumentationRegistry.getInstrumentation()
    private fun exigirOffline() {
        assumeTrue("Executar somente na rodada offline controlada",InstrumentationRegistry.getArguments().getString("offlineConfirmado")=="true")
        val manager=inst.targetContext.getSystemService(ConnectivityManager::class.java)
        assertNull("O teste precisa de ausencia real de rede ativa",manager.activeNetwork)
    }
    @Test fun consultaSemRedeFalhaSemFicarPresa()=runBlocking {
        exigirOffline()
        val inicio=SystemClock.elapsedRealtime();var falha=false
        try { withTimeout(25_000) { BusRepository.tabela(LineRepository.supportedLines.first().id) } }
        catch(e: Exception) { falha=true;assertFalse("Nao deve precisar do timeout externo",e is TimeoutCancellationException) }
        assertTrue("Consulta offline nao pode parecer sucesso",falha)
        val elapsed=SystemClock.elapsedRealtime()-inicio
        assertTrue(elapsed<23_000)
        val folder=File(inst.targetContext.getExternalFilesDir(null),"estresse").apply { mkdirs() }
        File(folder,"offline.json").writeText(Gson().toJson(mapOf("failedAsExpected" to falha,"durationMs" to elapsed)))
    }
    @Test fun sairCancelaConsultaEPollingDoViewModelMesmoSemRede() {
        exigirOffline()
        val field=MainViewModel::class.java.getDeclaredField("trabalho").apply { isAccessible=true }
        repeat(20) {
            lateinit var model:MainViewModel
            inst.runOnMainSync {
                model=MainViewModel(inst.targetContext.applicationContext as Application)
                model.definirVisivel(true)
                model.selecionarLinha(LineRepository.supportedLines.first())
                model.selecionarSentido(0)
            }
            SystemClock.sleep(30)
            inst.runOnMainSync {
                model.definirVisivel(false)
                val job=field.get(model) as? Job
                assertTrue("Consulta/polling permaneceu ativo apos sair",job==null || !job.isActive)
                assertNull(model.uiState.gpsValidoAte)
                assertNull(model.uiState.veiculoMaisProximo)
                assertTrue(model.uiState.minutosPorPonto.isEmpty())
            }
        }
    }
}
