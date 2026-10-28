package com.example.primeiroapp2.presentation.maps

import com.example.primeiroapp2.presentation.tile.TileConfig
import com.example.primeiroapp2.presentation.tile.TileResultado
import java.security.MessageDigest
import com.google.gson.Gson

/** Recursos transitórios da entrega do Tile, separados do cache persistente dos mapas locais. */
object RecursosGoogleTile {
    data class Recursos(val versao: String,val assinatura: String,val png: ByteArray,val semGps: ByteArray)
    private val recursos=linkedMapOf<String,Recursos>()
    fun assinatura(config: TileConfig,result: TileResultado?)=MessageDigest.getInstance("SHA-256")
        .digest(Gson().toJson(listOf(config,result)).toByteArray()).joinToString("") { "%02x".format(it) }
    @Synchronized fun registrar(assinatura: String,imagem: ClienteMapaStatic.Imagem): Recursos {
        val hash=MessageDigest.getInstance("SHA-256").apply { update(imagem.png);update(imagem.semGps);update(assinatura.toByteArray()) }
            .digest().joinToString("") { "%02x".format(it) }
        val valor=Recursos("gm-$hash",assinatura,imagem.png,imagem.semGps)
        recursos[valor.versao]=valor
        while(recursos.size>3) recursos.remove(recursos.keys.first())
        return valor
    }
    @Synchronized fun obter(versao: String)=recursos[versao]
}
