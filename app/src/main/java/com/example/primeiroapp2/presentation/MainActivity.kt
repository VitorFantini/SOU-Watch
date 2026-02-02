package com.example.primeiroapp2.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CRIAÇÃO DO VIEWMODEL
        val viewModel = MainViewModel(application)

        // Define o conteúdo da tela usando Compose
        setContent {
            BusScreen(viewModel = viewModel)
        }
    }
}