package com.kernel94.inventario123.ui.rentec

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.ProyectoRentec
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentecListaScreen(
    viewModel: RentecListaViewModel,
    onVolver: () -> Unit,
    onAbrirProyecto: (Int) -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.iniciar() }
    var mostrarCrear by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
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
                title = { Text("RENTEC") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarCrear = true }, containerColor = BsPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo proyecto", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Renovación Tecnológica: crea un proyecto para agrupar el equipo recibido e instalado.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                viewModel.proyectos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aún no hay proyectos RENTEC.", color = Color.Gray) }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                ) {
                    items(viewModel.proyectos, key = { it.id }) { p -> ProyectoCard(p, onClick = { onAbrirProyecto(p.id) }) }
                }
            }
        }
    }

    if (mostrarCrear) {
        var nombre by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!viewModel.creando) mostrarCrear = false },
            title = { Text("Nuevo proyecto RENTEC") },
            text = {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre (ej. \"Renovación CCTV Valles\")") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !viewModel.creando,
                    onClick = {
                        viewModel.crear(nombre) { id -> mostrarCrear = false; onAbrirProyecto(id) }
                    },
                ) { Text(if (viewModel.creando) "Creando…" else "Crear") }
            },
            dismissButton = { TextButton(onClick = { mostrarCrear = false }, enabled = !viewModel.creando) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ProyectoCard(p: ProyectoRentec, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(p.folio, p.usuario_nombre).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                )
                Text(
                    "${p.recibidos} en bodega · ${p.instalados} instalados",
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                )
            }
            AssistChip(
                onClick = onClick,
                label = { Text(if (p.abierto) "Abierto" else "Cerrado") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (p.abierto) Color(0xFFFFF3CD) else Color(0xFFE2E3E5)
                ),
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
