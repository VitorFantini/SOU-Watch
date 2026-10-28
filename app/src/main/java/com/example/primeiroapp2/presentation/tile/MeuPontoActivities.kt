package com.example.primeiroapp2.presentation.tile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.example.primeiroapp2.presentation.*
import com.example.primeiroapp2.presentation.maps.*
import kotlinx.coroutines.*
import java.time.Instant

class MeuPontoSetupActivity : ComponentActivity() {
    private lateinit var model: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model=ViewModelProvider(this)[MainViewModel::class.java]
        setContent {
            com.example.primeiroapp2.presentation.theme.Primeiroapp2Theme { BusScreen(model,onConfigurarPonto={ config ->
                MeuPontoStore(this).salvar(config)
                MeuPontoAtualizador.atualizar(this)
                MeuPontoAtualizador.solicitar(this)
                android.widget.Toast.makeText(this,"Ponto salvo no Tile",android.widget.Toast.LENGTH_SHORT).show()
                finish()
            }) }
        }
    }
    override fun onStart() { super.onStart(); model.definirVisivel(true) }
    override fun onStop() { model.definirVisivel(false); super.onStop() }
}
class MeuPontoMapaActivity : ComponentActivity() {
    private var resultado by mutableStateOf<TileResultado?>(null)
    private var config by mutableStateOf<TileConfig?>(null)
    private var agora by mutableStateOf(Instant.now())
    private var carregando by mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                MeuPontoAtualizador.atualizar(this@MeuPontoMapaActivity)
                val store=MeuPontoStore(this@MeuPontoMapaActivity)
                while(isActive) { config=store.config(); resultado=store.resultado(); agora=Instant.now(); carregando=MeuPontoAtualizador.carregando; delay(if(carregando) 1000 else 15_000) }
            }
        }
        setContent {
            var aproximar by remember { mutableStateOf(false) }
            BackHandler { finish() }
            val c=config; val r=resultado?.atual(agora)
            com.example.primeiroapp2.presentation.theme.Primeiroapp2Theme {
                ListaRelogio {
                    item { Legenda("Meu ponto · mapa",com.example.primeiroapp2.presentation.theme.TextoPrincipal) }
                    if(c==null) {
                        item { Legenda("Escolha uma parada para acompanhar") }
                        item { Acao("Configurar ponto") { startActivity(android.content.Intent(this@MeuPontoMapaActivity,MeuPontoSetupActivity::class.java)) } }
                    } else {
                        item { Legenda("Ponto ${c.ponto.sequencia} · Linha ${c.linha.numero}") }
                        item { Legenda(c.ponto.nome) }
                        item { MapaComAlternativa(c,r?.mapa?.let { if(r.estado==TileEstado.CHEGADA) it else it.copy(onibus=null) },Modifier.fillMaxWidth().height(130.dp),aproximar=aproximar,
                            onAbrir={ abrirMapaGoogle(this@MeuPontoMapaActivity,PedidoMapa(c,r?.mapa?.let { if(r.estado==TileEstado.CHEGADA) it else it.copy(onibus=null) },r?.validoAte()?.times(1000))) }) }
                        item { Legenda(r?.titulo() ?: "Aguardando atualização",when(r?.estado) {
                            TileEstado.CHEGADA -> Color(0xFF69E88C)
                            TileEstado.PROGRAMADO -> com.example.primeiroapp2.presentation.theme.TextoPrincipal
                            else -> Color.LightGray
                        }) }
                        item { Legenda(if(carregando) "Atualizando…" else r?.mensagem.orEmpty()) }
                        item { Legenda("Amarelo: ponto · Verde: ônibus\nToque no mapa para ampliar") }
                        item { Acao(if(aproximar) "Ver trajeto todo" else "Aproximar ônibus e ponto") { aproximar=!aproximar } }
                        item { Acao("Atualizar") { MeuPontoAtualizador.atualizar(this@MeuPontoMapaActivity) } }
                    }
                    item { Acao("Voltar") { finish() } }
                }
            }
        }
    }
}
