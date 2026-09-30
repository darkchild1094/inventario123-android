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
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockPfsListaScreen(
    viewModel: StockPfsListaViewModel,
    onVolver: () -> Unit,
    onSeleccionarUsuario: (Usuario) -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.iniciar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock PFS") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        when {
            viewModel.cargando -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BsPrimary)
            }
            viewModel.usuarios.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no hay ingenieros con activos registrados a su nombre.", color = Color.Gray)
            }
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(viewModel.usuarios, key = { it.id }) { u -> UsuarioStockCard(u, onClick = { onSeleccionarUsuario(u) }) }
            }
        }
    }
}

@Composable
private fun UsuarioStockCard(u: Usuario, onClick: () -> Unit) {
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
                Text(u.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                if (u.plaza_nombre != null) {
                    Text(u.plaza_nombre, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            AssistChip(
                onClick = onClick,
                label = { Text("${u.activos_count} activo${if (u.activos_count == 1) "" else "s"}") },
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
