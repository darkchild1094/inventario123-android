package com.kernel94.inventario123.ui.movimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernel94.inventario123.data.model.Movimiento
import com.kernel94.inventario123.ui.theme.BsDanger
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsCanvas
import com.kernel94.inventario123.ui.theme.BsPrimary
import com.kernel94.inventario123.ui.theme.BsSuccess
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
        Column(Modifier.padding(padding).fillMaxSize().background(BsCanvas)) {
            TabRow(
                selectedTabIndex = PESTANAS.indexOfFirst { it.first == viewModel.pestana }.coerceAtLeast(0),
                containerColor = Color.White, contentColor = BsPrimary,
                indicator = { tabPositions ->
                    val i = PESTANAS.indexOfFirst { it.first == viewModel.pestana }.coerceAtLeast(0)
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[i]), color = BsPrimary,
                    )
                },
            ) {
                PESTANAS.forEach { (clave, etiqueta) ->
                    val seleccionada = viewModel.pestana == clave
                    Tab(
                        selected = seleccionada,
                        onClick = { viewModel.cambiarPestana(clave) },
                        text = { Text(etiqueta, color = if (seleccionada) BsPrimary else Color(0xFF495057), fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal) },
                    )
                }
            }

            // Barra de búsqueda pill (handoff §3: lavanda bajo las pestañas).
            Surface(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                color = Color(0xFFECE6F0),
                shape = RoundedCornerShape(50),
            ) {
                OutlinedTextField(
                    value = viewModel.busqueda,
                    onValueChange = { viewModel.onBusquedaChange(it) },
                    placeholder = { Text("Buscar por activo, serie o tienda") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF49454F)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!viewModel.cargando && viewModel.error == null) {
                Text(
                    "${viewModel.totalResultados} movimiento(s)", style = MaterialTheme.typography.labelMedium, color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp),
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
                                    dia.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF6C757D), modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(filas, key = { it.id }) { m ->
                                FilaMovimiento(m, esUltimo = m.id == filas.last().id)
                            }
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

/** Color de la pastilla de evento — envíos en azul, altas/instalación en verde, bajas/eliminación en rojo. */
private fun colorEvento(evento: String): Color = when (evento) {
    "baja", "eliminacion" -> BsDanger
    "alta", "reemplazo_entra" -> BsSuccess
    else -> BsPrimary
}

@Composable
private fun FilaMovimiento(m: Movimiento, esUltimo: Boolean) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Riel de línea de tiempo: punto + línea vertical hacia el siguiente.
        Column(Modifier.width(24.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 20.dp).size(10.dp).clip(CircleShape).background(colorEvento(m.evento)))
            if (!esUltimo) Box(Modifier.width(2.dp).weight(1f).background(Color(0xFFDEE2E6)))
        }
        Card(
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = colorEvento(m.evento), shape = RoundedCornerShape(50)) {
                        Text(
                            m.eventoLabel, color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Text(m.creado_en?.drop(11)?.take(5) ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(Modifier.height(4.dp))
                Text(m.equipoTitulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(m.equipoIds, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                if (!m.stock_ant_nombre.isNullOrBlank() || !m.stock_new_nombre.isNullOrBlank()) {
                    Text(
                        "${m.stock_ant_nombre ?: "—"} → ${m.stock_new_nombre ?: "—"}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFF212529),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Por ${m.actor_nombre ?: "—"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    if (!m.nota.isNullOrBlank()) Text(m.nota, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                }
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
