package com.kernel94.inventario123.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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

/**
 * Escáner de cámara para leer la serie de un activo: primero intenta leer
 * un código de barras/QR (lo más común en equipos de cómputo/impresoras),
 * y si no encuentra ninguno, cae a reconocimiento de texto (OCR) para que
 * el usuario pueda tocar la línea correcta si la serie viene impresa sin
 * código de barras.
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
    var zoomActual by remember { mutableStateOf(0f) }
    var controlCamara by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (tienePermiso) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
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
                                .enableAllPotentialBarcodes() // Ayuda con códigos difíciles
                                .build()
                        )
                        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        val analysis = ImageAnalysis.Builder()
                            .setResolutionSelector(resolutionSelector)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !yaSeleccionado) {
                                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { codigos ->
                                        // Buscar el primer código que coincida con el filtro entre TODOS los detectados
                                        val match = codigos.firstOrNull { barcode ->
                                            val raw = barcode.rawValue
                                            !raw.isNullOrBlank() && (filtroPrefijo == null || raw.trim().startsWith(filtroPrefijo))
                                        }

                                        if (match != null && !yaSeleccionado) {
                                            yaSeleccionado = true
                                            onCodigoDetectado(match.rawValue!!.trim())
                                        } else if (codigos.isEmpty()) {
                                            // Solo si no hay ningún código de barras, intentamos OCR como respaldo
                                            textRecognizer.process(inputImage)
                                                .addOnSuccessListener { texto ->
                                                    var lineas = texto.textBlocks.flatMap { it.lines }
                                                        .map { it.text.trim() }

                                                    if (modoRegulador) {
                                                        // Lógica especial para Reguladores: buscar "Serie", "SERIE" o "serie"
                                                        // y extraer lo que viene después.
                                                        val regex = Regex("(?i)serie[:\\s]*(.*)")
                                                        lineas = lineas.mapNotNull { linea ->
                                                            val matchResult = regex.find(linea)
                                                            val extraido = matchResult?.groupValues?.get(1)?.trim()
                                                            if (!extraido.isNullOrBlank()) extraido else null
                                                        }
                                                    } else {
                                                        // Comportamiento normal
                                                        lineas = lineas.filter { it.length in 4..40 }
                                                            .filter { filtroPrefijo == null || it.startsWith(filtroPrefijo) }
                                                    }

                                                    if (lineas.isNotEmpty()) {
                                                        // Mantener las sugerencias previas y añadir las nuevas sin duplicados
                                                        val nuevas = (textosDetectados + lineas).distinct().take(6)
                                                        textosDetectados = nuevas
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        } else {
                                            // Hay códigos de barras pero ninguno coincide con el prefijo, 
                                            // cerramos el proxy para procesar el siguiente cuadro
                                            imageProxy.close()
                                        }
                                    }
                                    .addOnFailureListener { imageProxy.close() }
                                    .addOnCompleteListener {
                                        if (yaSeleccionado || mediaImage == null) imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                            controlCamara = camera.cameraControl
                            
                            // Si estamos buscando un UPS (con prefijo), aplicamos un zoom inicial del 20%
                            if (filtroPrefijo != null) {
                                zoomActual = 0.2f
                                controlCamara?.setLinearZoom(zoomActual)
                            }
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
                
                // Selector de Zoom manual
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
