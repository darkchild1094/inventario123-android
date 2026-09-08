package com.kernel94.inventario123.ui.consulta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.ConsultaCoincidencia
import com.kernel94.inventario123.data.model.ConsultaResponse
import com.kernel94.inventario123.data.model.Movimiento
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultaScreen(
    viewModel: ConsultaViewModel,
    onVolver: () -> Unit,
    onAbrirDetalle: (Int) -> Unit,
    onAbrirEscaner: () -> Unit = {},
    serieEscaneada: String? = null,
    onSerieConsumida: () -> Unit = {},
) {
    LaunchedEffect(serieEscaneada) {
        if (!serieEscaneada.isNullOrBlank()) {
            viewModel.texto = serieEscaneada
            onSerieConsumida()
            viewModel.consultar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consulta", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
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
            Text(
                "Escanea o teclea la serie, el código de barras o el N° de activo para saber a qué tienda pertenece.",
                style = MaterialTheme.typography.bodySmall, color = Color.Gray,
            )
            OutlinedTextField(
                value = viewModel.texto,
                onValueChange = { viewModel.texto = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Serie / código / N° de activo") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onAbrirEscaner) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear")
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.consultar() }),
            )
            Button(onClick = { viewModel.consultar() }, modifier = Modifier.fillMaxWidth()) {
                Text("Consultar")
            }

            when {
                viewModel.cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                viewModel.error != null -> Text(viewModel.error!!, color = MaterialTheme.colorScheme.error)
                viewModel.resultado != null -> ResultadoConsulta(viewModel.resultado!!, onAbrirDetalle)
            }
        }
    }
}

@Composable
private fun ResultadoConsulta(res: ConsultaResponse, onAbrirDetalle: (Int) -> Unit) {
    if (!res.encontrado && res.activo == null && res.coincidencias.isEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))) {
            Text(res.message ?: "Sin coincidencias.", Modifier.padding(14.dp), color = Color(0xFF664D03))
        }
        return
    }

    val a = res.activo
    if (a != null) {
        val u = res.ubicacion
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    listOfNotNull(a.dispositivo_nombre, a.marca_nombre, a.modelo_nombre).joinToString(" · ").ifBlank { "Equipo" },
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Serie: ${a.serie ?: "—"}  ·  CB: ${a.codigoBarras ?: "—"}  ·  N° activo: ${a.numActivo ?: "—"}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                )
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Filita("Ubicación", u?.resumen ?: "—", negrita = true)
                Filita("Tienda", u?.tienda_stock ?: u?.tienda_uso ?: "—")
                Filita("Bodega", u?.bodega ?: "—")
                Filita("Con", u?.usuario ?: "—")
                Filita("Plaza", u?.plaza_nombre ?: "—")
                Filita("Región / Negocio", "${u?.region_nombre ?: "—"} / ${u?.negocio_nombre ?: "—"}")
                Filita("Estatus", (u?.status ?: a.status))
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { onAbrirDetalle(a.id) }) { Text("Ver ficha completa") }
            }
        }
        if (res.historial.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Historial de movimientos", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    res.historial.forEach { MovLinea(it) }
                }
            }
        }
        return
    }

    // Varias coincidencias
    Text("${res.coincidencias.size} coincidencias:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(res.coincidencias) { c -> CoincidenciaRow(c, onAbrirDetalle) }
    }
}

@Composable
private fun CoincidenciaRow(c: ConsultaCoincidencia, onAbrirDetalle: (Int) -> Unit) {
    Card(
        onClick = { onAbrirDetalle(c.id) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                listOfNotNull(c.dispositivo_nombre, c.marca_nombre, c.modelo_nombre).joinToString(" · ").ifBlank { "Equipo" },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Serie ${c.serie ?: "—"} · CB ${c.codigo_barras ?: "—"} · ${c.ubicacion_corta ?: ""}",
                style = MaterialTheme.typography.bodySmall, color = Color.Gray,
            )
        }
    }
}

@Composable
private fun Filita(k: String, v: String, negrita: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(v, style = MaterialTheme.typography.bodySmall, fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun MovLinea(m: Movimiento) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            (Movimiento.EVENTOS[m.evento] ?: m.evento),
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
        )
        Text(
            listOfNotNull(m.creado_en?.take(16), m.actor_nombre).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall, color = Color.Gray,
        )
        m.nota?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
