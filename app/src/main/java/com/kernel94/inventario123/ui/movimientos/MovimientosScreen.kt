package com.kernel94.inventario123.ui.movimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernel94.inventario123.data.model.Movimiento
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val PESTANAS = listOf("bodega" to "Bodega", "usuario" to "Usuario", "tienda" to "Tienda")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    viewModel: MovimientosViewModel,
    onVolver: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movimientos", color = Color.White) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F3F5))) {
            TabRow(
                selectedTabIndex = PESTANAS.indexOfFirst { it.first == viewModel.pestana }.coerceAtLeast(0),
                containerColor = BsDark, contentColor = Color.White,
            ) {
                PESTANAS.forEach { (clave, etiqueta) ->
                    Tab(
                        selected = viewModel.pestana == clave,
                        onClick = { viewModel.cambiarPestana(clave) },
                        text = { Text(etiqueta) },
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                OutlinedTextField(
                    value = viewModel.busqueda,
                    onValueChange = { viewModel.onBusquedaChange(it) },
                    placeholder = { Text("Activo, serie, N° activo, tienda...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }

            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                viewModel.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(viewModel.error!!, color = Color.Gray) }
                viewModel.movimientos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin movimientos.", color = Color.Gray) }
                else -> {
                    val listState = rememberLazyListState()
                    val alFinal by remember {
                        derivedStateOf {
                            val ultimo = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            ultimo >= viewModel.movimientos.lastIndex - 5
                        }
                    }
                    LaunchedEffect(alFinal, viewModel.movimientos.size) { if (alFinal) viewModel.cargarMas() }

                    val grupos = viewModel.movimientos.groupBy { etiquetaDia(it.creado_en) }

                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        grupos.forEach { (dia, filas) ->
                            item(key = "dia_$dia") {
                                Text(
                                    dia, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF6C757D), modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(filas, key = { it.id }) { m -> FilaMovimiento(m) }
                        }
                        item(key = "pie") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                if (viewModel.cargandoMas) CircularProgressIndicator(color = BsPrimary, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                else Text("${viewModel.movimientos.size} de ${viewModel.totalResultados}", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaMovimiento(m: Movimiento) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(m.eventoLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = BsPrimary)
                Text(m.creado_en?.drop(11)?.take(5) ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(m.equipoTitulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(m.equipoIds, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
            if (!m.stock_ant_nombre.isNullOrBlank() || !m.stock_new_nombre.isNullOrBlank()) {
                Text(
                    "${m.stock_ant_nombre ?: "—"} → ${m.stock_new_nombre ?: "—"}",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF495057),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Por ${m.actor_nombre ?: "—"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                if (!m.nota.isNullOrBlank()) Text(m.nota, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

private fun etiquetaDia(creadoEn: String?): String {
    if (creadoEn.isNullOrBlank() || creadoEn.length < 10) return "—"
    return try {
        val fecha = LocalDate.parse(creadoEn.take(10))
        when (fecha) {
            LocalDate.now() -> "Hoy"
            LocalDate.now().minusDays(1) -> "Ayer"
            else -> fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM"))
        }
    } catch (e: Exception) { creadoEn.take(10) }
}
