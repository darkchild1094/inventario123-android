package com.kernel94.inventario123.ui.bodega

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.InventarioBodega
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioBodegaScreen(
    viewModel: InventarioBodegaViewModel,
    onVolver: () -> Unit,
    onAbrirInventario: (Int) -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.iniciar() }
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
                title = { Text("Inventario de Bodega") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.bodegas.size > 1) {
                FiltroDropdown(
                    etiqueta = "Bodega", opciones = viewModel.bodegas,
                    seleccionId = viewModel.bodegaId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { viewModel.onBodegaChange(it) },
                    etiquetaNula = "Selecciona una bodega",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            val inventarioAbierto = viewModel.inventarios.firstOrNull { it.abierto }
            Button(
                onClick = {
                    if (inventarioAbierto != null) onAbrirInventario(inventarioAbierto.id)
                    else viewModel.iniciarInventario(onAbrirInventario)
                },
                enabled = viewModel.bodegaId != null && !viewModel.iniciando,
                colors = ButtonDefaults.buttonColors(containerColor = BsPrimary),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (viewModel.iniciando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(if (inventarioAbierto != null) "Continuar inventario de ${inventarioAbierto.periodo}" else "Iniciar inventario de este mes")
                }
            }

            Text(
                "Histórico de inventarios (por mes)",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                viewModel.bodegaId == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Selecciona una bodega.", color = Color.Gray) }
                viewModel.inventarios.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aún no hay inventarios registrados.", color = Color.Gray) }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(viewModel.inventarios, key = { it.id }) { inv -> InventarioCard(inv, onClick = { onAbrirInventario(inv.id) }) }
                }
            }
        }
    }
}

@Composable
private fun InventarioCard(inv: InventarioBodega, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(inv.periodo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${inv.total_encontrado} de ${inv.total_esperado} encontrados" +
                        (inv.usuario_nombre?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                )
            }
            AssistChip(
                onClick = onClick,
                label = { Text(if (inv.abierto) "Abierto" else "Cerrado") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (inv.abierto) Color(0xFFFFF3CD) else Color(0xFFE2E3E5)
                )
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
