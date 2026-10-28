package com.example.primeiroapp2.presentation.theme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.*
val Amarelo = Color(0xFFFFC928)
val TextoPrincipal = Color(0xFFE8E8E8)
val Superficie = Color(0xFF171717)
@Composable fun Primeiroapp2Theme(content: @Composable () -> Unit) {
    MaterialTheme(colors = Colors(primary=TextoPrincipal, primaryVariant=TextoPrincipal, secondary=TextoPrincipal,
        background=Color.Black, surface=Superficie, onPrimary=Color.Black,
        onSecondary=Color.Black, onBackground=Color.White, onSurface=Color.White), content=content)
}
