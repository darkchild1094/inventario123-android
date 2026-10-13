package com.kernel94.inventario123.ui.rentec

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.ProyectoRentec
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentecListaScreen(
    viewModel: RentecListaViewModel,
    onVolver: () -> Unit,
    onAbrirProyecto: (Int) -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.iniciar() }
    var mostrarCrear by remember { mutableStateOf(false) }
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
                title = { Text("RENTEC") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarCrear = true }, containerColor = BsPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo proyecto", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Renovación Tecnológica: crea un proyecto para agrupar el equipo recibido e instalado.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                viewModel.proyectos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aún no hay proyectos RENTEC.", color = Color.Gray) }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                ) {
                    items(viewModel.proyectos, key = { it.id }) { p -> ProyectoCard(p, onClick = { onAbrirProyecto(p.id) }) }
                }
            }
        }
    }

    if (mostrarCrear) {
        var nombre by remember { mutableStateOf("") }
        var plazaId by remember { mutableStateOf<Int?>(null) }
        var fechaInicio by remember { mutableStateOf<String?>(null) }
        var fechaFin by remember { mutableStateOf<String?>(null) }
        var mostrarPickerInicio by remember { mutableStateOf(false) }
        var mostrarPickerFin by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!viewModel.creando) mostrarCrear = false },
            title = { Text("Nuevo proyecto RENTEC") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text("Nombre (ej. \"Renovación CCTV Valles\")") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    FiltroDropdown(
                        etiqueta = "Plaza *", opciones = viewModel.plazas,
                        seleccionId = plazaId, idDe = { it.id }, nombreDe = { it.nombre },
                        onSeleccion = { plazaId = it },
                        etiquetaNula = "Elige la plaza...",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = fechaInicio ?: "", onValueChange = {}, readOnly = true,
                        label = { Text("Fecha de inicio *") },
                        trailingIcon = {
                            IconButton(onClick = { mostrarPickerInicio = true }) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = fechaFin ?: "", onValueChange = {}, readOnly = true,
                        label = { Text("Fecha de fin *") },
                        trailingIcon = {
                            IconButton(onClick = { mostrarPickerFin = true }) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !viewModel.creando,
                    onClick = {
                        viewModel.crear(nombre, plazaId, fechaInicio.orEmpty(), fechaFin.orEmpty()) { id ->
                            mostrarCrear = false; onAbrirProyecto(id)
                        }
                    },
                ) { Text(if (viewModel.creando) "Creando…" else "Crear") }
            },
            dismissButton = { TextButton(onClick = { mostrarCrear = false }, enabled = !viewModel.creando) { Text("Cancelar") } },
        )

        if (mostrarPickerInicio) {
            SelectorFecha(
                onDismiss = { mostrarPickerInicio = false },
                onSeleccion = { fechaInicio = it; mostrarPickerInicio = false },
            )
        }
        if (mostrarPickerFin) {
            SelectorFecha(
                onDismiss = { mostrarPickerFin = false },
                onSeleccion = { fechaFin = it; mostrarPickerFin = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(onDismiss: () -> Unit, onSeleccion: (String) -> Unit) {
    val estado = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { ms ->
                    val fecha = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    onSeleccion(fecha.format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
            }) { Text("Elegir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) { DatePicker(state = estado) }
}

@Composable
private fun ProyectoCard(p: ProyectoRentec, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        listOfNotNull(p.folio, p.plaza_nombre, p.usuario_nombre).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                    )
                    if (p.fecha_inicio != null || p.fecha_fin != null) {
                        Text(
                            "${p.fecha_inicio ?: "?"} → ${p.fecha_fin ?: "?"}",
                            style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                        )
                    }
                }
                AssistChip(
                    onClick = onClick,
                    label = { Text(if (p.abierto) "Abierto" else "Cerrado") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (p.abierto) Color(0xFFFFF3CD) else Color(0xFFE2E3E5)
                    ),
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE9ECEF))
                ) {
                    Box(Modifier.fillMaxWidth(p.avancePct / 100f).fillMaxHeight().background(BsPrimary))
                }
                Spacer(Modifier.width(8.dp))
                Text("${p.instalados} de ${p.recibidos + p.instalados} instalados · ${p.avancePct}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}
