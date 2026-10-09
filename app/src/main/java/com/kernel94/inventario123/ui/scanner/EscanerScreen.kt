package com.kernel94.inventario123.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
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
import java.util.concurrent.Executors
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
 * Modos (según el dispositivo elegido en el formulario, vía hints del backend):
 *  - UPS (`filtroPrefijo` != null, `zoomAlto` = true): código diminuto; más zoom, enfoque al centro.
 *  - Regulador (`modoRegulador` = true): sin código útil; OCR tras "SERIE:"/"S/N:".
 *  - Genérico: barcode y, si no hay, líneas de OCR tocables.
 * `codigoLongitud`/`codigoSoloDigitos` fijan la forma válida del código de barras
 * (por defecto 8 dígitos); vienen del hint del dispositivo, no están fijos en código.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun EscanerScreen(
    onCodigoDetectado: (String) -> Unit,
    onCerrar: () -> Unit,
    instruccion: String = "Apunta al código de barras o a la etiqueta",
    filtroPrefijo: String? = null,
    modoRegulador: Boolean = false,
    target: String = "serie",
    zoomAlto: Boolean = false,
    codigoLongitud: Int = 8,
    codigoSoloDigitos: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val esCodigo = target == "codigo" || target == "codigo_barras"

    // Validación por forma del campo objetivo. El código de barras usa la
    // longitud/tipo que mande el hint del dispositivo (por defecto 8 dígitos);
    // así un dispositivo con etiquetas distintas no se valida como si fueran todas iguales.
    // codigoLongitud <= 0: sin dispositivo objetivo fijo (p.ej. escaneo de
    // inventario de bodega contra una lista mixta de equipos) — no se puede
    // exigir un solo formato, así que se acepta cualquier código no vacío
    // con forma razonable en vez de forzar 8 dígitos u otro formato ajeno.
    val regexCodigo = remember(codigoLongitud, codigoSoloDigitos) {
        if (codigoLongitud <= 0) null
        else if (codigoSoloDigitos) Regex("^\\d{$codigoLongitud}$")
        else Regex("^[A-Za-z0-9]{$codigoLongitud}$")
    }
    val regexSerie = remember { Regex("^[A-Za-z0-9][A-Za-z0-9\\-./]{3,29}$") }
    fun limpiar(v: String) = v.replace(Regex("[\\u202a-\\u202e\\u200e\\u200f\\s]"), "").trim()
    fun cumpleForma(v: String): Boolean {
        val c = limpiar(v)
        return if (esCodigo) (regexCodigo?.matches(c) ?: regexSerie.matches(c)) else regexSerie.matches(c)
    }
    fun normalizarOcr(v: String): String {
        val c = limpiar(v)
        return if (esCodigo) c.map {
            when (it) { 'O', 'o', 'D' -> '0'; 'I', 'l', 'i' -> '1'; 'S', 's' -> '5'; 'B' -> '8'; 'Z', 'z' -> '2'; 'G' -> '6'; else -> it }
        }.joinToString("") else c
    }

    /**
     * Extrae números de serie en formatos comunes de etiquetas (TC58B1, etc).
     * Busca patrones como "EID:", "S/N:", "SN:", "SERIE:", "MAE ID:", etc.
     */
    fun extraerSerieFormatos(linea: String): String? {
        val patrones = listOf(
            // "S/N: 26170524200696"
            Regex("(?i)s\\s*(?:/|\\|)\\s*n\\s*[:\\-]?\\s*([A-Za-z0-9]{6,20})"),
            // "EID: 8904903200000100000002946488"
            Regex("(?i)eid\\s*[:\\-]?\\s*([A-Za-z0-9]{6,30})"),
            // "SERIE: ..."
            Regex("(?i)serie\\s*[:\\-]?\\s*([A-Za-z0-9\\-./]{3,30})"),
            // "MAE ID: ..."
            Regex("(?i)mae\\s+id\\s*[:\\-]?\\s*([A-Za-z0-9]{6,20})"),
            // Números secuenciales largos (19+ dígitos tipo IMEI/EID)
            Regex("\\b([0-9]{19,30})\\b"),
            // Números medianos (10-18 dígitos tipo S/N)
            Regex("\\b([0-9]{10,18})\\b")
        )
        for (patron in patrones) {
            patron.find(linea)?.groupValues?.get(1)?.let {
                val limpio = limpiar(it).takeIf { s -> s.isNotBlank() }
                if (limpio != null) return limpio
            }
        }
        return null
    }

    /**
     * "SN:" o "S/N:" seguido del valor, sin exigirle ninguna forma al número
     * de serie en sí (ni longitud ni charset) — etiquetas como la del TC58B1
     * traen varios números (EID, MAC, IMEI) y el prefijo "SN"/"S/N" es la
     * única señal confiable de cuál es la serie; una vez que aparece ese
     * prefijo, se confía en lo que sigue en vez de rechazarlo por su forma.
     * Solo corta en el primer espacio para no arrastrar texto de más.
     */
    fun extraerPorPrefijoSN(linea: String): String? {
        val regex = Regex("(?i)s\\s*/?\\s*n\\s*:\\s*(\\S+)")
        return regex.find(linea)?.groupValues?.get(1)
            ?.trimEnd('.', ',', ';')
            ?.takeIf { it.isNotBlank() }
            ?.let { limpiar(it) }
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
    var zoomActual by remember { mutableStateOf(if (zoomAlto) 0.45f else 0f) }
    var controlCamara by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // Hilo propio para el análisis de cada frame: antes corría en el hilo
    // principal (ContextCompat.getMainExecutor) y, junto con el recorte vía
    // JPEG que había antes, saturaba la CPU y dejaba a ML Kit sin tiempo de
    // devolver nada — eso era el "no escanea nada", no el reconocimiento en sí.
    val executorAnalisis = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executorAnalisis.shutdown() } }

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

                        analysis.setAnalyzer(executorAnalisis) { imageProxy ->
                            if (yaSeleccionado) { imageProxy.close(); return@setAnalyzer }
                            val rot = imageProxy.imageInfo.rotationDegrees
                            val recorte = recortarNV21AlCentro(imageProxy)
                            val inputImage = when {
                                recorte != null -> InputImage.fromByteArray(
                                    recorte.datos, recorte.ancho, recorte.alto, rot,
                                    InputImage.IMAGE_FORMAT_NV21
                                )
                                imageProxy.image != null -> InputImage.fromMediaImage(imageProxy.image!!, rot)
                                else -> { imageProxy.close(); return@setAnalyzer }
                            }

                            if (modoRegulador) {
                                textRecognizer.process(inputImage)
                                    .addOnSuccessListener { texto ->
                                        val lineas = texto.textBlocks.flatMap { it.lines }.map { it.text.trim() }
                                        // Intenta extraer del formato estándar "SERIE:", "S/N:", "EID:", etc.
                                        var candidato = lineas.firstNotNullOfOrNull { extraerSerieFormatos(it) }
                                        // Si no encontró, intenta combinaciones de dos líneas consecutivas
                                        if (candidato == null && lineas.size > 1) {
                                            candidato = lineas.zipWithNext { a, b -> "$a $b" }
                                                .firstNotNullOfOrNull { extraerSerieFormatos(it) }
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
                                                        val lineasCrudas = texto.textBlocks.flatMap { it.lines }.map { it.text.trim() }

                                                        // Serie (no código): si la etiqueta trae "SN:"/"S/N:", se toma
                                                        // ese valor directo, sin pasarlo por cumpleForma() — es la
                                                        // señal más confiable para distinguir la serie de los demás
                                                        // números de la etiqueta (EID, MAC, IMEI...), y no hay que
                                                        // rechazarla solo porque no calce con un formato esperado.
                                                        val candidatoSN = if (!esCodigo) {
                                                            lineasCrudas.firstNotNullOfOrNull { extraerPorPrefijoSN(it) }
                                                        } else null

                                                        if (candidatoSN != null && confirmarCandidato(candidatoSN)) {
                                                            aceptar(candidatoSN)
                                                            return@addOnSuccessListener
                                                        }

                                                        val lineas = lineasCrudas.map { limpiar(it) }
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

            // Bloque superior completo (controles + instrucción) en una sola
            // Column: antes cada pieza se posicionaba con align+padding fijo
            // de forma independiente, y al no coincidir las alturas reales
            // una tapaba a la otra (se veía la instrucción mezclada con los
            // botones). Apilado en una Column no hay forma de que se crucen.
            // .statusBarsPadding() es lo que faltaba para Xiaomi/MIUI: en
            // Android 15+ el sistema fuerza edge-to-edge y el contenido se
            // dibuja DETRÁS de la barra de estado si la pantalla no le pone
            // su propio padding (el statusBarColor fijo del tema ya no alcanza).
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
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
                        TextButton(
                            onClick = {
                                zoomActual = (zoomActual - 0.1f).coerceAtLeast(0f)
                                controlCamara?.setLinearZoom(zoomActual)
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("-", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
                        Text("Zoom", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        TextButton(
                            onClick = {
                                zoomActual = (zoomActual + 0.1f).coerceAtMost(1f)
                                controlCamara?.setLinearZoom(zoomActual)
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
                    }
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                // Instrucción principal, debajo de los controles (nunca encima).
                Text(
                    instruccion,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    maxLines = 2
                )

                if (zoomAlto) {
                    Text(
                        "Pellizca para zoom",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            }

            // Marco guía al centro (adaptativo a pantalla)
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.85f)
                    .aspectRatio(4f / 3f)
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                ) {}
            }

            // Card con opciones detectadas (responsive, max altura en pantallas pequeñas)
            if (textosDetectados.isNotEmpty() && !yaSeleccionado) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .fillMaxWidth(0.95f)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.85f)
                    )
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            if (esCodigo) "Toca el CÓDIGO correcto:" else "Toca la SERIE correcta:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        textosDetectados.forEachIndexed { idx, linea ->
                            if (idx < 5) { // Máximo 5 opciones para no desbordar
                                TextButton(
                                    onClick = { aceptar(linea) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text(
                                        text = if (cumpleForma(linea)) "✓ $linea" else linea,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cumpleForma(linea)) Color(0xFF51CF66) else Color.White,
                                        maxLines = 1
                                    )
                                }
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

/** Recorte NV21 listo para InputImage.fromByteArray: bytes + dimensiones reales. */
private class RecorteNV21(val datos: ByteArray, val ancho: Int, val alto: Int)

/**
 * Recorta el frame a la franja central (≈92 % ancho × ≈45 % alto) donde queda el
 * marco guía, trabajando directo sobre los bytes YUV — SIN pasar por
 * comprimir a JPEG y volver a decodificar, que es lo que hacía la versión
 * anterior (`YuvImage.compressToJpeg` + `BitmapFactory.decodeByteArray` en
 * CADA frame de la cámara, ~15-20 veces por segundo). Ese round-trip por JPEG
 * saturaba la CPU y era la causa real de "no escanea nada": ML Kit se
 * quedaba sin tiempo de CPU para procesar el frame antes de que llegara el
 * siguiente. Devuelve null si la conversión falla (se usa el frame completo).
 */
private fun recortarNV21AlCentro(imageProxy: ImageProxy): RecorteNV21? {
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

        // Pares (no impares): el submuestreo de color 4:2:0 agrupa de 2 en 2,
        // tanto el tamaño del recorte como su origen deben caer en esa rejilla.
        var cw = (w * 0.92f).toInt().coerceAtMost(w)
        var ch = (h * 0.45f).toInt().coerceAtMost(h)
        cw -= cw % 2
        ch -= ch % 2
        if (cw <= 0 || ch <= 0) return null
        var cx = ((w - cw) / 2).coerceAtLeast(0)
        var cy = ((h - ch) / 2).coerceAtLeast(0)
        cx -= cx % 2
        cy -= cy % 2

        val salida = ByteArray(cw * ch + cw * ch / 2)
        var pos = 0
        // Plano Y: una fila de cw bytes por cada fila del recorte.
        for (fila in 0 until ch) {
            System.arraycopy(nv21, (cy + fila) * w + cx, salida, pos, cw)
            pos += cw
        }
        // Plano VU (submuestreado 2x2, intercalado V,U): misma lógica a la mitad de escala.
        val inicioVU = w * h
        for (fila in 0 until ch / 2) {
            System.arraycopy(nv21, inicioVU + (cy / 2 + fila) * w + cx, salida, pos, cw)
            pos += cw
        }
        RecorteNV21(salida, cw, ch)
    } catch (e: Exception) {
        null
    }
}
