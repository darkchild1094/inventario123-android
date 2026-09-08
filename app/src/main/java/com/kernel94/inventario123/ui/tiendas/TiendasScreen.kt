package com.kernel94.inventario123.ui.tiendas

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
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiendasScreen(
    viewModel: TiendasViewModel,
    onVolver: () -> Unit,
    onAbrirTienda: (Int) -> Unit = {},
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
                title = { Text("Tiendas") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Entra a una tienda para ver y administrar sus activos." +
                    if (viewModel.puedeAsignarAti) " Puedes asignar el ATI responsable de cada tienda." else "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            FiltroDropdown(
                etiqueta = "Plaza", opciones = viewModel.plazas,
                seleccionId = viewModel.plazaId, idDe = { it.id }, nombreDe = { it.nombre },
                onSeleccion = { viewModel.onPlazaChange(it) },
                etiquetaNula = "Todas mis plazas",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = viewModel.busqueda,
                onValueChange = { viewModel.onBusquedaChange(it) },
                label = { Text("Nombre o CR de tienda") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )

            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                viewModel.tiendas.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin tiendas para el filtro.", color = Color.Gray) }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(viewModel.tiendas, key = { it.id }) { t ->
                        Card(
                            onClick = { onAbrirTienda(t.id) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(t.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            listOfNotNull(
                                                t.cr_tienda?.let { "CR $it" },
                                                t.plaza_nombre,
                                            ).joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                                        )
                                    }
                                    AssistChip(onClick = { onAbrirTienda(t.id) }, label = { Text("${t.activos_count} activos") })
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                                if (viewModel.puedeAsignarAti) {
                                    Spacer(Modifier.height(6.dp))
                                    FiltroDropdown(
                                        etiqueta = "ATI responsable", opciones = viewModel.atis,
                                        seleccionId = t.ati_usuario_id, idDe = { it.id }, nombreDe = { it.nombre },
                                        onSeleccion = { viewModel.asignarAti(t.id, it) },
                                        etiquetaNula = "— Sin asignar —",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
