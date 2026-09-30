package com.kernel94.inventario123.ui.rentec

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kernel94.inventario123.data.model.RentecInstalado
import com.kernel94.inventario123.data.model.RentecRecibido
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentecDetalleScreen(
    viewModel: RentecDetalleViewModel,
    proyectoId: Int,
    onVolver: () -> Unit,
    onRecibirEquipo: (Int, String) -> Unit,
    onInstalarEquipo: (Int, String) -> Unit,
) {
    LaunchedEffect(proyectoId) { viewModel.cargar(proyectoId) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmarCierre by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    val p = viewModel.proyecto

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(p?.folio ?: "RENTEC") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                actions = {
                    IconButton(onClick = {
                        if (!viewModel.exportando) {
                            viewModel.exportar(context) { archivo ->
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir RENTEC"))
                            }
                        }
                    }) {
                        if (viewModel.exportando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(Icons.Filled.FileDownload, contentDescription = "Exportar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        when {
            viewModel.cargando && p == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
            p == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { Text("No se pudo cargar el proyecto.", color = Color.Gray) }
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                item {
                    Column {
                        Text(p.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${p.recibidos} en bodega · ${p.instalados} instalados" +
                                (p.usuario_nombre?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                        )
                    }
                }
                if (p.abierto) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onRecibirEquipo(p.id, p.folio) },
                                colors = ButtonDefaults.buttonColors(containerColor = BsPrimary),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp)); Text("Recibir equipo")
                            }
                            Button(
                                onClick = { onInstalarEquipo(p.id, p.folio) },
                                colors = ButtonDefaults.buttonColors(containerColor = BsPrimary),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Store, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp)); Text("Instalar")
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { confirmarCierre = true }) { Text("Cerrar proyecto") }
                        }
                    }
                }

                if (p.detalle_recibidos.isNotEmpty()) {
                    item { SeccionTitulo("En bodega, pendientes de instalar (${p.detalle_recibidos.size})") }
                    items(p.detalle_recibidos, key = { "rec-${it.activo_id}" }) { RecibidoRow(it) }
                }

                if (p.detalle_instalados.isNotEmpty()) {
                    item { SeccionTitulo("Instalados (${p.detalle_instalados.size})") }
                    items(p.detalle_instalados, key = { "ins-${it.movimiento_id}" }) { InstaladoRow(it) }
                }

                if (p.detalle_recibidos.isEmpty() && p.detalle_instalados.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text("Aún no hay activos en este proyecto.", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("¿Cerrar proyecto?") },
            text = { Text("Ya no se podrá seguir usando este folio para recibir o instalar equipo nuevo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarCierre = false
                    viewModel.cerrar { scope.launch { snackbarHostState.showSnackbar("Proyecto cerrado.") } }
                }) { Text("Cerrar proyecto") }
            },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SeccionTitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = BsDark)
}

@Composable
private fun RecibidoRow(r: RentecRecibido) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = Color(0xFF6C757D))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    listOfNotNull(r.dispositivo_nombre, r.marca_nombre, r.modelo_nombre).joinToString(" "),
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    listOfNotNull(r.serie?.let { "Serie $it" }, r.codigo_barras?.let { "CB $it" }).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun InstaladoRow(i: RentecInstalado) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Color(0xFF198754))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    listOfNotNull(i.dispositivo_nombre, i.marca_nombre, i.modelo_nombre).joinToString(" "),
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    listOfNotNull(
                        listOfNotNull(i.cr_tienda, i.tienda_nombre).joinToString(" ").ifBlank { null },
                        i.serie_entra?.let { "Serie $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                )
                if (i.serie_sale != null) {
                    Text("Reemplazó a serie ${i.serie_sale}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF842029))
                }
            }
        }
    }
}
