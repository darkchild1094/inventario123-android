package com.kernel94.inventario123.ui.tiendas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TiendasScreen(
    viewModel: TiendasViewModel,
    onVolver: () -> Unit,
    onAbrirTienda: (Int) -> Unit = {},
    onNuevaTienda: () -> Unit = {},
) {
    LaunchedEffect(Unit) { viewModel.iniciar() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmarEliminar by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (viewModel.modoSeleccion) Text("${viewModel.seleccion.size} seleccionada(s)")
                    else Text("Tiendas")
                },
                navigationIcon = {
                    IconButton(onClick = { if (viewModel.modoSeleccion) viewModel.salirDeSeleccion() else onVolver() }) {
                        Icon(if (viewModel.modoSeleccion) Icons.Filled.Close else Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    if (viewModel.modoSeleccion && viewModel.puedeAsignarAti) {
                        IconButton(onClick = { confirmarEliminar = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White),
            )
        },
        floatingActionButton = {
            if (!viewModel.modoSeleccion && viewModel.puedeAsignarAti) {
                FloatingActionButton(onClick = onNuevaTienda, containerColor = BsPrimary) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva tienda", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Entra a una tienda para ver y administrar sus activos." +
                    if (viewModel.puedeAsignarAti) " Puedes asignar el ATI responsable de cada tienda." else "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            FiltroDropdown(
                etiqueta = "Plaza", opciones = viewModel.plazas,
                seleccionId = viewModel.plazaId, idDe = { it.id }, nombreDe = { it.nombre },
                onSeleccion = { viewModel.onPlazaChange(it) },
                etiquetaNula = "Todas mis plazas",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = viewModel.busqueda,
                onValueChange = { viewModel.onBusquedaChange(it) },
                label = { Text("Nombre o CR de tienda") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )

            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                viewModel.tiendas.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin tiendas para el filtro.", color = Color.Gray) }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(viewModel.tiendas, key = { it.id }) { t ->
                        val marcada = t.id in viewModel.seleccion
                        Card(
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { if (viewModel.modoSeleccion) viewModel.alternarSeleccion(t.id) else onAbrirTienda(t.id) },
                                onLongClick = { if (viewModel.puedeAsignarAti) viewModel.activarSeleccion(t.id) },
                            ),
                            colors = CardDefaults.cardColors(containerColor = if (marcada) Color(0xFFDCE9FF) else Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.modoSeleccion) {
                                        Checkbox(checked = marcada, onCheckedChange = { viewModel.alternarSeleccion(t.id) })
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(t.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            listOfNotNull(
                                                t.cr_tienda?.let { "CR $it" },
                                                t.plaza_nombre,
                                            ).joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                                        )
                                        if (t.ultimo_inv_fecha != null) {
                                            Text(
                                                "Último inventario: ${t.ultimo_inv_fecha} · ${t.ultimo_inv_encontrados ?: 0} encontrados" +
                                                    (t.ultimo_inv_no_encontrados?.takeIf { it > 0 }?.let { " · $it no encontrados" } ?: ""),
                                                style = MaterialTheme.typography.labelSmall, color = Color(0xFF6C757D),
                                            )
                                        } else {
                                            Text("Sin inventario registrado", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB08800))
                                        }
                                    }
                                    AssistChip(onClick = { onAbrirTienda(t.id) }, label = { Text("${t.activos_count} activos") })
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                                if (viewModel.puedeAsignarAti && !viewModel.modoSeleccion) {
                                    Spacer(Modifier.height(6.dp))
                                    FiltroDropdown(
                                        etiqueta = "ATI responsable", opciones = viewModel.atis,
                                        seleccionId = t.ati_usuario_id, idDe = { it.id }, nombreDe = { it.nombre },
                                        onSeleccion = { viewModel.asignarAti(t.id, it) },
                                        etiquetaNula = "— Sin asignar —",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar ${viewModel.seleccion.size} tienda(s)?") },
            text = { Text("Las que tengan activos instalados no se podrán eliminar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminar = false
                        viewModel.eliminarSeleccion { ok, total ->
                            scope.launch { snackbarHostState.showSnackbar("$ok de $total tienda(s) eliminada(s).") }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } },
        )
    }
}
