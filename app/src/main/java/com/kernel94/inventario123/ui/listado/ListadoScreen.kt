package com.kernel94.inventario123.ui.listado

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.ui.listado.components.ActivoCard
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    viewModel: ListadoViewModel,
    onAbrirDetalle: (Int) -> Unit,
    onEditar: (Int) -> Unit,
    onCrearNuevo: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.iniciar() }
    var mostrarFiltros by remember { mutableStateOf(false) }
    val vistasDisponibles = viewModel.perfil?.vistasDisponibles ?: listOf("todos")
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun exportarYCompartir() {
        viewModel.exportar(context) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    val archivo = resultado.datos
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir inventario"))
                }
                is Resultado.Error -> {
                    scope.launch { snackbarHostState.showSnackbar(resultado.mensaje) }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Inventario123") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = androidx.compose.ui.graphics.Color.White),
                actions = {
                    if (viewModel.perfil?.permisos?.puedeExportar == true) {
                        IconButton(onClick = { exportarYCompartir() }, enabled = !viewModel.exportando) {
                            if (viewModel.exportando) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.FileDownload, contentDescription = "Exportar a Excel", tint = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtros", tint = androidx.compose.ui.graphics.Color.White)
                    }
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.perfil?.permisos?.puedeCrearActivo == true) {
                FloatingActionButton(onClick = onCrearNuevo, containerColor = BsPrimary) {
                    Icon(Icons.Filled.Add, contentDescription = "Registrar activo", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            if (vistasDisponibles.size > 1) {
                TabRow(selectedTabIndex = vistasDisponibles.indexOf(viewModel.vistaActual).coerceAtLeast(0)) {
                    vistasDisponibles.forEach { vista ->
                        Tab(
                            selected = viewModel.vistaActual == vista,
                            onClick = { viewModel.cambiarVista(vista) },
                            text = { Text(etiquetaVista(vista)) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.busqueda,
                onValueChange = { viewModel.onBusquedaChange(it) },
                placeholder = { Text("Serie, placa, modelo, negocio, región, plaza o usuario...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )

            if (mostrarFiltros && viewModel.perfil?.permisos?.puedeFiltrarPorPlaza == true) {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    FiltroDropdown(
                        etiqueta = "Negocio", opciones = viewModel.catalogos.negocios,
                        seleccionId = viewModel.negocioId, idDe = { it.id }, nombreDe = { it.nombre },
                        onSeleccion = { viewModel.negocioId = it; viewModel.onFiltroChange() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    FiltroDropdown(
                        etiqueta = "Región", opciones = viewModel.catalogos.regiones.filter { viewModel.negocioId == null || it.negocio_id == viewModel.negocioId },
                        seleccionId = viewModel.regionId, idDe = { it.id }, nombreDe = { it.nombre },
                        onSeleccion = { viewModel.regionId = it; viewModel.onFiltroChange() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    FiltroDropdown(
                        etiqueta = "Plaza", opciones = viewModel.catalogos.plazas.filter { (viewModel.negocioId == null || it.negocio_id == viewModel.negocioId) && (viewModel.regionId == null || it.region_id == viewModel.regionId) },
                        seleccionId = viewModel.plazaId, idDe = { it.id }, nombreDe = { it.nombre },
                        onSeleccion = { viewModel.plazaId = it; viewModel.onFiltroChange() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    FiltroDropdown(
                        etiqueta = "Usuario", opciones = viewModel.catalogos.usuarios.filter { viewModel.plazaId == null || it.plaza_id == viewModel.plazaId },
                        seleccionId = viewModel.usuarioId, idDe = { it.id }, nombreDe = { it.nombre },
                        onSeleccion = { viewModel.usuarioId = it; viewModel.onFiltroChange() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    TextButton(onClick = { viewModel.limpiarFiltros() }) { Text("Limpiar filtros") }
                }
            }

            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                viewModel.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(viewModel.error!!) }
                viewModel.activos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No se encontraron activos.") }
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(viewModel.activos) { activo ->
                        ActivoCard(
                            activo = activo,
                            onClick = { onAbrirDetalle(activo.id) },
                            onEditar = { onEditar(activo.id) },
                            onEliminar = { /* confirmación se maneja en detalle */ onAbrirDetalle(activo.id) },
                        )
                    }
                    if (viewModel.totalPaginas > 1) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                TextButton(enabled = viewModel.paginaActual > 1, onClick = { viewModel.cargar(viewModel.paginaActual - 1) }) { Text("Anterior") }
                                Text("Página ${viewModel.paginaActual} de ${viewModel.totalPaginas}", modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 8.dp))
                                TextButton(enabled = viewModel.paginaActual < viewModel.totalPaginas, onClick = { viewModel.cargar(viewModel.paginaActual + 1) }) { Text("Siguiente") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun etiquetaVista(vista: String): String = when (vista) {
    "bodega" -> "Bodega"; "mi_stock" -> "Mi Stock"; "todos" -> "Todos"; else -> vista
}
