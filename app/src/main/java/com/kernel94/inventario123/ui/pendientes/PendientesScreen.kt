package com.kernel94.inventario123.ui.pendientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.ActivoPendiente
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendientesScreen(viewModel: PendientesViewModel, onVolver: () -> Unit) {
    val lista by viewModel.pendientes.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensaje = null }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Pendientes de envío") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    IconButton(onClick = { viewModel.sincronizar() }, enabled = !viewModel.sincronizando) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sincronizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.sincronizando) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (lista.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay altas pendientes.", color = Color.Gray)
                }
            } else {
                if (lista.any { it.estado == "enviado" }) {
                    TextButton(onClick = { viewModel.limpiarEnviados() }, modifier = Modifier.align(Alignment.End)) {
                        Text("Quitar los ya enviados")
                    }
                }
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(lista, key = { it.localId }) { p -> PendienteRow(p, viewModel) }
                }
            }
        }
    }
}

@Composable
private fun PendienteRow(p: ActivoPendiente, vm: PendientesViewModel) {
    val (bg, fg, txt) = when (p.estado) {
        "enviado"  -> Triple(Color(0xFFD1E7DD), Color(0xFF0F5132), "Enviado · ID ${p.serverId ?: "—"}")
        "error"    -> Triple(Color(0xFFF8D7DA), Color(0xFF842029), "Error")
        "enviando" -> Triple(Color(0xFFCFE2FF), Color(0xFF084298), "Enviando…")
        else       -> Triple(Color(0xFFFFF3CD), Color(0xFF664D03), "Pendiente")
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(p.titulo, fontWeight = FontWeight.SemiBold)
                Surface(color = bg, shape = MaterialTheme.shapes.small) {
                    Text(txt, color = fg, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            val cat = p.campos["modelo_id"]?.let { "modelo #$it" } ?: ""
            Text(
                "Estatus: ${p.campos["status"] ?: "—"} · CB: ${p.campos["codigo_barras"] ?: "—"} $cat",
                style = MaterialTheme.typography.bodySmall, color = Color.Gray,
            )
            p.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF842029)) }
            Row {
                if (p.estado == "error" || p.estado == "pendiente") {
                    TextButton(onClick = { vm.reintentar(p) }) { Text("Reintentar") }
                }
                if (p.estado != "enviando") {
                    TextButton(onClick = { vm.descartar(p) }) { Text("Descartar", color = Color(0xFF842029)) }
                }
            }
        }
    }
}
