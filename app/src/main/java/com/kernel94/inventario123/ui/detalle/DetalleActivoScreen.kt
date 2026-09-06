package com.kernel94.inventario123.ui.detalle

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kernel94.inventario123.data.model.Movimiento
import com.kernel94.inventario123.data.remote.Urls
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White),
                actions = {
                    val a = viewModel.activo
                    if (a?.puedeEditar == true) IconButton(onClick = { onEditar(activoId) }) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.White) }
                    if (a?.puedeEliminar == true) IconButton(onClick = { confirmarEliminar = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.White) }
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
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(a.dispositivo_nombre ?: "Equipo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            StatusBadge(a)
                        }
                        Spacer(Modifier.height(16.dp))
                        FilaDetalle("Serie", a.serie ?: "—")
                        FilaDetalle("Código de barras", a.codigoBarras ?: "—")
                        FilaDetalle("N° de activo", a.numActivo ?: "—")
                        FilaDetalle("Dispositivo", a.dispositivo_nombre ?: "—")
                        FilaDetalle("Marca", a.marca_nombre ?: "—")
                        FilaDetalle("Modelo", a.modelo_nombre ?: "—")
                        Divider(Modifier.padding(vertical = 10.dp))
                        FilaDetalle("Negocio", a.negocio_nombre ?: "—")
                        FilaDetalle("Región", a.region_nombre ?: "—")
                        FilaDetalle("Plaza", a.plaza_nombre ?: "—")
                        Divider(Modifier.padding(vertical = 10.dp))
                        FilaDetalle(a.ubicacionLabel, a.ubicacionValor)
                        if (!a.tienda_uso_nombre.isNullOrBlank()) FilaDetalle("En uso en", a.tienda_uso_nombre!!)
                        if (!a.procedencia_nombre.isNullOrBlank()) FilaDetalle("Procedencia", a.procedencia_nombre!!)
                        Divider(Modifier.padding(vertical = 10.dp))
                        FilaDetalle("Fecha de alta", a.fecha_alta ?: "—")
                        FilaDetalle("Última modificación", a.fecha_modificacion ?: "—")
                        FilaDetalle("ID interno", "#${a.id.toString().padStart(4, '0')}")

                        val fotos = listOfNotNull(
                            a.foto_equipo?.let { "Equipo" to it },
                            a.foto_serie?.let { "Serie" to it },
                            a.foto_activo?.let { "Código de barras" to it },
                        )
                        if (fotos.isNotEmpty()) {
                            Divider(Modifier.padding(vertical = 10.dp))
                            Text("Fotos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val context = LocalContext.current
                                fotos.forEach { (etiqueta, nombre) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AsyncImage(
                                            model = Urls.activoFotoThumb(nombre),
                                            contentDescription = etiqueta,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Urls.activoFoto(nombre)))
                                                    context.startActivity(intent)
                                                }
                                        )
                                        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        if (viewModel.timeline.isNotEmpty()) {
                            Divider(Modifier.padding(vertical = 14.dp))
                            Text("Línea de tiempo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            viewModel.timeline.forEach { m ->
                                MovimientoItem(m)
                                Divider(Modifier.padding(vertical = 6.dp), color = Color.LightGray.copy(alpha = 0.4f))
                            }
                        }
                        Spacer(Modifier.height(24.dp))
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
private fun MovimientoItem(m: Movimiento) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(color = colorEvento(m.evento), shape = RoundedCornerShape(50)) {
                Text(
                    m.eventoLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Text(m.creado_en ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Spacer(Modifier.height(4.dp))
        Text(m.equipoTitulo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(m.equipoIds, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
        if (m.hayCambioStatus) {
            Text("Estatus: ${m.status_anterior ?: "—"} → ${m.status_nuevo ?: "—"}", style = MaterialTheme.typography.bodySmall)
        }
        if (m.hayCambioStock) {
            Text("Stock: ${m.stock_ant_nombre ?: "—"} → ${m.stock_new_nombre}", style = MaterialTheme.typography.bodySmall)
        }
        if (!m.tienda_nombre.isNullOrBlank()) {
            Text("Tienda: ${m.tienda_nombre}", style = MaterialTheme.typography.bodySmall)
        }
        if (m.tieneRelacionado) {
            Spacer(Modifier.height(2.dp))
            Text("↔ ${m.relLabel}: ${m.relTitulo}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(m.relIds, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
        }
        if (!m.nota.isNullOrBlank()) {
            Text(m.nota, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text("Por: ${m.actor_nombre ?: "Sistema"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

/** Mismo mapeo de colores que _resultados.php (badges de evento). */
fun colorEvento(evento: String): Color = when (evento) {
    "alta", "reemplazo_entra" -> Color(0xFF198754)
    "cambio_status" -> Color(0xFF0D6EFD)
    "cambio_stock" -> Color(0xFF0DCAF0)
    "reemplazo_sale" -> Color(0xFFFFC107)
    "baja" -> Color(0xFFDC3545)
    "eliminacion" -> Color(0xFF212529)
    else -> Color(0xFF6C757D)
}

@Composable
private fun FilaDetalle(etiqueta: String, valor: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
