package com.kernel94.inventario123.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
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
import java.util.concurrent.TimeUnit

/**
 * Escáner de cámara para leer la serie de un activo.
 *
 * Tres modos, según el dispositivo seleccionado en el formulario:
 * - UPS (filtroPrefijo = "3S"): el código de barras es MUY pequeño en la
 *   etiqueta. Se optimiza con más zoom inicial, enfoque automático al
 *   centro del recuadro guía, zoom con pellizco (pinch) para acercarse
 *   manualmente, y una confirmación de 2 lecturas iguales seguidas antes
 *   de aceptar el valor (evita que una lectura parcial/ruidosa se acepte
 *   de un solo frame).
 * - Regulador (modoRegulador = true): no tiene código de barras utilizable;
 *   se usa OCR y se extrae solo el texto que viene después de "serie:",
 *   "Serie:", "SERIE:" o "S/N:" en la etiqueta. También exige 2 lecturas
 *   iguales seguidas antes de aceptar.
 * - Genérico (sin prefijo ni modoRegulador): comportamiento original,
 *   intenta código de barras y si no hay, muestra líneas de OCR para que
 *   el usuario toque la correcta.
 */
@Composable
fun EscanerScreen(
    onCodigoDetectado: (String) -> Unit,
    onCerrar: () -> Unit,
    instruccion: String = "Apunta al código de barras o a la etiqueta",
    filtroPrefijo: String? = null,
    modoRegulador: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val esUps = filtroPrefijo != null

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
    // UPS: arrancamos con más zoom porque el código es muy pequeño.
    var zoomActual by remember { mutableStateOf(if (esUps) 0.45f else 0f) }
    var controlCamara by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // ── Confirmación por lecturas repetidas ──────────────────────────────
    // Para UPS (barcode) y Regulador (OCR), exigimos que la MISMA lectura
    // salga 2 veces seguidas antes de aceptarla como definitiva. Con un
    // código diminuto o texto impreso de baja calidad, una sola lectura
    // puede venir incompleta o mal interpretada; dos lecturas iguales
    // consecutivas casi garantizan que es correcta, a costa de una
    // fracción de segundo extra (imperceptible para el usuario).
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

    Box(Modifier.fillMaxSize()) {
        if (tienePermiso) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    // Pellizcar para acercar/alejar manualmente: clave para
                    // poder ajustar con precisión sobre un código muy chico.
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

                        // Configuración de resolución para mayor precisión (1080p para códigos pequeños)
                        val resolutionSelector = ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER))
                            .build()

                        val preview = Preview.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                        val barcodeScanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                .enableAllPotentialBarcodes() // Ayuda con códigos difíciles/pequeños
                                .build()
                        )
                        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        val analysis = ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage == null || yaSeleccionado) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                            if (modoRegulador) {
                                // Regulador: no tiene barcode utilizable, vamos directo a OCR
                                // en cada frame (más rápido y enfocado que intentar barcode primero).
                                textRecognizer.process(inputImage)
                                    .addOnSuccessListener { texto ->
                                        val lineas = texto.textBlocks.flatMap { it.lines }.map { it.text.trim() }

                                        // Regex: acepta "serie", "Serie", "SERIE" o "S/N" (con o sin
                                        // espacios alrededor de la diagonal), seguido de ":" opcional
                                        // y el valor. NO se ancla al inicio de línea, para capturar
                                        // también "MODELO X SERIE: ABC123" en una sola línea.
                                        val regex = Regex(
                                            """(?i)(?:serie|s\s*/\s*n)\s*[:\-]?\s*([A-Za-z0-9][A-Za-z0-9\-./]{2,29})"""
                                        )

                                        fun extraer(linea: String): String? =
                                            regex.find(linea)?.groupValues?.get(1)
                                                ?.trim()?.trimEnd('.', ',', ';', ' ')
                                                ?.takeIf { it.isNotBlank() }

                                        // 1) intento línea por línea
                                        var candidato = lineas.firstNotNullOfOrNull { extraer(it) }

                                        // 2) si el OCR partió "SERIE:" y el valor en dos líneas
                                        //    distintas, probamos también líneas consecutivas unidas.
                                        if (candidato == null && lineas.size > 1) {
                                            candidato = lineas.zipWithNext { a, b -> "$a $b" }
                                                .firstNotNullOfOrNull { extraer(it) }
                                        }

                                        if (candidato != null) {
                                            if (confirmarCandidato(candidato) && !yaSeleccionado) {
                                                yaSeleccionado = true
                                                onCodigoDetectado(candidato)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { codigos ->
                                        val match = codigos.firstOrNull { barcode ->
                                            val raw = barcode.rawValue
                                            !raw.isNullOrBlank() && (filtroPrefijo == null || filtroPrefijo.split(",").any { p -> raw.trim().startsWith(p.trim(), ignoreCase = true) })
                                        }

                                        when {
                                            match != null && esUps -> {
                                                // UPS: exigir 2 lecturas iguales seguidas antes de aceptar,
                                                // por lo pequeño/sensible que es el código.
                                                val valor = match.rawValue!!.trim()
                                                if (confirmarCandidato(valor) && !yaSeleccionado) {
                                                    yaSeleccionado = true
                                                    onCodigoDetectado(valor)
                                                }
                                                imageProxy.close()
                                            }
                                            match != null -> {
                                                // Placa u otro escaneo genérico: aceptar de inmediato (comportamiento original).
                                                yaSeleccionado = true
                                                onCodigoDetectado(match.rawValue!!.trim())
                                                imageProxy.close()
                                            }
                                            filtroPrefijo == null -> {
                                                // Sin código de barras válido: OCR como respaldo, el usuario
                                                // toca la línea correcta (comportamiento original).
                                                textRecognizer.process(inputImage)
                                                    .addOnSuccessListener { texto ->
                                                        val lineas = texto.textBlocks.flatMap { it.lines }
                                                            .map { it.text.trim() }
                                                            .filter { it.length in 4..40 }
                                                        if (lineas.isNotEmpty() && !yaSeleccionado) {
                                                            val nuevas = (textosDetectados + lineas).distinct().take(6)
                                                            textosDetectados = nuevas
                                                        }
                                                    }
                                                    .addOnCompleteListener { imageProxy.close() }
                                            }
                                            else -> {
                                                // UPS sin match este frame: seguimos esperando.
                                                imageProxy.close()
                                            }
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

                            // Enfoque automático centrado en el recuadro guía. Fundamental
                            // para códigos de barras muy pequeños (UPS): sin esto, la cámara
                            // puede quedarse enfocada en el fondo en vez de en la etiqueta.
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

            // Controles superiores
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
                
                // Selector de Zoom manual (también se puede hacer pellizco sobre la imagen)
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

            // Marco guía visual
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

            // Resultados de OCR como respaldo: el usuario toca la línea correcta
            if (textosDetectados.isNotEmpty() && !yaSeleccionado) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("No se detectó código de barras. Toca la línea que sea la serie:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        textosDetectados.forEach { linea ->
                            TextButton(onClick = { yaSeleccionado = true; onCodigoDetectado(linea) }) { Text(linea) }
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
