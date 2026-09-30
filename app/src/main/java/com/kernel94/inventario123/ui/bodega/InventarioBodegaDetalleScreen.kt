package com.kernel94.inventario123.ui.bodega

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.InventarioBodegaDetalle
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioBodegaDetalleScreen(
    viewModel: InventarioBodegaDetalleViewModel,
    inventarioId: Int,
    onVolver: () -> Unit,
    onAbrirEscaner: () -> Unit,
    codigoEscaneado: String?,
    onCodigoConsumido: () -> Unit,
) {
    LaunchedEffect(inventarioId) { viewModel.cargar(inventarioId) }
    LaunchedEffect(codigoEscaneado) {
        codigoEscaneado?.let { viewModel.escanear(it); onCodigoConsumido() }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }
    var detalleParaNota by remember { mutableStateOf<InventarioBodegaDetalle?>(null) }
    var confirmarCierre by remember { mutableStateOf(false) }

    val inv = viewModel.inventario

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(inv?.let { "Inventario ${it.periodo}" } ?: "Inventario") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        },
        floatingActionButton = {
            if (inv?.abierto == true) {
                FloatingActionButton(onClick = onAbrirEscaner, containerColor = BsPrimary) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.cargando && inv == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                inv == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No se pudo cargar el inventario.", color = Color.Gray) }
                else -> {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${inv.total_encontrado} de ${inv.total_esperado} encontrados",
                            fontWeight = FontWeight.Bold,
                        )
                        if (inv.abierto) {
                            Button(
                                onClick = { confirmarCierre = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C757D)),
                            ) { Text("Cerrar inventario") }
                        } else {
                            AssistChip(onClick = {}, label = { Text("Cerrado") })
                        }
                    }

                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                    ) {
                        items(inv.detalle, key = { it.detalle_id }) { d ->
                            DetalleRow(
                                d = d,
                                onClickNota = { if (!d.encontradoBool) detalleParaNota = d },
                            )
                        }
                    }
                }
            }
        }
    }

    detalleParaNota?.let { d ->
        var texto by remember(d.detalle_id) { mutableStateOf(d.nota ?: "") }
        AlertDialog(
            onDismissRequest = { detalleParaNota = null },
            title = { Text("Justificar activo no encontrado") },
            text = {
                Column {
                    Text(
                        listOfNotNull(d.dispositivo_nombre, d.serie ?: d.codigo_barras ?: d.num_activo)
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        label = { Text("¿Dónde está / por qué no aparece?") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.guardarNota(d.detalle_id, texto)
                    detalleParaNota = null
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { detalleParaNota = null }) { Text("Cancelar") } },
        )
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("¿Cerrar inventario?") },
            text = { Text("Ya no se podrán escanear más activos en este inventario. Podrás seguir agregando justificaciones a los faltantes.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarCierre = false
                    viewModel.cerrar { scope.launch { snackbarHostState.showSnackbar("Inventario cerrado.") } }
                }) { Text("Cerrar inventario") }
            },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DetalleRow(d: InventarioBodegaDetalle, onClickNota: () -> Unit) {
    val encontrado = d.encontradoBool
    val fondo = if (encontrado) Color(0xFFD1E7DD) else Color(0xFFF8D7DA)
    Card(
        onClick = onClickNota,
        colors = CardDefaults.cardColors(containerColor = fondo),
        modifier = Modifier.fillMaxWidth().background(fondo, RoundedCornerShape(8.dp)),
    ) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (encontrado) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (encontrado) Color(0xFF198754) else Color(0xFFDC3545),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(d.dispositivo_nombre, d.marca_nombre, d.modelo_nombre).joinToString(" "),
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    listOfNotNull(
                        d.serie?.let { "Serie $it" },
                        d.codigo_barras?.let { "CB $it" },
                        d.num_activo?.let { "N° $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = Color.DarkGray,
                )
                if (!encontrado && !d.nota.isNullOrBlank()) {
                    Text("Nota: ${d.nota}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF842029))
                }
            }
        }
    }
}
