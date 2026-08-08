package com.kernel94.inventario123.ui.listado

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.ui.listado.components.ActivoCard
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsDark
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
    var activoAEliminar by remember { mutableStateOf<Int?>(null) }
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
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = viewModel.perfil?.usuario?.foto?.let { 
                                "https://fieldserviceplus.alwaysdata.net/inventario123/uploads/usuarios/$it" 
                            } ?: "file:///android_asset/logo_login.png",
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Gray.copy(alpha = 0.2f))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = viewModel.perfil?.usuario?.nombre ?: "Usuario",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = viewModel.perfil?.usuario?.plaza_nombre ?: "Inventario123",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
                actions = {
                    if (viewModel.perfil?.permisos?.puedeExportar == true) {
                        IconButton(onClick = { exportarYCompartir() }, enabled = !viewModel.exportando) {
                            if (viewModel.exportando) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.FileDownload, contentDescription = "Exportar", tint = Color.White)
                            }
                        }
                    }
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(Icons.Filled.FilterAlt, contentDescription = "Filtros", tint = Color.White)
                    }
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewModel.perfil?.permisos?.puedeCrearActivo == true) {
                FloatingActionButton(onClick = onCrearNuevo, containerColor = BsPrimary) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F3F5))) {

            if (vistasDisponibles.size > 1) {
                TabRow(
                    selectedTabIndex = vistasDisponibles.indexOf(viewModel.vistaActual).coerceAtLeast(0),
                    containerColor = BsDark,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        val index = vistasDisponibles.indexOf(viewModel.vistaActual).coerceAtLeast(0)
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[index]),
                            color = BsPrimary
                        )
                    }
                ) {
                    vistasDisponibles.forEach { vista ->
                        Tab(
                            selected = viewModel.vistaActual == vista,
                            onClick = { viewModel.cambiarVista(vista) },
                            text = { Text(etiquetaVista(vista), color = Color.White) }
                        )
                    }
                }
            }

            // Barra de búsqueda estilo backend
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.busqueda,
                    onValueChange = { viewModel.onBusquedaChange(it) },
                    placeholder = { Text("Serie, placa, modelo...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BsPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFFF8F9FA),
                        unfocusedContainerColor = Color(0xFFF8F9FA)
                    )
                )
            }

            if (mostrarFiltros && viewModel.perfil?.permisos?.puedeFiltrarPorPlaza == true) {
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                FiltroDropdown(
                                    etiqueta = "Negocio", opciones = viewModel.catalogos.negocios,
                                    seleccionId = viewModel.negocioId, idDe = { it.id }, nombreDe = { it.nombre },
                                    onSeleccion = { viewModel.negocioId = it; viewModel.onFiltroChange() }
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                FiltroDropdown(
                                    etiqueta = "Plaza", opciones = viewModel.catalogos.plazas,
                                    seleccionId = viewModel.plazaId, idDe = { it.id }, nombreDe = { it.nombre },
                                    onSeleccion = { viewModel.plazaId = it; viewModel.onFiltroChange() }
                                )
                            }
                        }
                        TextButton(
                            onClick = { viewModel.limpiarFiltros() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Limpiar filtros")
                        }
                    }
                }
            }

            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                viewModel.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(viewModel.error!!, color = Color.Gray) }
                viewModel.activos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay activos para mostrar", color = Color.Gray) }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(viewModel.activos) { activo ->
                        ActivoCard(
                            activo = activo,
                            onClick = { onAbrirDetalle(activo.id) },
                            onEditar = { onEditar(activo.id) },
                            onEliminar = { activoAEliminar = activo.id },
                        )
                    }
                }
            }
        }

        if (activoAEliminar != null) {
            AlertDialog(
                onDismissRequest = { activoAEliminar = null },
                title = { Text("¿Eliminar activo?") },
                text = { Text("Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = activoAEliminar!!
                            activoAEliminar = null
                            viewModel.eliminar(id) { ok, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activoAEliminar = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

private fun etiquetaVista(vista: String): String = when (vista) {
    "bodega" -> "Bodega"; "mi_stock" -> "Mi Stock"; "todos" -> "Todos"; else -> vista.replaceFirstChar { it.uppercase() }
}
