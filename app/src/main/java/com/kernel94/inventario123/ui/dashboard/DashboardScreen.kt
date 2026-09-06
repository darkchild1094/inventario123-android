package com.kernel94.inventario123.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.DashMovimiento
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

private val EVENTOS = mapOf(
    "alta" to "Alta", "cambio_status" to "Cambio de estatus", "cambio_stock" to "Cambio de stock",
    "reemplazo_entra" to "Entra por reemplazo", "reemplazo_sale" to "Sale por reemplazo",
    "edicion" to "Edición", "baja" to "Baja", "eliminacion" to "Eliminación",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAbrirInventario: () -> Unit,
    onAbrirHistorial: () -> Unit,
    onAbrirTraslados: () -> Unit,
    onAbrirPendientes: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.cargar() }
    val r = viewModel.resumen
    val p = viewModel.perfil
    val nombre = p?.usuario?.nombre?.split(" ")?.firstOrNull() ?: "Usuario"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hola, $nombre", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "${p?.permisos?.tipo?.uppercase() ?: ""} · ${p?.usuario?.plaza_nombre ?: ""}",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .75f),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F3F5))
                .verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                viewModel.cargando && r == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                viewModel.error != null && r == null -> Text(viewModel.error!!, color = Color.Gray)
                r != null -> {
                    // KPIs
                    val kpis = listOf(
                        Triple("Total", r.total, Color(0xFF212529)),
                        Triple("En uso", r.por_status["en_uso"] ?: 0, Color(0xFF6C757D)),
                        Triple("En bodega", r.por_status["en_bodega"] ?: 0, Color(0xFF198754)),
                        Triple("Asignado", r.por_status["asignado"] ?: 0, Color(0xFF0D6EFD)),
                        Triple("Garantía", r.por_status["garantia"] ?: 0, Color(0xFFFFC107)),
                        Triple("Baja", r.por_status["baja"] ?: 0, Color(0xFFDC3545)),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        kpis.chunked(3).forEach { fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                fila.forEach { (lbl, n, c) ->
                                    Card(
                                        Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = c),
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text("$n", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge,
                                                color = if (lbl == "Garantía") Color.Black else Color.White)
                                            Text(lbl, style = MaterialTheme.typography.labelSmall,
                                                color = (if (lbl == "Garantía") Color.Black else Color.White).copy(alpha = .85f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (r.traslados_pendientes > 0) {
                        Card(
                            onClick = onAbrirTraslados,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                        ) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = Color(0xFF664D03))
                                Spacer(Modifier.width(10.dp))
                                Text("${r.traslados_pendientes} solicitud(es) por firmar", color = Color(0xFF664D03), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Por tipo de equipo
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Por tipo de equipo", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            val max = (r.por_dispositivo.maxOfOrNull { it.n } ?: 1).coerceAtLeast(1)
                            r.por_dispositivo.forEach { d ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(d.nombre, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    Text("${d.n}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE9ECEF))
                                ) {
                                    Box(Modifier.fillMaxWidth(d.n.toFloat() / max).fillMaxHeight().background(BsPrimary))
                                }
                            }
                        }
                    }

                    // Movimientos recientes
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Movimientos recientes", fontWeight = FontWeight.Bold)
                                TextButton(onClick = onAbrirHistorial) { Text("Ver todo") }
                            }
                            if (r.movimientos.isEmpty()) {
                                Text("Sin movimientos.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            } else r.movimientos.forEach { MovRow(it) }
                        }
                    }

                    Button(onClick = onAbrirInventario, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ver inventario")
                    }
                    OutlinedButton(onClick = onAbrirPendientes, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Altas pendientes de envío")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MovRow(m: DashMovimiento) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(EVENTOS[m.evento] ?: m.evento, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(m.creado_en?.take(16) ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Text(
            listOfNotNull(m.equipo?.ifBlank { null }, m.serie).joinToString(" · ").ifBlank { "Equipo" },
            style = MaterialTheme.typography.bodySmall, color = Color.Gray,
        )
    }
}
