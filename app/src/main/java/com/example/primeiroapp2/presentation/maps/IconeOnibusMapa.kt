package com.example.primeiroapp2.presentation.maps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun iconeOnibusMapa(densidade: Float): BitmapDescriptor {
    val bitmap=Bitmap.createBitmap((24*densidade).toInt().coerceAtLeast(24),(28*densidade).toInt().coerceAtLeast(28),Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap);canvas.scale(bitmap.width/24f,bitmap.height/28f)
    val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color=Color.BLACK;canvas.drawRoundRect(2f,1f,22f,26f,5f,5f,paint)
    paint.color=Color.rgb(105,232,140);canvas.drawRoundRect(4f,3f,20f,23f,3f,3f,paint)
    canvas.drawCircle(7f,25f,2f,paint);canvas.drawCircle(17f,25f,2f,paint)
    paint.color=Color.BLACK;canvas.drawRoundRect(7f,7f,17f,15f,1f,1f,paint)
    canvas.drawCircle(8f,20f,1.4f,paint);canvas.drawCircle(16f,20f,1.4f,paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
