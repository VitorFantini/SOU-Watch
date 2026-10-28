package com.example.primeiroapp2.presentation.maps

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto.Coordenada
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material.Text
import com.example.primeiroapp2.R
import com.example.primeiroapp2.presentation.data.RastreamentoCorreto
import com.example.primeiroapp2.presentation.tile.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay

/** Sem Map ID. A instância é mantida entre atualizações do ônibus. */
@Composable fun MapaComAlternativa(
    config: TileConfig,mapa: TileMapa?,modifier: Modifier=Modifier,
    aproximar: Boolean=false,interativo: Boolean=false,recentralizar: Int=0,
    onAbrir: (() -> Unit)?=null,permitirGoogle: Boolean=true
) {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val abrirAtual by rememberUpdatedState(onAbrir)
    val chave=remember(context) { MapasConfig.chaveAndroid(context) }
    var falhou by remember(config.chave,interativo) { mutableStateOf(false) }
    var carregou by remember(config.chave,interativo) { mutableStateOf(false) }
    var googleMap by remember(config.chave,interativo) { mutableStateOf<GoogleMap?>(null) }
    val view=remember(config.chave,interativo,chave,permitirGoogle,falhou) {
        if(!permitirGoogle || falhou || chave.isBlank()) null else runCatching {
            // Inicia com o trajeto enquadrado, antes mesmo da primeira imagem do Lite.
            val caminhoInicial=runCatching { RastreamentoCorreto.decodificar(mapa?.shape.orEmpty()) }.getOrDefault(emptyList())
            val inicial=ViewportMapa.enquadrar(pontosDoEnquadramento(
                Coordenada(config.ponto.lat,config.ponto.lng),mapa?.onibus,caminhoInicial,aproximar),192,192,margem=24)
            MapView(context,GoogleMapOptions().liteMode(!interativo).mapType(GoogleMap.MAP_TYPE_NORMAL)
                .camera(CameraPosition.fromLatLngZoom(LatLng(inicial.centro.lat,inicial.centro.lng),inicial.zoom.toFloat())))
                .apply { onCreate(Bundle()) }
        }.getOrNull()
    }
    DisposableEffect(view,lifecycle) {
        var destruido=false;var iniciado=false;var retomado=false
        fun destruir() {
            if(view!=null && !destruido) {
                if(retomado) runCatching { view.onPause() }
                if(iniciado) runCatching { view.onStop() }
                runCatching { view.onDestroy() };destruido=true;retomado=false;iniciado=false
            }
        }
        val observer=LifecycleEventObserver { _,event ->
            if(view!=null && !destruido) runCatching {
                when(event) {
                    Lifecycle.Event.ON_START -> if(!iniciado) { view.onStart();iniciado=true }
                    Lifecycle.Event.ON_RESUME -> if(!retomado) { view.onResume();retomado=true }
                    Lifecycle.Event.ON_PAUSE -> if(retomado) { view.onPause();retomado=false }
                    Lifecycle.Event.ON_STOP -> if(iniciado) { view.onStop();iniciado=false }
                    Lifecycle.Event.ON_DESTROY -> destruir()
                    else -> Unit
                }
            }.onFailure { falhou=true }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer);destruir() }
    }
    DisposableEffect(view) {
        var ativo=true
        if(view!=null) runCatching {
            view.getMapAsync { map ->
                if(!ativo) return@getMapAsync
                googleMap=map
                map.uiSettings.apply {
                    isMapToolbarEnabled=false;isMyLocationButtonEnabled=false
                    isCompassEnabled=interativo;isZoomControlsEnabled=false
                    setAllGesturesEnabled(interativo)
                }
                map.isBuildingsEnabled=false;map.isIndoorEnabled=false
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context,R.raw.mapa_escuro))
                val d=context.resources.displayMetrics.density
                if(interativo) map.setPadding(0,(28*d).toInt(),0,(64*d).toInt())
                map.setOnMapLoadedCallback { carregou=true }
                if(!interativo) map.setOnMapClickListener { abrirAtual?.invoke() }
            }
        }.onFailure { falhou=true }
        onDispose { ativo=false;googleMap?.setOnMapLoadedCallback(null);googleMap=null }
    }
    LaunchedEffect(view,lifecycle) {
        if(view!=null) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(15_000)
            if(!carregou) falhou=true
        }
    }
    var ultimoShape by remember(view) { mutableStateOf<String?>(null) }
    var linha by remember(view) { mutableStateOf<Polyline?>(null) }
    var parada by remember(view) { mutableStateOf<Marker?>(null) }
    var onibus by remember(view) { mutableStateOf<Marker?>(null) }
    var camera by remember(view) { mutableStateOf<String?>(null) }
    var dimensoes by remember(view) { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(googleMap,mapa,aproximar,recentralizar,dimensoes,carregou) {
        val map=googleMap ?: return@LaunchedEffect
        runCatching {
            val ponto=LatLng(config.ponto.lat,config.ponto.lng)
            if(parada==null) parada=map.addMarker(MarkerOptions().position(ponto).title("Seu ponto").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)))
            val pos=mapa?.onibus
            if(pos==null) { onibus?.remove();onibus=null }
            else {
                val local=LatLng(pos.lat,pos.lng)
                if(onibus==null) onibus=map.addMarker(MarkerOptions().position(local).title("Ônibus com GPS recente").icon(iconeOnibusMapa(context.resources.displayMetrics.density)).anchor(.5f,.5f))
                else onibus?.position=local
            }
            val caminho=runCatching { RastreamentoCorreto.decodificar(mapa?.shape.orEmpty()) }.getOrDefault(emptyList()).map { LatLng(it.lat,it.lng) }
            if(ultimoShape!=mapa?.shape) {
                linha?.remove();linha=if(caminho.size>=2) map.addPolyline(PolylineOptions().addAll(caminho).color(AndroidColor.rgb(138,180,248)).width(4f)) else null
                ultimoShape=mapa?.shape
            }
            val enquadrar=pontosDoEnquadramento(Coordenada(ponto.latitude,ponto.longitude),pos,caminho.map { Coordenada(it.latitude,it.longitude) },aproximar)
            val assinatura=if(interativo) "$recentralizar:$aproximar:$dimensoes:${mapa?.shape}:$carregou" else "$recentralizar:$aproximar:$pos:${mapa?.shape}:$dimensoes:$carregou"
            if(camera!=assinatura) {
                // O Lite recebe centro e zoom explícitos, em pixels lógicos, após o layout.
                val density=context.resources.displayMetrics.density
                val width=(dimensoes.width/density).toInt()
                val height=(dimensoes.height/density).toInt()-(if(interativo) 92 else 0)
                if(width>=96 && height>=96) {
                    val viewport=ViewportMapa.enquadrar(enquadrar,width,height,margem=24)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(viewport.centro.lat,viewport.centro.lng),viewport.zoom.toFloat()))
                    camera=assinatura
                }
            }
        }.onFailure { falhou=true }
    }
    Box(modifier.clipToBounds()) {
        if(!carregou || falhou || view==null) Canvas(Modifier.fillMaxSize().then(if(onAbrir!=null) Modifier.clickable { onAbrir() } else Modifier)) {
            MeuPontoMapaRenderer.desenharMapa(drawContext.canvas.nativeCanvas,config,mapa,size.width,size.height,aproximar)
        }
        if(view!=null && !falhou) AndroidView(factory={ view },modifier=Modifier.fillMaxSize().onSizeChanged { dimensoes=it }.alpha(if(carregou) 1f else .01f))
        if(falhou || view==null) Text("Mapa local",color=Color.LightGray,fontSize=9.sp,textAlign=TextAlign.Center,
            modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=if(interativo) 66.dp else 2.dp))
    }
}
