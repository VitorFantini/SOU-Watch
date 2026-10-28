package com.example.primeiroapp2.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.example.primeiroapp2.presentation.tile.MeuPontoStore

class MainActivity : ComponentActivity() {
    private lateinit var model: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(250L)
                .withEndAction { provider.remove() }
                .start()
        }
        model=ViewModelProvider(this)[MainViewModel::class.java]
        if(savedInstanceState==null && intent.getBooleanExtra("abrir_meu_ponto",false)) {
            val store=MeuPontoStore(this)
            store.config()?.let { model.abrirMeuPonto(it,store.resultado()) }
        }
        setContent { com.example.primeiroapp2.presentation.theme.Primeiroapp2Theme { BusScreen(model) } }
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if(intent.getBooleanExtra("abrir_meu_ponto",false)) {
            val store=MeuPontoStore(this)
            store.config()?.let { model.abrirMeuPonto(it,store.resultado()) }
        }
    }
    override fun onStart() { super.onStart(); model.definirVisivel(true) }
    override fun onStop() { model.definirVisivel(false); super.onStop() }
}
