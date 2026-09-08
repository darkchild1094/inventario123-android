package com.kernel94.inventario123.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
) {
    val context = LocalContext.current
    LaunchedEffect(tiendaFijaId) { viewModel.iniciar(tiendaFijaId) }
    LaunchedEffect(serieEscaneada) {
        if (!serieEscaneada.isNullOrBlank()) { viewModel.onSerieChange(serieEscaneada); onSerieConsumida() }
    }
    LaunchedEffect(codigoEscaneado) {
        if (!codigoEscaneado.isNullOrBlank()) { viewModel.codigoBarras = codigoEscaneado; onCodigoConsumido() }
    }
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
                title = { Text("Movimiento en tienda", color = Color.White) },
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
                    onSeleccion = { viewModel.tiendaId = it }, etiquetaNula = "Selecciona tienda...",
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

            OutlinedTextField(
                value = viewModel.serie, onValueChange = viewModel::onSerieChange,
                label = { Text(if (viewModel.modo == ModoMov.RETIRO) "Serie del equipo a retirar" else "Serie") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { IconButton(onClick = onAbrirEscanerSerie) { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear") } },
            )
            OutlinedTextField(
                value = viewModel.codigoBarras, onValueChange = { viewModel.codigoBarras = it },
                label = { Text("Código de barras") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { IconButton(onClick = onAbrirEscanerCodigo) { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear") } },
            )

            // Hint del lookup
            viewModel.lookup?.let { lk ->
                val (txt, color) = when {
                    viewModel.modo == ModoMov.RETIRO && lk.encontrado && lk.en_esta_tienda -> "Instalado aquí — se retirará a tu stock." to Color(0xFF198754)
                    viewModel.modo == ModoMov.RETIRO -> "Esa serie no está instalada en esta tienda." to Color(0xFFDC3545)
                    lk.encontrado && lk.en_mi_stock -> "Está en tu stock — se moverá ese equipo a la tienda." to Color(0xFF198754)
                    lk.encontrado -> "Serie ya existe en otra ubicación (${lk.ubicacion_corta ?: ""})." to Color.Gray
                    else -> "Serie nueva — captura dispositivo y modelo." to Color.Gray
                }
                Text(txt, color = color, style = MaterialTheme.typography.bodySmall)
            }

            // Alta nueva: dispositivo + modelo (instalación/reemplazo cuando no está en mi stock)
            if (viewModel.modo != ModoMov.RETIRO && viewModel.necesitaAltaNueva) {
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

            // Reemplazo: equipo que sale
            if (viewModel.modo == ModoMov.REEMPLAZO) {
                HorizontalDivider()
                Text("Equipo que se retira (pasa a tu stock)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                OutlinedTextField(
                    value = viewModel.salidaSerie, onValueChange = viewModel::onSalidaSerieChange,
                    label = { Text("Serie del que sale") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = viewModel.salidaCodigoBarras, onValueChange = { viewModel.salidaCodigoBarras = it },
                    label = { Text("CB del que sale") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                viewModel.lookupSalida?.let { lk ->
                    val ok = lk.encontrado && lk.en_esta_tienda
                    Text(
                        if (ok) "Instalado aquí — se retirará a tu stock." else "El equipo que sale no está instalado en esta tienda.",
                        color = if (ok) Color(0xFF198754) else Color(0xFFDC3545),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.motivo, onValueChange = { viewModel.motivo = it },
                label = { Text("Razón / motivo") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            FotoActivoCampo(
                etiqueta = "Foto del equipo", urlActual = null,
                uriSeleccionada = viewModel.fotoEquipoUri, onCambio = { viewModel.fotoEquipoUri = it },
                modifier = Modifier.fillMaxWidth(),
            )

            if (!online) {
                Text("Sin conexión: la instalación de equipo nuevo y el reemplazo se guardan y se envían al recuperar señal. El retiro requiere conexión.",
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
