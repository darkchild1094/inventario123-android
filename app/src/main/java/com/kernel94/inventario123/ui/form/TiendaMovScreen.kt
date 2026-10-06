package com.kernel94.inventario123.ui.form

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiendaMovScreen(
    viewModel: TiendaMovViewModel,
    tiendaFijaId: Int?,
    onVolver: () -> Unit,
    onAbrirEscanerSerie: () -> Unit,
    onAbrirEscanerCodigo: () -> Unit,
    serieEscaneada: String?,
    codigoEscaneado: String?,
    onSerieConsumida: () -> Unit,
    onCodigoConsumido: () -> Unit,
    proyectoRentecContexto: Int? = null,
    proyectoRentecFolioContexto: String? = null,
) {
    val context = LocalContext.current
    // Qué campo se está escaneando: "serie" | "codigo" | "salida_serie" | "salida_codigo"
    var objetivoEscaneo by remember { mutableStateOf("serie") }
    LaunchedEffect(tiendaFijaId) { viewModel.iniciar(tiendaFijaId, proyectoRentecContexto, proyectoRentecFolioContexto) }
    LaunchedEffect(serieEscaneada) {
        if (!serieEscaneada.isNullOrBlank()) {
            if (objetivoEscaneo == "salida_serie") viewModel.onSalidaSerieChange(serieEscaneada)
            else viewModel.onSerieChange(serieEscaneada)
            onSerieConsumida()
        }
    }
    LaunchedEffect(codigoEscaneado) {
        if (!codigoEscaneado.isNullOrBlank()) {
            if (objetivoEscaneo == "salida_codigo") viewModel.salidaCodigoBarras = codigoEscaneado
            else viewModel.codigoBarras = codigoEscaneado
            onCodigoConsumido()
        }
    }
    val abrirSerie = { obj: String -> objetivoEscaneo = obj; onAbrirEscanerSerie() }
    val abrirCodigo = { obj: String -> objetivoEscaneo = obj; onAbrirEscanerCodigo() }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let { snackbar.showSnackbar(it); viewModel.limpiarMensaje() }
    }

    val online by viewModel.online.collectAsState()
    val tiendaNombre = viewModel.tiendas.firstOrNull { it.id == viewModel.tiendaId }?.nombre

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (proyectoRentecContexto != null) "Instalar — ${proyectoRentecFolioContexto ?: "RENTEC"}" else "Movimiento en tienda",
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        if (viewModel.cargando) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F3F5))
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (viewModel.tiendaFija) {
                Text("Tienda: ${tiendaNombre ?: "—"}", fontWeight = FontWeight.Bold)
            } else {
                FiltroDropdown(
                    etiqueta = "Tienda", opciones = viewModel.tiendas,
                    seleccionId = viewModel.tiendaId, idDe = { it.id },
                    nombreDe = { (it.cr_tienda?.let { c -> "$c · " } ?: "") + it.nombre },
                    onSeleccion = { viewModel.onTiendaChange(it) }, etiquetaNula = "Selecciona tienda...",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Modo
            val modos = listOf(ModoMov.INSTALACION to "Instalación", ModoMov.RETIRO to "Retiro", ModoMov.REEMPLAZO to "Reemplazo")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modos.forEachIndexed { i, (m, lbl) ->
                    SegmentedButton(
                        selected = viewModel.modo == m,
                        onClick = { viewModel.onModoChange(m) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = modos.size),
                    ) { Text(lbl) }
                }
            }

            // RENTEC: elegir de lo ya recibido en bodega en vez de escribirlo —
            // sigue siendo editable después (los campos de abajo no se bloquean).
            if (proyectoRentecContexto != null && viewModel.activosRecibidosRentec.isNotEmpty()) {
                FiltroDropdown(
                    etiqueta = "Equipo recibido", opciones = viewModel.activosRecibidosRentec,
                    seleccionId = null, idDe = { it.activo_id },
                    nombreDe = { listOfNotNull(it.dispositivo_nombre, it.marca_nombre, it.modelo_nombre, it.serie).joinToString(" · ") },
                    onSeleccion = { id -> viewModel.activosRecibidosRentec.find { it.activo_id == id }?.let(viewModel::onSeleccionarRecibido) },
                    etiquetaNula = "Elige de lo recibido, o escribe abajo",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Sección del equipo que ENTRA ─────────────────────────────────
            // En modo Retiro no entra nada: la única sección es la del que sale,
            // así que esta cambia de título y recoge los datos del que se retira.
            val esRetiro = viewModel.modo == ModoMov.RETIRO
            SeccionEquipo(
                titulo = if (esRetiro) "Equipo que se RETIRA" else "Equipo que se INSTALA",
                subtitulo = if (esRetiro) "Sale de la tienda y pasa a tu stock."
                            else "Queda instalado y funcionando en la tienda.",
                acento = if (esRetiro) VERDE_RETIRO else AZUL_INSTALA,
            ) {
                OutlinedTextField(
                    value = viewModel.serie, onValueChange = viewModel::onSerieChange,
                    label = { Text(if (esRetiro) "Serie del equipo a retirar" else "Serie del equipo a instalar") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { abrirSerie("serie") }) { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear serie") } },
                )
                OutlinedTextField(
                    value = viewModel.codigoBarras, onValueChange = { viewModel.codigoBarras = it.filter(Char::isDigit).take(8) },
                    label = { Text("Código de barras (8 dígitos)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = viewModel.codigoBarras.isNotBlank() && viewModel.codigoBarras.length != 8,
                    supportingText = { if (viewModel.codigoBarras.isNotBlank() && viewModel.codigoBarras.length != 8) Text("Deben ser 8 dígitos numéricos") },
                    trailingIcon = { IconButton(onClick = { abrirCodigo("codigo") }) { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código") } },
                )

                viewModel.lookup?.let { lk ->
                    val (txt, color) = when {
                        esRetiro && lk.encontrado && lk.en_esta_tienda -> "Instalado aquí — se retirará a tu stock." to VERDE_RETIRO
                        esRetiro -> "Esa serie no está instalada en esta tienda." to ROJO_AVISO
                        lk.encontrado && lk.en_mi_stock -> "Está en tu stock — se moverá ese equipo a la tienda." to VERDE_RETIRO
                        lk.encontrado -> "Serie ya existe en otra ubicación (${lk.ubicacion_corta ?: ""})." to Color.Gray
                        else -> "Serie nueva — captura dispositivo y modelo." to Color.Gray
                    }
                    Text(txt, color = color, style = MaterialTheme.typography.bodySmall)
                    PlacaActivoFijo(lk.activo?.numActivo)
                }

                // Alta nueva: dispositivo + modelo (sólo si la serie no existe ya).
                if (!esRetiro && viewModel.necesitaAltaNueva) {
                    FiltroDropdown(
                        etiqueta = "Dispositivo", opciones = viewModel.catalogos.dispositivos,
                        seleccionId = viewModel.dispositivoId, idDe = { it.id }, nombreDe = { it.nombre },
                        onSeleccion = { viewModel.dispositivoId = it; viewModel.modeloId = null }, etiquetaNula = "—",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FiltroDropdown(
                        etiqueta = "Modelo", opciones = viewModel.modelosFiltrados,
                        seleccionId = viewModel.modeloId, idDe = { it.id },
                        nombreDe = { (it.marca_nombre?.let { m -> "$m " } ?: "") + it.nombre },
                        onSeleccion = { viewModel.modeloId = it }, etiquetaNula = "—",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                FotosDelEquipo(
                    equipoUri = viewModel.fotoEquipoUri, onEquipo = { viewModel.fotoEquipoUri = it },
                    serieUri = viewModel.fotoSerieUri, onSerie = { viewModel.fotoSerieUri = it },
                    codigoUri = viewModel.fotoActivoUri, onCodigo = { viewModel.fotoActivoUri = it },
                )
            }

            // ── Sección del equipo que SALE (sólo en reemplazo) ───────────────
            if (viewModel.modo == ModoMov.REEMPLAZO) {
                SeccionEquipo(
                    titulo = "Equipo que se RETIRA",
                    subtitulo = "El viejo: sale de la tienda y pasa a tu stock.",
                    acento = VERDE_RETIRO,
                ) {
                    if (viewModel.activosEnTiendaSalida.isNotEmpty()) {
                        FiltroDropdown(
                            etiqueta = "Elígelo de los que hay en la tienda", opciones = viewModel.activosEnTiendaSalida,
                            seleccionId = null, idDe = { it.id },
                            nombreDe = { listOfNotNull(it.dispositivo_nombre, it.marca_nombre, it.modelo_nombre, it.serie).joinToString(" · ") },
                            onSeleccion = { id -> viewModel.activosEnTiendaSalida.find { it.id == id }?.let(viewModel::onSeleccionarSalida) },
                            etiquetaNula = "Elige de la lista, o escanea abajo",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "Si no aparece en la lista, escanea su serie o su código abajo y se identifica solo.",
                            style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                        )
                    }
                    OutlinedTextField(
                        value = viewModel.salidaSerie, onValueChange = viewModel::onSalidaSerieChange,
                        label = { Text("Serie del equipo que sale") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { abrirSerie("salida_serie") }) { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear serie del que sale") } },
                    )
                    OutlinedTextField(
                        value = viewModel.salidaCodigoBarras, onValueChange = { viewModel.salidaCodigoBarras = it.filter(Char::isDigit).take(8) },
                        label = { Text("Código de barras del que sale (8 dígitos)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        isError = viewModel.salidaCodigoBarras.isNotBlank() && viewModel.salidaCodigoBarras.length != 8,
                        trailingIcon = { IconButton(onClick = { abrirCodigo("salida_codigo") }) { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código del que sale") } },
                    )
                    viewModel.lookupSalida?.let { lk ->
                        val ok = lk.encontrado && lk.en_esta_tienda
                        Text(
                            if (ok) "Identificado — se retirará a tu stock." else "El equipo que sale no está instalado en esta tienda.",
                            color = if (ok) VERDE_RETIRO else ROJO_AVISO,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        PlacaActivoFijo(lk.activo?.numActivo ?: viewModel.salidaNumActivo)
                    }

                    FotosDelEquipo(
                        equipoUri = viewModel.fotoSalidaUri, onEquipo = { viewModel.fotoSalidaUri = it },
                        serieUri = viewModel.fotoSalidaSerieUri, onSerie = { viewModel.fotoSalidaSerieUri = it },
                        codigoUri = viewModel.fotoSalidaActivoUri, onCodigo = { viewModel.fotoSalidaActivoUri = it },
                    )
                }
            }

            MotivoDropdown(
                seleccion = viewModel.motivo,
                onSeleccion = { viewModel.motivo = it },
                modifier = Modifier.fillMaxWidth(),
            )

            if (!online) {
                Text("Sin conexión: la instalación de equipo nuevo se guarda y se envía al recuperar señal. El retiro y el reemplazo requieren conexión.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8860B))
            }

            Button(
                onClick = { viewModel.guardar(context) { onVolver() } },
                enabled = !viewModel.guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (viewModel.guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Guardar")
            }
        }
    }
}
