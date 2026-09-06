package com.kernel94.inventario123.ui.historial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Movimiento
import com.kernel94.inventario123.ui.detalle.colorEvento
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel,
    onVolver: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.iniciar() }
    var mostrarFiltros by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Historial de movimientos")
                        Text(
                            "${viewModel.totalResultados} movimiento(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                actions = {
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Filtros", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            OutlinedTextField(
                value = viewModel.serie,
                onValueChange = { viewModel.onSerieChange(it) },
                label = { Text("Serie / código / N° activo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            )

            if (mostrarFiltros) {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    FiltroDropdown(
                        etiqueta = "Evento",
                        opciones = Movimiento.EVENTOS.entries.toList(),
                        seleccionId = viewModel.evento?.let { ev -> Movimiento.EVENTOS.keys.indexOf(ev) }?.takeIf { it >= 0 },
                        idDe = { Movimiento.EVENTOS.keys.indexOf(it.key) },
                        nombreDe = { it.value },
                        onSeleccion = { idx ->
                            viewModel.evento = idx?.let { Movimiento.EVENTOS.keys.toList().getOrNull(it) }
                            viewModel.onFiltroChange()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    FiltroDropdown(
                        etiqueta = "Tienda", opciones = viewModel.catalogos.tiendas,
                        seleccionId = viewModel.tiendaId, idDe = { it.id }, nombreDe = { it.nombre },
                        onSeleccion = { viewModel.tiendaId = it; viewModel.onFiltroChange() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    FiltroDropdown(
                        etiqueta = "Usuario", opciones = viewModel.catalogos.usuarios,
                        seleccionId = viewModel.usuarioId, idDe = { it.id }, nombreDe = { it.nombre },
                        onSeleccion = { viewModel.usuarioId = it; viewModel.onFiltroChange() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.desde,
                            onValueChange = { viewModel.desde = it },
                            label = { Text("Desde (AAAA-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = viewModel.hasta,
                            onValueChange = { viewModel.hasta = it },
                            label = { Text("Hasta (AAAA-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.onFiltroChange() }) { Text("Aplicar fechas") }
                        TextButton(onClick = { viewModel.limpiarFiltros() }) { Text("Limpiar") }
                    }
                }
            }

            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                viewModel.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(viewModel.error!!, color = Color.Gray) }
                viewModel.movimientos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay movimientos con los filtros aplicados.", color = Color.Gray) }
                else -> LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(viewModel.movimientos) { m -> MovimientoRow(m) }
                }
            }

            if (viewModel.totalPaginas > 1) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.paginaAnterior() }, enabled = viewModel.paginaActual > 1) { Text("‹ Anterior") }
                    Text("Página ${viewModel.paginaActual} de ${viewModel.totalPaginas}", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { viewModel.paginaSiguiente() }, enabled = viewModel.paginaActual < viewModel.totalPaginas) { Text("Siguiente ›") }
                }
            }
        }
    }
}

@Composable
private fun MovimientoRow(m: Movimiento) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = colorEvento(m.evento), shape = RoundedCornerShape(50)) {
                    Text(
                        m.eventoLabel, color = Color.White,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Text(m.creado_en ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(Modifier.height(6.dp))
            Text(m.equipoTitulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(m.equipoIds, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
            if (m.hayCambioStatus) Text("Estatus: ${m.status_anterior ?: "—"} → ${m.status_nuevo ?: "—"}", style = MaterialTheme.typography.bodySmall)
            if (m.hayCambioStock) Text("Stock: ${m.stock_ant_nombre ?: "—"} → ${m.stock_new_nombre}", style = MaterialTheme.typography.bodySmall)
            else if (!m.stock_new_nombre.isNullOrBlank() || !m.stock_ant_nombre.isNullOrBlank())
                Text("Stock: ${m.stock_new_nombre ?: m.stock_ant_nombre}", style = MaterialTheme.typography.bodySmall)
            if (!m.tienda_nombre.isNullOrBlank()) Text("Tienda: ${m.tienda_nombre}", style = MaterialTheme.typography.bodySmall)
            if (m.tieneRelacionado) {
                Spacer(Modifier.height(2.dp))
                Text("↔ ${m.relLabel}: ${m.relTitulo}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(m.relIds, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
            }
            if (!m.nota.isNullOrBlank()) Text(m.nota, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Por: ${m.actor_nombre ?: "Sistema"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
