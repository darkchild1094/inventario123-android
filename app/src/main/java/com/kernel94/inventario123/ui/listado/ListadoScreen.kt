package com.kernel94.inventario123.ui.listado

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.ui.common.PlazaTabs
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
    modulo: String? = null,
    tiendaId: Int? = null,
    usuarioId: Int? = null,
    onAbrirHistorial: () -> Unit = {},
    onAbrirTiendas: () -> Unit = {},
    onAbrirModelos: () -> Unit = {},
    onAbrirSolicitudes: () -> Unit = {},
    onAbrirPendientes: () -> Unit = {},
    onAbrirModulo: (String) -> Unit = {},
    onAbrirConsulta: () -> Unit = {},
    onAbrirDashboard: () -> Unit = {},
    onAbrirUsuarios: () -> Unit = {},
    // Botón de auditoría física (escaneo). Bodega: solo si moduloEditable
    // (coordinador/admin). Mi Stock / Stock PFS: siempre visible — el
    // backend decide a quién puede auditar cada quien (uno mismo siempre).
    onAbrirInventario: (() -> Unit)? = null,
) {
    LaunchedEffect(modulo, tiendaId, usuarioId) { viewModel.iniciar(modulo, tiendaId, usuarioId) }
    var mostrarFiltros by remember { mutableStateOf(false) }
    var activoAEliminar by remember { mutableStateOf<Int?>(null) }
    var mostrarTransferir by remember { mutableStateOf(false) }
    val vistasDisponibles = viewModel.perfil?.vistasDisponibles ?: listOf("todos")
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val permisos = viewModel.perfil?.permisos

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
                is Resultado.Error -> scope.launch { snackbarHostState.showSnackbar(resultado.mensaje) }
            }
        }
    }

    fun cerrarYHacer(accion: () -> Unit) {
        scope.launch { drawerState.close() }
        accion()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            com.kernel94.inventario123.ui.shell.AppDrawerContent(
                perfil = viewModel.perfil,
                moduloActivo = viewModel.modulo ?: "",
                solicitudesPendientes = viewModel.solicitudesPendientes,
                onModulo = { m -> cerrarYHacer { onAbrirModulo(m) } },
                onDashboard = { cerrarYHacer(onAbrirDashboard) },
                onConsulta = { cerrarYHacer(onAbrirConsulta) },
                onHistorial = { cerrarYHacer(onAbrirHistorial) },
                onTraslados = { cerrarYHacer(onAbrirSolicitudes) },
                onPendientes = { cerrarYHacer(onAbrirPendientes) },
                onModelos = { cerrarYHacer(onAbrirModelos) },
                onUsuarios = { cerrarYHacer(onAbrirUsuarios) },
                onCerrarSesion = { cerrarYHacer(onCerrarSesion) },
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = Color.White)
                        }
                    },
                    title = {
                        val m = viewModel.modulo
                        Text(
                            if (viewModel.modoSeleccion) "${viewModel.seleccion.size} seleccionado(s)"
                            else m?.replaceFirstChar { it.uppercase() }
                                ?: (viewModel.perfil?.usuario?.plaza_nombre ?: "Inventario123"),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White,
                        )
                    },
                    actions = {
                        // En modo selección sólo tienen sentido estas dos.
                        if (viewModel.modoSeleccion) {
                            IconButton(onClick = { viewModel.seleccionarTodos() }) {
                                Icon(Icons.Filled.SelectAll, contentDescription = "Seleccionar todos", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.salirDeSeleccion() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Salir de selección", tint = Color.White)
                            }
                            return@TopAppBar
                        }
                        val puedeVerInventario = onAbrirInventario != null && when (viewModel.modulo) {
                            "bodega" -> viewModel.moduloEditable
                            "mi_stock", "stock_pfs" -> true
                            else -> false
                        }
                        if (puedeVerInventario) {
                            IconButton(onClick = onAbrirInventario!!) {
                                Icon(Icons.Filled.FactCheck, contentDescription = "Inventario", tint = Color.White)
                            }
                        }
                        IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                            Icon(Icons.Filled.FilterAlt, contentDescription = "Filtros",
                                tint = if (mostrarFiltros) BsPrimary else Color.White)
                        }
                        if (permisos?.puedeExportar == true) {
                            IconButton(onClick = { if (!viewModel.exportando) exportarYCompartir() }) {
                                if (viewModel.exportando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                else Icon(Icons.Filled.FileDownload, contentDescription = "Exportar", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
                )
            },
            floatingActionButton = {
                // En modo selección el FAB de "nuevo" estorba: lo que toca es
                // entregar lo marcado.
                if (viewModel.modoSeleccion) {
                    ExtendedFloatingActionButton(
                        onClick = { mostrarTransferir = true },
                        containerColor = BsPrimary,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Transferir ${viewModel.seleccion.size}")
                    }
                } else {
                    val puedeCrearAqui = permisos?.puedeCrearActivo == true &&
                        (viewModel.modulo == null || viewModel.moduloEditable)
                    if (puedeCrearAqui) {
                        FloatingActionButton(onClick = onCrearNuevo, containerColor = BsPrimary) {
                            Icon(Icons.Filled.Add, contentDescription = "Nuevo", tint = Color.White)
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F3F5))) {

                // Una pestaña por plaza en los módulos acotados por plaza. Sin
                // esto un coordinador con dos plazas veía las dos bodegas
                // revueltas en una sola lista.
                if (viewModel.mostrarPestanasPlaza) {
                    PlazaTabs(
                        opciones = viewModel.misPlazas,
                        seleccionId = viewModel.plazaId,
                        onSeleccion = {
                            viewModel.salirDeSeleccion()
                            viewModel.plazaId = it
                            viewModel.onFiltroChange()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (viewModel.modulo == null && vistasDisponibles.size > 1) {
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
                            val conteo = viewModel.conteosVistas[vista]
                            Tab(
                                selected = viewModel.vistaActual == vista,
                                onClick = { viewModel.cambiarVista(vista) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(etiquetaVista(vista), color = Color.White)
                                        if (conteo != null) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (viewModel.vistaActual == vista) BsPrimary else Color.White.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = conteo.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
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
                        placeholder = { Text("Serie, código, N° activo, modelo...") },
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

                if (mostrarFiltros && permisos?.puedeFiltrarPorPlaza == true) {
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

                            if (viewModel.vistaActual == "todos") {
                                Spacer(Modifier.height(8.dp))
                                FiltroDropdown(
                                    etiqueta = "Técnico / Usuario",
                                    opciones = viewModel.catalogos.usuarios,
                                    seleccionId = viewModel.usuarioId,
                                    idDe = { it.id },
                                    nombreDe = { it.nombre },
                                    onSeleccion = { viewModel.usuarioId = it; viewModel.onFiltroChange() },
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                    else -> {
                        val listState = rememberLazyListState()

                        // Paginación: cuando quedan pocas tarjetas por delante,
                        // se pide la página siguiente. El ViewModel ignora la
                        // llamada si ya está cargando o si no hay más páginas.
                        val alFinal by remember {
                            derivedStateOf {
                                val ultimoVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                ultimoVisible >= viewModel.activos.lastIndex - 5
                            }
                        }
                        LaunchedEffect(alFinal, viewModel.activos.size) {
                            if (alFinal) viewModel.cargarMas()
                        }

                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(viewModel.activos, key = { it.id }) { activo ->
                                val marcado = activo.id in viewModel.seleccion
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.modoSeleccion) {
                                        Checkbox(
                                            checked = marcado,
                                            onCheckedChange = { viewModel.alternarSeleccion(activo.id) },
                                        )
                                    }
                                    Box(Modifier.weight(1f)) {
                                        ActivoCard(
                                            activo = activo,
                                            // En modo selección, tocar la tarjeta marca
                                            // en vez de abrir el detalle.
                                            onClick = {
                                                if (viewModel.modoSeleccion) viewModel.alternarSeleccion(activo.id)
                                                else onAbrirDetalle(activo.id)
                                            },
                                            onEditar = { onEditar(activo.id) },
                                            onEliminar = { activoAEliminar = activo.id },
                                            onMantenerPresionado = if (viewModel.puedeTransferirAqui) {
                                                { viewModel.activarSeleccion(activo.id) }
                                            } else null,
                                        )
                                    }
                                }
                            }

                            // Pie: deja ver que hay más y cuánto falta, en vez de
                            // cortar la lista en silencio como hacía el tope de 5000.
                            item(key = "pie_paginacion") {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (viewModel.cargandoMas) {
                                        CircularProgressIndicator(
                                            color = BsPrimary,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp,
                                        )
                                    } else {
                                        Text(
                                            "${viewModel.activos.size} de ${viewModel.totalResultados}",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Entregar lo seleccionado a otra persona de la plaza. El equipo no
            // cambia de manos aquí: queda pendiente hasta que el otro acepta.
            if (mostrarTransferir) {
                var destino by remember { mutableStateOf<Int?>(null) }
                var nota by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { mostrarTransferir = false },
                    icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                    title = { Text("Transferir ${viewModel.seleccion.size} equipo(s)") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "El equipo cambia de manos cuando la otra persona acepte la transferencia. " +
                                "Mientras tanto sigue siendo tu responsabilidad.",
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                            )
                            FiltroDropdown(
                                etiqueta = "¿A quién se lo entregas? *",
                                opciones = viewModel.destinatarios,
                                seleccionId = destino, idDe = { it.id },
                                nombreDe = { "${it.nombre} · ${com.kernel94.inventario123.data.model.rolLabel(it.tipo)}" },
                                onSeleccion = { destino = it },
                                etiquetaNula = "Elige a la persona...",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = nota, onValueChange = { nota = it },
                                label = { Text("Motivo (opcional)") },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = destino != null && !viewModel.transfiriendo,
                            onClick = {
                                val d = destino ?: return@TextButton
                                mostrarTransferir = false
                                viewModel.transferir(d, nota) { _, msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                        ) { Text("Enviar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarTransferir = false }) { Text("Cancelar") }
                    },
                )
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
}

private fun etiquetaVista(vista: String): String = when (vista) {
    "bodega" -> "Bodega"; "mi_stock" -> "Mi Stock"; "todos" -> "Todos"; else -> vista.replaceFirstChar { it.uppercase() }
}
