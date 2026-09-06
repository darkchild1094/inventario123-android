package com.kernel94.inventario123.ui.solicitudes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.SolicitudTraslado
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudesScreen(
    viewModel: SolicitudesViewModel,
    onVolver: () -> Unit,
    onAbrirDetalle: (Int) -> Unit,
    onNueva: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.cargarLista() }
    val estados = listOf(null, "pendiente", "aprobada", "rechazada", "cancelada")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Traslados a bodega") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        floatingActionButton = {
            if (viewModel.puedeCrear) {
                FloatingActionButton(onClick = onNueva, containerColor = BsPrimary) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva solicitud", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                estados.forEach { e ->
                    FilterChip(
                        selected = viewModel.filtroEstado == e,
                        onClick = { viewModel.cargarLista(e) },
                        label = { Text(e?.replaceFirstChar { it.uppercase() } ?: "Todas") },
                    )
                }
            }

            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                viewModel.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(viewModel.error!!) }
                viewModel.solicitudes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin solicitudes.", color = Color.Gray) }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(viewModel.solicitudes) { s -> SolicitudRow(s) { onAbrirDetalle(s.id) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolicitudRow(s: SolicitudTraslado, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("#${s.id} · ${s.origen_nombre ?: "—"}", fontWeight = FontWeight.Bold)
                Text(
                    "${s.activos_count} activo(s) → ${s.bodega_nombre ?: "bodega"} · ${s.plaza_nombre ?: ""}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
                s.creado_en?.let { Text(it.take(16), style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
            }
            EstadoBadge(s.estado)
        }
    }
}

@Composable
fun EstadoBadge(estado: String) {
    val (bg, fg) = when (estado) {
        "pendiente" -> Color(0xFFFFF3CD) to Color(0xFF664D03)
        "aprobada"  -> Color(0xFFD1E7DD) to Color(0xFF0F5132)
        "rechazada" -> Color(0xFFF8D7DA) to Color(0xFF842029)
        else        -> Color(0xFFE2E3E5) to Color(0xFF41464B)
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(estado.uppercase(), color = fg, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
