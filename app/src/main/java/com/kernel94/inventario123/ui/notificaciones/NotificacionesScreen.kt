package com.kernel94.inventario123.ui.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Notificacion
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

/**
 * "Enviar" recibidos/enviados — reemplaza Historial/Transferencias/
 * SolicitudTraslado/Altas pendientes de envío (ver rediseño). Quién, cuándo,
 * sentido, y los activos del lote; tocar marca leída.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    viewModel: NotificacionesViewModel,
    onVolver: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.cargar() }
    val notifs by viewModel.notificaciones.collectAsState()
    val sinLeer = notifs.count { !it.leida }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notificaciones", color = Color.White)
                        Text(
                            if (sinLeer > 0) "$sinLeer sin leer" else "Todo al día",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .75f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) }
                },
                actions = {
                    if (sinLeer > 0) {
                        IconButton(onClick = { viewModel.marcarLeida(null) }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Marcar todas como leídas", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F3F5))) {
            if (notifs.isEmpty()) {
                Text(
                    "Sin notificaciones.", color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(notifs, key = { it.id }) { n -> NotificacionCard(n, onClick = { viewModel.marcarLeida(n.id) }) }
                }
            }
        }
    }
}

@Composable
private fun NotificacionCard(n: Notificacion, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (n.leida) Color.White else Color(0xFFE7F0FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (n.leida) Color(0xFFE9ECEF) else BsPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.SwapHoriz, contentDescription = null,
                    tint = if (n.leida) Color(0xFF6C757D) else Color.White,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        n.de_nombre,
                        fontWeight = if (n.leida) FontWeight.Medium else FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(n.creado_en.take(16), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text(
                    if (n.direccion == "a_stock") "te transfirió ${n.activos.size} activo(s) a tu stock"
                    else "envió ${n.activos.size} activo(s) a ${n.lugar}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(4.dp))
                Surface(color = Color(0xFFE8DEF8), shape = RoundedCornerShape(50)) {
                    Text(
                        if (n.direccion == "a_stock") "${n.lugar} → Mi stock" else "Mi stock → ${n.lugar}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                n.activos.forEach { a ->
                    Text(
                        "${a.dispositivo_nombre ?: ""} · ${a.modelo_nombre ?: ""} · ${a.serie ?: a.codigo_barras ?: ""}",
                        style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1,
                    )
                }
            }
            if (!n.leida) {
                Box(Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(BsPrimary))
            }
        }
    }
}
