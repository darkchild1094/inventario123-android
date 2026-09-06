package com.kernel94.inventario123.ui.solicitudes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kernel94.inventario123.data.remote.Urls
import com.kernel94.inventario123.ui.firma.FirmaCanvas
import com.kernel94.inventario123.ui.firma.rememberFirmaState
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudDetalleScreen(
    viewModel: SolicitudesViewModel,
    solicitudId: Int,
    onVolver: () -> Unit,
) {
    LaunchedEffect(solicitudId) { viewModel.cargarDetalle(solicitudId) }
    val s = viewModel.detalle
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val firma = rememberFirmaState()
    var motivo by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Solicitud #$solicitudId") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        }
    ) { padding ->
        when {
            viewModel.cargando || s == null && viewModel.error == null ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            viewModel.error != null && s == null ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(viewModel.error!!) }
            s != null -> Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Estado: ", fontWeight = FontWeight.Bold)
                    EstadoBadge(s.estado)
                }
                Campo("Movimiento", s.destinoLabel)
                Campo("Origen", s.origenLabel)
                if (s.destino == "en_bodega") Campo("Bodega destino", s.bodega_nombre)
                if (s.destino == "asignado") Campo("Recibe", s.destino_usuario_nombre)
                Campo("Plaza", s.plaza_nombre)
                Campo("Solicitante", s.solicitante_nombre)
                Campo("Creada", s.creado_en?.take(16))
                s.nota?.takeIf { it.isNotBlank() }?.let { Campo("Motivo", it) }
                if (s.estado == "rechazada") s.motivo_rechazo?.let { Campo("Motivo del rechazo", it) }

                Text("Activos (${s.activos.size})", fontWeight = FontWeight.Bold)
                s.activos.forEach { a ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                listOfNotNull(a.marca_nombre, a.modelo_nombre).joinToString(" ").ifBlank { "Sin modelo" },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${a.dispositivo_nombre ?: "—"} · Serie: ${a.serie ?: "ninguna"} · Cód: ${a.codigo_barras ?: "—"} · ${a.status ?: "—"}",
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray
                            )
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FirmaMini("Solicitante", s.firma_solicitante, Modifier.weight(1f))
                    FirmaMini(if (s.dobleFirma) "ATI" else "Aprobador", s.firma_aprobador, Modifier.weight(1f))
                    if (s.dobleFirma) FirmaMini("Coordinador", s.firma_aprobador2, Modifier.weight(1f))
                }

                if (s.puedeFirmar && s.estado == "pendiente") {
                    HorizontalDivider()
                    Text("Firmar (requiere tu firma)", fontWeight = FontWeight.Bold)
                    FirmaCanvas(firma, Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            val png = firma.exportarPng()
                            if (png == null) { scope.launch { snackbar.showSnackbar("Falta tu firma para aprobar.") }; return@Button }
                            viewModel.aprobar(solicitudId, png) { ok, msg -> scope.launch { snackbar.showSnackbar(msg) } }
                        },
                        enabled = !viewModel.enviando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF198754)),
                    ) { Text(if (viewModel.enviando) "Procesando…" else "Firmar") }

                    OutlinedTextField(
                        value = motivo, onValueChange = { motivo = it },
                        label = { Text("Motivo del rechazo") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.rechazar(solicitudId, motivo) { ok, msg -> scope.launch { snackbar.showSnackbar(msg) } }
                        },
                        enabled = !viewModel.enviando,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Rechazar") }
                }

                if (s.puedeCancelar && s.estado == "pendiente") {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = { viewModel.cancelar(solicitudId) { ok, msg -> scope.launch { snackbar.showSnackbar(msg) } } },
                        enabled = !viewModel.enviando,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancelar solicitud") }
                }
            }
        }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String?) {
    if (valor.isNullOrBlank()) return
    Row {
        Text("$etiqueta: ", fontWeight = FontWeight.SemiBold)
        Text(valor)
    }
}

@Composable
private fun FirmaMini(etiqueta: String, archivo: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        if (archivo != null) {
            AsyncImage(model = Urls.firma(archivo), contentDescription = null, modifier = Modifier.fillMaxWidth().height(80.dp))
        } else {
            Text("Pendiente", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
