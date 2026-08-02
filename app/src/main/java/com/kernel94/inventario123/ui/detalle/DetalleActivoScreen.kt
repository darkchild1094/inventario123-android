package com.kernel94.inventario123.ui.detalle

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.ui.listado.components.StatusBadge
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleActivoScreen(
    viewModel: DetalleViewModel,
    activoId: Int,
    onVolver: () -> Unit,
    onEditar: (Int) -> Unit,
) {
    LaunchedEffect(activoId) { viewModel.cargar(activoId) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var mensajeFinal by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del activo") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = androidx.compose.ui.graphics.Color.White, navigationIconContentColor = androidx.compose.ui.graphics.Color.White),
                actions = {
                    val a = viewModel.activo
                    if (a?.puedeEditar == true) IconButton(onClick = { onEditar(activoId) }) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = androidx.compose.ui.graphics.Color.White) }
                    if (a?.puedeEliminar == true) IconButton(onClick = { confirmarEliminar = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = androidx.compose.ui.graphics.Color.White) }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                viewModel.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(viewModel.error!!) }
                viewModel.activo != null -> {
                    val a = viewModel.activo!!
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(a.dispositivo_nombre ?: "Equipo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            StatusBadge(a)
                        }
                        Spacer(Modifier.height(16.dp))
                        FilaDetalle("Serie", a.serie ?: "—")
                        FilaDetalle("Placa / Activo Fijo", a.placa ?: "—")
                        FilaDetalle("Modelo", a.modelo_nombre ?: "—")
                        Divider(Modifier.padding(vertical = 10.dp))
                        FilaDetalle("Negocio", a.negocio_nombre ?: "—")
                        FilaDetalle("Región", a.region_nombre ?: "—")
                        FilaDetalle("Plaza", a.plaza_nombre ?: "—")
                        Divider(Modifier.padding(vertical = 10.dp))
                        FilaDetalle(if (a.stock_tipo == "usuario") "Asignado a" else "Bodega", a.asignadoOBodega)
                        if (!a.tienda_uso_nombre.isNullOrBlank()) FilaDetalle("En uso en", a.tienda_uso_nombre!!)
                        if (!a.procedencia_nombre.isNullOrBlank()) FilaDetalle("Procedencia", a.procedencia_nombre!!)
                        Divider(Modifier.padding(vertical = 10.dp))
                        FilaDetalle("Fecha de alta", a.fecha_alta ?: "—")
                        FilaDetalle("Última modificación", a.fecha_modificacion ?: "—")
                        FilaDetalle("ID interno", "#${a.id.toString().padStart(4, '0')}")
                    }
                }
            }

            if (confirmarEliminar) {
                AlertDialog(
                    onDismissRequest = { confirmarEliminar = false },
                    title = { Text("¿Eliminar este activo?") },
                    text = { Text("Esta acción no se puede deshacer.") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmarEliminar = false
                            viewModel.eliminar(activoId) { ok, msg -> mensajeFinal = msg; if (ok) onVolver() }
                        }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } }
                )
            }
        }
    }
}

@Composable
private fun FilaDetalle(etiqueta: String, valor: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
