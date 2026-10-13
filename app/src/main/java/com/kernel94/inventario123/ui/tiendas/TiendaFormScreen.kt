package com.kernel94.inventario123.ui.tiendas

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

/** Alta/edición de tienda. id=null → alta nueva; con id precarga y permite borrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiendaFormScreen(
    viewModel: TiendasViewModel,
    tiendaId: Int?,
    onVolver: () -> Unit,
    onGuardado: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.iniciar()
        if (tiendaId != null) viewModel.cargarParaEditar(tiendaId)
    }
    val t = viewModel.tiendaEnEdicion
    var cr by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var plazaId by remember { mutableStateOf<Int?>(null) }
    var atiId by remember { mutableStateOf<Int?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(t) {
        if (tiendaId != null && t != null) {
            cr = t.cr_tienda ?: ""
            nombre = t.nombre
            plazaId = t.plaza_id
            atiId = t.ati_usuario_id
            t.plaza_id?.let { viewModel.cargarAtisForm(it) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (tiendaId == null) "Nueva tienda" else "Editar tienda") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                actions = {
                    if (tiendaId != null) {
                        IconButton(onClick = { confirmarEliminar = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        if (tiendaId != null && t == null && viewModel.cargandoForm) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = cr, onValueChange = { cr = it },
                label = { Text("CR (único) *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it },
                label = { Text("Nombre *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FiltroDropdown(
                etiqueta = "Plaza *", opciones = viewModel.plazas,
                seleccionId = plazaId, idDe = { it.id }, nombreDe = { it.nombre },
                onSeleccion = { plazaId = it; it?.let { id -> viewModel.cargarAtisForm(id) } },
                modifier = Modifier.fillMaxWidth(),
            )
            FiltroDropdown(
                etiqueta = "ATI responsable", opciones = viewModel.atisForm,
                seleccionId = atiId, idDe = { it.id }, nombreDe = { it.nombre },
                onSeleccion = { atiId = it },
                etiquetaNula = "— Sin asignar —",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val p = plazaId
                    if (cr.isBlank() || nombre.isBlank() || p == null) {
                        scope.launch { snackbarHostState.showSnackbar("Completa CR, nombre y plaza.") }
                        return@Button
                    }
                    guardando = true
                    viewModel.guardarTienda(tiendaId, cr.trim(), nombre.trim(), p, atiId) { ok, msg ->
                        guardando = false
                        if (ok) onGuardado(msg) else scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                },
                enabled = !guardando,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BsPrimary),
            ) {
                if (guardando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (tiendaId == null) "Crear tienda" else "Guardar cambios")
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar tienda?") },
            text = { Text("Esta acción no se puede deshacer. Si tiene activos instalados, no se podrá eliminar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminar = false
                        tiendaId?.let {
                            viewModel.eliminarTienda(it) { ok, msg ->
                                if (ok) onGuardado(msg) else scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } },
        )
    }
}
