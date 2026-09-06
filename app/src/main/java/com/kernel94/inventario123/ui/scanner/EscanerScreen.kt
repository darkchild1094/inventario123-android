package com.kernel94.inventario123.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.util.Size
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Escáner de cámara para leer la SERIE o el CÓDIGO DE BARRAS de un activo.
 *
 * Mejoras de precisión sobre la versión anterior:
 *  - **Recorte al recuadro guía**: el frame se recorta a la zona central (donde
 *    está el marco blanco) antes de pasarlo a ML Kit. Menos ruido de fondo,
 *    lectura más rápida y estable. Si el recorte falla se usa el frame completo.
 *  - **Validación por campo** (`target`): `codigo_barras` solo acepta 7–14
 *    dígitos; `serie` acepta alfanumérico con `- . /`. Un valor que no cumple la
 *    forma del campo NO se auto-acepta: se ofrece como opción tocable.
 *  - **Votación en todos los modos**: se exige la misma lectura 2 veces seguidas
 *    antes de aceptar (barcode genérico, UPS y OCR).
 *  - **Variante normalizada de OCR**: además del texto crudo se ofrece una
 *    versión con las confusiones típicas corregidas (O→0, I/l→1, S→5, B→8, Z→2).
 *  - **Formatos de inventario priorizados** en el lector de códigos.
 *
 * Modos (según el dispositivo elegido en el formulario):
 *  - UPS (`filtroPrefijo` != null): código diminuto; más zoom, enfoque al centro.
 *  - Regulador (`modoRegulador` = true): sin código útil; OCR tras "SERIE:"/"S/N:".
 *  - Genérico: barcode y, si no hay, líneas de OCR tocables.
 */
@Composable
fun EscanerScreen(
    onCodigoDetectado: (String) -> Unit,
    onCerrar: () -> Unit,
    instruccion: String = "Apunta al código de barras o a la etiqueta",
    filtroPrefijo: String? = null,
    modoRegulador: Boolean = false,
    target: String = "serie",
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val esUps = filtroPrefijo != null
    val esCodigo = target == "codigo" || target == "codigo_barras"

    // Validación por forma del campo objetivo.
    val regexCodigo = remember { Regex("^\\d{7,14}$") }
    val regexSerie = remember { Regex("^[A-Za-z0-9][A-Za-z0-9\\-./]{3,29}$") }
    fun limpiar(v: String) = v.replace(Regex("[\\u202a-\\u202e\\u200e\\u200f\\s]"), "").trim()
    fun cumpleForma(v: String): Boolean {
        val c = limpiar(v)
        return if (esCodigo) regexCodigo.matches(c) else regexSerie.matches(c)
    }
    fun normalizarOcr(v: String): String {
        val c = limpiar(v)
        return if (esCodigo) c.map {
            when (it) { 'O', 'o', 'D' -> '0'; 'I', 'l', 'i' -> '1'; 'S', 's' -> '5'; 'B' -> '8'; 'Z', 'z' -> '2'; 'G' -> '6'; else -> it }
        }.joinToString("") else c
    }

    var tienePermiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val lanzadorPermiso = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { concedido -> tienePermiso = concedido }

    LaunchedEffect(Unit) { if (!tienePermiso) lanzadorPermiso.launch(Manifest.permission.CAMERA) }

    var textosDetectados by remember { mutableStateOf<List<String>>(emptyList()) }
    var yaSeleccionado by remember { mutableStateOf(false) }
    var linternaEncendida by remember { mutableStateOf(false) }
    var zoomActual by remember { mutableStateOf(if (esUps) 0.45f else 0f) }
    var controlCamara by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // ── Confirmación por lecturas repetidas (todos los modos) ────────────
    var candidatoPrevio by remember { mutableStateOf<String?>(null) }
    var candidatoRepeticiones by remember { mutableStateOf(0) }
    fun confirmarCandidato(valor: String, umbral: Int = 2): Boolean {
        return if (valor == candidatoPrevio) {
            candidatoRepeticiones++
            candidatoRepeticiones >= umbral
        } else {
            candidatoPrevio = valor
            candidatoRepeticiones = 1
            false
        }
    }

    fun aceptar(valor: String) {
        if (yaSeleccionado) return
        yaSeleccionado = true
        onCodigoDetectado(limpiar(valor))
    }

    Box(Modifier.fillMaxSize()) {
        if (tienePermiso) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                zoomActual = (zoomActual * zoom).coerceIn(0f, 1f)
                                controlCamara?.setLinearZoom(zoomActual)
                            }
                        }
                    },
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val resolutionSelector = ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER))
                            .build()

                        val preview = Preview.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                        // Formatos típicos de etiquetas de inventario primero, pero
                        // sin excluir el resto.
                        val barcodeScanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                                    Barcode.FORMAT_CODABAR, Barcode.FORMAT_ITF,
                                    Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                                    Barcode.FORMAT_UPC_A, Barcode.FORMAT_QR_CODE,
                                    Barcode.FORMAT_DATA_MATRIX,
                                )
                                .enableAllPotentialBarcodes()
                                .build()
                        )
                        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        val analysis = ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            if (yaSeleccionado) { imageProxy.close(); return@setAnalyzer }
                            val rot = imageProxy.imageInfo.rotationDegrees
                            val recortado = recortarAlCentro(imageProxy)
                            val inputImage = when {
                                recortado != null -> InputImage.fromBitmap(recortado, rot)
                                imageProxy.image != null -> InputImage.fromMediaImage(imageProxy.image!!, rot)
                                else -> { imageProxy.close(); return@setAnalyzer }
                            }

                            if (modoRegulador) {
                                textRecognizer.process(inputImage)
                                    .addOnSuccessListener { texto ->
                                        val lineas = texto.textBlocks.flatMap { it.lines }.map { it.text.trim() }
                                        val regex = Regex(
                                            """(?i)(?:serie|s\s*/\s*n)\s*[:\-]?\s*([A-Za-z0-9][A-Za-z0-9\-./]{2,29})"""
                                        )
                                        fun extraer(linea: String): String? =
                                            regex.find(linea)?.groupValues?.get(1)
                                                ?.trim()?.trimEnd('.', ',', ';', ' ')
                                                ?.takeIf { it.isNotBlank() }
                                        var candidato = lineas.firstNotNullOfOrNull { extraer(it) }
                                        if (candidato == null && lineas.size > 1) {
                                            candidato = lineas.zipWithNext { a, b -> "$a $b" }
                                                .firstNotNullOfOrNull { extraer(it) }
                                        }
                                        if (candidato != null && confirmarCandidato(candidato)) aceptar(candidato)
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { codigos ->
                                        val match = codigos.firstOrNull { barcode ->
                                            val raw = barcode.rawValue
                                            !raw.isNullOrBlank() && (filtroPrefijo == null ||
                                                filtroPrefijo.split(",").any { p -> raw.trim().startsWith(p.trim(), ignoreCase = true) })
                                        }
                                        when {
                                            match != null -> {
                                                val valor = limpiar(match.rawValue!!)
                                                // Votación SIEMPRE (antes solo UPS).
                                                if (confirmarCandidato(valor)) {
                                                    if (cumpleForma(valor)) aceptar(valor)
                                                    else if (!yaSeleccionado) {
                                                        textosDetectados = (textosDetectados + valor).distinct().take(6)
                                                    }
                                                }
                                                imageProxy.close()
                                            }
                                            filtroPrefijo == null -> {
                                                textRecognizer.process(inputImage)
                                                    .addOnSuccessListener { texto ->
                                                        val lineas = texto.textBlocks.flatMap { it.lines }
                                                            .map { limpiar(it.text) }
                                                            .filter { it.length in 4..40 }
                                                        if (lineas.isNotEmpty() && !yaSeleccionado) {
                                                            val conNormalizadas = lineas.flatMap { l ->
                                                                val n = normalizarOcr(l)
                                                                if (n != l && n.isNotBlank()) listOf(l, n) else listOf(l)
                                                            }
                                                            // Empuja al frente las que cumplen la forma del campo.
                                                            val ordenadas = conNormalizadas.sortedByDescending { cumpleForma(it) }
                                                            textosDetectados = (textosDetectados + ordenadas).distinct().take(6)
                                                        }
                                                    }
                                                    .addOnCompleteListener { imageProxy.close() }
                                            }
                                            else -> imageProxy.close()
                                        }
                                    }
                                    .addOnFailureListener { imageProxy.close() }
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                            controlCamara = camera.cameraControl
                            controlCamara?.setLinearZoom(zoomActual)

                            val puntoCentral = previewView.meteringPointFactory.createPoint(0.5f, 0.5f)
                            val accionEnfoque = FocusMeteringAction.Builder(
                                puntoCentral,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                            camera.cameraControl.startFocusAndMetering(accionEnfoque)
                        } catch (_: Exception) { }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        linternaEncendida = !linternaEncendida
                        controlCamara?.enableTorch(linternaEncendida)
                    }
                ) {
                    Icon(
                        if (linternaEncendida) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Linterna",
                        tint = Color.White
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        zoomActual = (zoomActual - 0.1f).coerceAtLeast(0f)
                        controlCamara?.setLinearZoom(zoomActual)
                    }) { Text("-", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
                    Text("Zoom", color = Color.White)
                    TextButton(onClick = {
                        zoomActual = (zoomActual + 0.1f).coerceAtMost(1f)
                        controlCamara?.setLinearZoom(zoomActual)
                    }) { Text("+", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
                    .height(120.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                ) {}
            }

            Text(
                instruccion,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp)
            )

            if (esUps) {
                Text(
                    "Pellizca la pantalla para acercar el zoom",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 118.dp)
                )
            }

            if (textosDetectados.isNotEmpty() && !yaSeleccionado) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if (esCodigo) "Toca el CÓDIGO correcto (dígitos):" else "Toca la línea que sea la SERIE:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        textosDetectados.forEach { linea ->
                            TextButton(onClick = { aceptar(linea) }) {
                                Text(if (cumpleForma(linea)) "✓  $linea" else linea)
                            }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Se necesita permiso de cámara para escanear.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { lanzadorPermiso.launch(Manifest.permission.CAMERA) }) { Text("Conceder permiso") }
            }
        }
    }
}

/**
 * Recorta el frame a la franja central (≈92 % ancho × ≈45 % alto) donde queda el
 * marco guía. Devuelve null si la conversión falla (se usa el frame completo).
 */
private fun recortarAlCentro(imageProxy: ImageProxy): Bitmap? {
    return try {
        val image = imageProxy.image ?: return null
        if (image.format != ImageFormat.YUV_420_888) return null

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val w = image.width
        val h = image.height
        val yuv = YuvImage(nv21, ImageFormat.NV21, w, h, null)
        val out = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, w, h), 92, out)
        val bytes = out.toByteArray()
        val full = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val cw = (w * 0.92f).toInt().coerceAtMost(w)
        val ch = (h * 0.45f).toInt().coerceAtMost(h)
        val cx = ((w - cw) / 2).coerceAtLeast(0)
        val cy = ((h - ch) / 2).coerceAtLeast(0)
        val crop = Bitmap.createBitmap(full, cx, cy, cw, ch)
        if (crop != full) full.recycle()
        crop
    } catch (e: Exception) {
        null
    }
}
