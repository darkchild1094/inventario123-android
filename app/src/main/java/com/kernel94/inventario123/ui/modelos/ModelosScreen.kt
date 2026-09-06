package com.kernel94.inventario123.ui.modelos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Modelo
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelosScreen(viewModel: ModelosViewModel, onVolver: () -> Unit) {
    LaunchedEffect(Unit) { viewModel.cargar() }
    var enEdicion by remember { mutableStateOf<Modelo?>(null) }
    var mostrarForm by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Modelo?>(null) }
    var filtroTexto by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val lista = remember(viewModel.modelos, filtroTexto) {
        val t = filtroTexto.trim().lowercase()
        if (t.isEmpty()) viewModel.modelos
        else viewModel.modelos.filter {
            "${it.nombre} ${it.marca_nombre ?: ""} ${it.dispositivo_nombre ?: ""}".lowercase().contains(t)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de modelos") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { enEdicion = null; mostrarForm = true }, containerColor = BsPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo modelo", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = filtroTexto, onValueChange = { filtroTexto = it },
                label = { Text("Buscar modelo / marca / categoría") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                viewModel.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(viewModel.error!!) }
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lista) { m ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(m.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        listOfNotNull(m.dispositivo_nombre, m.marca_nombre).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                                    )
                                    Text("${m.activos_count} activos", style = MaterialTheme.typography.labelSmall, color = BsPrimary)
                                }
                                IconButton(onClick = { enEdicion = m; mostrarForm = true }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                                IconButton(onClick = { aEliminar = m }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarForm) {
        FormularioModeloDialog(
            viewModel = viewModel,
            modelo = enEdicion,
            onCerrar = { mostrarForm = false },
            onGuardado = { _, msg -> mostrarForm = false; scope.launch { snackbar.showSnackbar(msg) } },
        )
    }

    aEliminar?.let { m ->
        EliminarModeloDialog(
            viewModel = viewModel,
            modelo = m,
            onCerrar = { aEliminar = null },
            onListo = { _, msg -> aEliminar = null; scope.launch { snackbar.showSnackbar(msg) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioModeloDialog(
    viewModel: ModelosViewModel,
    modelo: Modelo?,
    onCerrar: () -> Unit,
    onGuardado: (Boolean, String) -> Unit,
) {
    var nombre by remember { mutableStateOf(modelo?.nombre ?: "") }
    var dispositivoId by remember { mutableStateOf(modelo?.dispositivo_id) }
    var marcaId by remember { mutableStateOf(modelo?.marca_id) }
    var marcaNueva by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (modelo == null) "Nuevo modelo" else "Editar modelo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FiltroDropdown(
                    etiqueta = "Categoría de dispositivo",
                    opciones = viewModel.dispositivos,
                    seleccionId = dispositivoId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { dispositivoId = it },
                    etiquetaNula = "Selecciona…",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                FiltroDropdown(
                    etiqueta = "Marca",
                    opciones = viewModel.marcas,
                    seleccionId = marcaId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { marcaId = it; if (it != null) marcaNueva = "" },
                    etiquetaNula = "— Sin marca —",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                OutlinedTextField(
                    value = marcaNueva, onValueChange = { marcaNueva = it; if (it.isNotBlank()) marcaId = null },
                    label = { Text("…o escribe una marca nueva") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it.take(100) },
                    label = { Text("Modelo *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = nombre.isNotBlank() && (dispositivoId ?: 0) > 0,
                onClick = {
                    viewModel.guardar(
                        id = modelo?.id, nombre = nombre, dispositivoId = dispositivoId ?: 0,
                        marcaId = marcaId, marcaNueva = marcaNueva, onListo = onGuardado,
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

@Composable
private fun EliminarModeloDialog(
    viewModel: ModelosViewModel,
    modelo: Modelo,
    onCerrar: () -> Unit,
    onListo: (Boolean, String) -> Unit,
) {
    val enUso = modelo.activos_count > 0
    var destinoId by remember { mutableStateOf<Int?>(null) }
    // mismo dispositivo primero
    val destinos = remember(viewModel.modelos, modelo) {
        viewModel.modelos.filter { it.id != modelo.id }
            .sortedByDescending { it.dispositivo_id == modelo.dispositivo_id }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Eliminar modelo") },
        text = {
            Column {
                Text("¿Eliminar «${modelo.nombre}»?")
                if (enUso) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${modelo.activos_count} activos usan este modelo. Elige a qué modelo reasignarlos:",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    )
                    FiltroDropdown(
                        etiqueta = "Reasignar activos a",
                        opciones = destinos,
                        seleccionId = destinoId, idDe = { it.id },
                        nombreDe = { "${it.dispositivo_nombre ?: ""} · ${it.marca_nombre?.let { m -> "$m " } ?: ""}${it.nombre}" },
                        onSeleccion = { destinoId = it },
                        etiquetaNula = "Selecciona un modelo destino…",
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !enUso || destinoId != null,
                onClick = { viewModel.eliminar(modelo.id, if (enUso) destinoId else null, onListo) }
            ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
