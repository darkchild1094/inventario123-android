package com.kernel94.inventario123.ui.firma

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

/** Estado de un pad de firma. `exportarPng()` devuelve la firma como PNG o null si está vacía. */
class FirmaState {
    internal val trazos = mutableStateListOf<List<Offset>>()
    internal var trazoActual by mutableStateOf<List<Offset>>(emptyList())
    internal var anchoPx by mutableStateOf(1)
    internal var altoPx by mutableStateOf(1)

    val vacia: Boolean get() = trazos.isEmpty() && trazoActual.isEmpty()

    fun limpiar() {
        trazos.clear()
        trazoActual = emptyList()
    }

    /** Rasteriza los trazos a un PNG (fondo blanco, trazo negro). null si está vacía. */
    fun exportarPng(maxAncho: Int = 800): ByteArray? {
        if (vacia) return null
        val w = anchoPx.coerceAtLeast(1)
        val h = altoPx.coerceAtLeast(1)
        val escala = if (w > maxAncho) maxAncho.toFloat() / w else 1f
        val bw = (w * escala).toInt().coerceAtLeast(1)
        val bh = (h * escala).toInt().coerceAtLeast(1)

        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val c = AndroidCanvas(bmp)
        c.drawColor(AndroidColor.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f * escala
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        (trazos + listOf(trazoActual)).forEach { puntos ->
            if (puntos.size < 2) return@forEach
            val p = AndroidPath()
            p.moveTo(puntos.first().x * escala, puntos.first().y * escala)
            puntos.drop(1).forEach { p.lineTo(it.x * escala, it.y * escala) }
            c.drawPath(p, paint)
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return out.toByteArray()
    }
}

@Composable
fun rememberFirmaState(): FirmaState = remember { FirmaState() }

@Composable
fun FirmaCanvas(
    state: FirmaState,
    modifier: Modifier = Modifier,
    alto: Int = 180,
) {
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(alto.dp)
                .background(Color.White)
                .border(2.dp, Color(0xFFADB5BD))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> state.trazoActual = listOf(offset) },
                        onDrag = { change, _ ->
                            state.trazoActual = state.trazoActual + change.position
                        },
                        onDragEnd = {
                            if (state.trazoActual.isNotEmpty()) {
                                state.trazos.add(state.trazoActual)
                                state.trazoActual = emptyList()
                            }
                        },
                        onDragCancel = { state.trazoActual = emptyList() },
                    )
                }
        ) {
            state.anchoPx = size.width.toInt()
            state.altoPx = size.height.toInt()
            (state.trazos + listOf(state.trazoActual)).forEach { puntos ->
                if (puntos.size < 2) return@forEach
                val path = Path().apply {
                    moveTo(puntos.first().x, puntos.first().y)
                    puntos.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, color = Color.Black, style = Stroke(width = 4f, cap = StrokeCap.Round))
            }
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = { state.limpiar() }) { Text("Limpiar") }
            Text(
                "Dibuja tu firma con el dedo.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }
    }
}
