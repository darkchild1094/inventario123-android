package com.kernel94.inventario123.ui.listado

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kernel94.inventario123.data.repository.Resultado
import com.kernel94.inventario123.ui.common.PlazaTabs
import com.kernel94.inventario123.ui.listado.components.ActivoCard
import com.kernel94.inventario123.ui.listado.components.ActivoListItem
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsCanvas
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    onAbrirTiendas: () -> Unit = {},
    onAbrirModelos: () -> Unit = {},
    onAbrirNotificaciones: () -> Unit = {},
    onAbrirModulo: (String) -> Unit = {},
    onAbrirConsulta: () -> Unit = {},
    onAbrirDashboard: () -> Unit = {},
    onAbrirUsuarios: () -> Unit = {},
    // Botón de auditoría física (escaneo). Bodega: solo si moduloEditable
    // (coordinador/admin). Mi Stock / Stock PFS: siempre visible — el
    // backend decide a quién puede auditar cada quien (uno mismo siempre).
    // Tiendas: siempre visible (ver InventarioTienda, otro mecanismo).
    onAbrirInventario: (() -> Unit)? = null,
    // Sólo modulo=="tiendas": editar esta tienda.
    onEditarTienda: (() -> Unit)? = null,
) {
    LaunchedEffect(modulo, tiendaId, usuarioId) { viewModel.iniciar(modulo, tiendaId, usuarioId) }
    var mostrarFiltros by remember { mutableStateOf(false) }
    var activoAEliminar by remember { mutableStateOf<Int?>(null) }
    var mostrarEnviar by remember { mutableStateOf(false) }
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
                onModulo = { m -> cerrarYHacer { onAbrirModulo(m) } },
                onDashboard = { cerrarYHacer(onAbrirDashboard) },
                onConsulta = { cerrarYHacer(onAbrirConsulta) },
                onNotificaciones = { cerrarYHacer(onAbrirNotificaciones) },
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
                        if (viewModel.modoSeleccion) {
                            IconButton(onClick = { viewModel.salirDeSeleccion() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Salir de selección", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = Color.White)
                            }
                        }
                    },
                    title = {
                        if (viewModel.modoSeleccion) {
                            Text("${viewModel.seleccion.size} seleccionado(s)", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            val m = viewModel.modulo
                            Text(
                                m?.replaceFirstChar { it.uppercase() }
                                    ?: (viewModel.perfil?.usuario?.plaza_nombre ?: "Inventario123"),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White,
                            )
                        }
                    },
                    actions = {
                        if (viewModel.modoSeleccion) {
                            IconButton(onClick = { viewModel.seleccionarTodos() }) {
                                Icon(Icons.Filled.SelectAll, contentDescription = "Seleccionar todos", tint = Color.White)
                            }
                            return@TopAppBar
                        }
                        if (onEditarTienda != null) {
                            IconButton(onClick = onEditarTienda) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar tienda", tint = Color.White)
                            }
                        }
                        val puedeVerInventario = onAbrirInventario != null && when (viewModel.modulo) {
                            "bodega" -> viewModel.moduloEditable
                            "mi_stock", "stock_pfs", "tiendas" -> true
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
                if (viewModel.modoSeleccion) {
                    ExtendedFloatingActionButton(
                        onClick = { mostrarEnviar = true },
                        containerColor = BsPrimary,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar ${viewModel.seleccion.size}")
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
            Column(Modifier.padding(padding).fillMaxSize().background(BsCanvas)) {

                // Una pestaña por plaza en los módulos acotados por plaza. Sin
                // esto un coordinador con dos plazas veía las dos bodegas
                // revueltas en una sola lista.
                if (viewModel.mostrarPestanasPlaza) {
                    PlazaTabs(
                        opciones = viewModel.misPlazas,
                        seleccionId = viewModel.plazaId,
                        onSeleccion = {
                            viewModel.plazaId = it
                            viewModel.onFiltroChange()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                viewModel.tiendaActual?.let { t ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                listOfNotNull(t.cr_tienda?.let { cr -> "CR $cr" }, t.plaza_nombre).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                            )
                            Text(
                                if (t.ultimo_inv_fecha != null)
                                    "Último inventario: ${t.ultimo_inv_fecha} · ${t.ultimo_inv_encontrados ?: 0} encontrados" +
                                        (t.ultimo_inv_no_encontrados?.takeIf { it > 0 }?.let { " · $it no encontrados" } ?: "")
                                else "Sin inventario registrado",
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
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
                                        Checkbox(checked = marcado, onCheckedChange = { viewModel.alternarSeleccion(activo.id) })
                                    }
                                    Box(Modifier.weight(1f)) {
                                        val abrir = {
                                            if (viewModel.modoSeleccion) viewModel.alternarSeleccion(activo.id)
                                            else onAbrirDetalle(activo.id)
                                        }
                                        if (viewModel.moduloEditable) {
                                            ActivoCard(
                                                activo = activo,
                                                onClick = abrir,
                                                onEditar = { onEditar(activo.id) },
                                                onEliminar = { activoAEliminar = activo.id },
                                                onMantenerPresionado = if (viewModel.puedeEnviarAqui) {
                                                    { viewModel.activarSeleccion(activo.id) }
                                                } else null,
                                            )
                                        } else {
                                            // Solo lectura (ej. el ATI viendo Bodega, o el
                                            // stock de otro usuario): lista compacta, sin
                                            // editar/eliminar, y tocar solo lleva al detalle.
                                            ActivoListItem(activo = activo, onClick = abrir)
                                        }
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

            // "Enviar" inmediato: a bodega (de otra plaza) o a una persona de
            // la plaza. No hay aceptación: el equipo cambia de manos ya, y el
            // receptor solo recibe un aviso (ver Notificaciones).
            if (mostrarEnviar) {
                var aBodega by remember { mutableStateOf(viewModel.modulo == "bodega" || viewModel.modulo == "mi_stock") }
                var destinoPlaza by remember { mutableStateOf<Int?>(null) }
                var destinoUsuario by remember { mutableStateOf<Int?>(null) }
                AlertDialog(
                    onDismissRequest = { mostrarEnviar = false },
                    icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                    title = { Text("Enviar ${viewModel.seleccion.size} equipo(s)") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "El equipo cambia de lugar de inmediato; la otra persona solo recibe un aviso.",
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = aBodega, onClick = { aBodega = true })
                                Text("A bodega", modifier = Modifier.clickable { aBodega = true })
                                Spacer(Modifier.width(16.dp))
                                RadioButton(selected = !aBodega, onClick = { aBodega = false })
                                Text("Al usuario", modifier = Modifier.clickable { aBodega = false })
                            }
                            if (aBodega) {
                                FiltroDropdown(
                                    etiqueta = "Bodega destino (plaza) *",
                                    opciones = viewModel.plazasDestino,
                                    seleccionId = destinoPlaza, idDe = { it.id }, nombreDe = { it.nombre },
                                    onSeleccion = { destinoPlaza = it },
                                    etiquetaNula = "Elige la plaza...",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                FiltroDropdown(
                                    etiqueta = "¿A quién se lo entregas? *",
                                    opciones = viewModel.destinatarios,
                                    seleccionId = destinoUsuario, idDe = { it.id },
                                    nombreDe = { "${it.nombre} · ${com.kernel94.inventario123.data.model.rolLabel(it.tipo)}" },
                                    onSeleccion = { destinoUsuario = it },
                                    etiquetaNula = "Elige a la persona...",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = (if (aBodega) destinoPlaza != null else destinoUsuario != null) && !viewModel.enviando,
                            onClick = {
                                mostrarEnviar = false
                                viewModel.enviar(
                                    if (aBodega) "bodega" else "usuario",
                                    if (aBodega) destinoPlaza else null,
                                    if (aBodega) null else destinoUsuario,
                                ) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            },
                        ) { Text("Enviar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarEnviar = false }) { Text("Cancelar") }
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
