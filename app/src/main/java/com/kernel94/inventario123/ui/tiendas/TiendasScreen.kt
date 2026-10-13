package com.kernel94.inventario123.ui.tiendas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kernel94.inventario123.data.repository.Resultado
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
    val context = LocalContext.current
    var confirmarEliminar by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    fun exportarYCompartir() {
        viewModel.exportar(context) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    val archivo = resultado.datos
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Compartir tiendas"))
                }
                is Resultado.Error -> scope.launch { snackbarHostState.showSnackbar(resultado.mensaje) }
            }
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
                    if (!viewModel.modoSeleccion && viewModel.perfil?.permisos?.puedeExportar == true) {
                        IconButton(onClick = { if (!viewModel.exportando) exportarYCompartir() }) {
                            if (viewModel.exportando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            else Icon(Icons.Filled.FileDownload, contentDescription = "Exportar", tint = Color.White)
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
        Column(Modifier.padding(padding).fillMaxSize().background(com.kernel94.inventario123.ui.theme.BsCanvas)) {
            if (viewModel.plazas.size > 1) {
                com.kernel94.inventario123.ui.common.PlazaTabs(
                    opciones = viewModel.plazas,
                    seleccionId = viewModel.plazaId,
                    onSeleccion = { viewModel.onPlazaChange(it) },
                    modifier = Modifier.fillMaxWidth().background(Color.White),
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                color = Color(0xFFECE6F0),
                shape = RoundedCornerShape(50),
            ) {
                OutlinedTextField(
                    value = viewModel.busqueda,
                    onValueChange = { viewModel.onBusquedaChange(it) },
                    placeholder = { Text("Buscar por nombre o CR") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF49454F)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!viewModel.cargando) {
                Text(
                    "${viewModel.tiendas.size} tienda(s)", style = MaterialTheme.typography.labelMedium, color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

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
                            Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (viewModel.modoSeleccion) {
                                    Checkbox(checked = marcada, onCheckedChange = { viewModel.alternarSeleccion(t.id) })
                                } else {
                                    Box(
                                        Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(Color(0xFFDCE9FF)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = BsPrimary)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(t.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        listOfNotNull(t.cr_tienda?.let { "CR $it" }, t.plaza_nombre).joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                                    )
                                    Text(
                                        "ATI: ${t.ati_nombre ?: "sin asignar"} · ${t.activos_count} activos",
                                        style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                                    )
                                    if (t.ultimo_inv_fecha == null) {
                                        Text("Sin inventario registrado", style = MaterialTheme.typography.labelSmall, color = com.kernel94.inventario123.ui.theme.BsWarning)
                                    }
                                }
                                if (!viewModel.modoSeleccion) {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
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
