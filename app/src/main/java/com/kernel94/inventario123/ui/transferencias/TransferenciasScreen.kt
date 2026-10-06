package com.kernel94.inventario123.ui.transferencias

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Transferencia
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

private val VERDE = Color(0xFF198754)
private val ROJO = Color(0xFFDC3545)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferenciasScreen(
    viewModel: TransferenciasViewModel,
    onVolver: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.cargar() }
    val snackbar = remember { SnackbarHostState() }
    var rechazando by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let { snackbar.showSnackbar(it); viewModel.limpiarMensaje() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Transferencias", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        if (viewModel.cargando && viewModel.porAceptar.isEmpty() && viewModel.enviadas.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F3F5)),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Para ti", fontWeight = FontWeight.Bold)
                Text(
                    "Equipo que alguien te mandó. No es tuyo hasta que lo aceptas.",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                )
            }
            if (viewModel.porAceptar.isEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Inbox, contentDescription = null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("Nada esperando tu respuesta.", color = Color.Gray)
                    }
                }
            } else {
                items(viewModel.porAceptar, key = { "r${it.id}" }) { t ->
                    TarjetaTransferencia(t, ocupado = viewModel.ocupado) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.aceptar(t.id) },
                                enabled = !viewModel.ocupado,
                                colors = ButtonDefaults.buttonColors(containerColor = VERDE),
                            ) { Text("Aceptar") }
                            OutlinedButton(
                                onClick = { rechazando = t.id },
                                enabled = !viewModel.ocupado,
                            ) { Text("Rechazar", color = ROJO) }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Las que mandaste", fontWeight = FontWeight.Bold)
            }
            if (viewModel.enviadas.isEmpty()) {
                item { Text("Todavía no has transferido nada.", color = Color.Gray) }
            } else {
                items(viewModel.enviadas, key = { "e${it.id}" }) { t ->
                    TarjetaTransferencia(t, ocupado = viewModel.ocupado) {
                        if (t.puedeCancelar) {
                            OutlinedButton(onClick = { viewModel.cancelar(t.id) }, enabled = !viewModel.ocupado) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }
    }

    rechazando?.let { id ->
        var motivo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rechazando = null },
            title = { Text("¿Por qué no lo aceptas?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "El equipo se queda con quien te lo mandó. El motivo le llega a esa persona.",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                    )
                    OutlinedTextField(
                        value = motivo, onValueChange = { motivo = it },
                        label = { Text("Motivo *") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = motivo.isNotBlank(),
                    onClick = { viewModel.rechazar(id, motivo); rechazando = null },
                ) { Text("Rechazar", color = ROJO) }
            },
            dismissButton = { TextButton(onClick = { rechazando = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun TarjetaTransferencia(
    t: Transferencia,
    ocupado: Boolean,
    acciones: @Composable () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("De: ${t.desdeLabel}", fontWeight = FontWeight.Bold)
                EstadoChip(t.estado)
            }
            t.destino_usuario_nombre?.let {
                Text("Para: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                "${t.activos.size} equipo(s)",
                style = MaterialTheme.typography.labelMedium, color = BsPrimary,
            )
            t.activos.take(4).forEach { a ->
                Text(
                    listOfNotNull(
                        a.dispositivo_nombre, a.marca_nombre, a.modelo_nombre,
                        a.serie?.takeIf { s -> s.isNotBlank() } ?: a.codigo_barras?.let { c -> "CB $c" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (t.activos.size > 4) {
                Text("…y ${t.activos.size - 4} más", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            t.nota?.takeIf { it.isNotBlank() }?.let {
                Text("Motivo: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            t.motivo_rechazo?.takeIf { it.isNotBlank() }?.let {
                Text("Rechazada: $it", style = MaterialTheme.typography.bodySmall, color = ROJO)
            }
            acciones()
        }
    }
}

@Composable
private fun EstadoChip(estado: String) {
    val (texto, color) = when (estado) {
        "pendiente" -> "Pendiente" to Color(0xFFB8860B)
        "aprobada"  -> "Aceptada" to VERDE
        "rechazada" -> "Rechazada" to ROJO
        "cancelada" -> "Cancelada" to Color.Gray
        else        -> estado to Color.Gray
    }
    Text(texto, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
}
