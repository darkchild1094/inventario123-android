package com.kernel94.inventario123.ui.tiendas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kernel94.inventario123.data.model.InventarioTiendaDetalle
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsCanvas
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioTiendaScreen(
    viewModel: InventarioTiendaViewModel,
    tiendaId: Int,
    nombreTienda: String,
    onFinalizado: (String) -> Unit,
    onCancelado: () -> Unit,
    onAgregarActivo: () -> Unit,
) {
    LaunchedEffect(tiendaId) { viewModel.cargar(tiendaId) }
    // Al volver de "Agregar activo" (CrearEditarActivoScreen) la pantalla se
    // reanuda: ahí se engancha al checklist lo que se haya dado de alta en la
    // tienda desde que se abrió (ver sincronizarNuevos — no depende de recibir
    // un id porque esa alta puede quedar encolada offline).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var primeraVez = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (primeraVez) primeraVez = false else viewModel.sincronizarNuevos(tiendaId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel.mensaje) {
        viewModel.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }
    var confirmarCancelar by remember { mutableStateOf(false) }
    var confirmarFinalizar by remember { mutableStateOf(false) }
    val inv = viewModel.inventario

    fun pedirCancelar() { if (inv?.abierto == true) confirmarCancelar = true else onCancelado() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inventario", color = Color.White)
                        Text(nombreTienda, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .75f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { pedirCancelar() }) { Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = Color.White) }
                },
                actions = {
                    if (inv?.abierto == true) {
                        IconButton(onClick = { confirmarFinalizar = true }) {
                            Icon(Icons.Filled.Check, contentDescription = "Finalizar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        },
        floatingActionButton = {
            if (inv?.abierto == true) {
                ExtendedFloatingActionButton(onClick = onAgregarActivo, containerColor = BsPrimary, contentColor = Color.White) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar activo")
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(BsCanvas)) {
            when {
                viewModel.cargando && inv == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BsPrimary) }
                inv == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No se pudo abrir el inventario.", color = Color.Gray) }
                else -> {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${inv.total_encontrado} de ${inv.total_esperado} revisados", fontWeight = FontWeight.Bold)
                        if (!inv.abierto) AssistChip(onClick = {}, label = { Text("Cerrado") })
                    }

                    if (inv.abierto) {
                        var codigoManual by remember { mutableStateOf("") }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = codigoManual,
                                onValueChange = { codigoManual = it },
                                label = { Text("Escanear o teclear serie / N° activo") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                                    if (codigoManual.isNotBlank()) { viewModel.escanear(codigoManual.trim()); codigoManual = "" }
                                }),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                if (codigoManual.isNotBlank()) { viewModel.escanear(codigoManual.trim()); codigoManual = "" }
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = BsPrimary)
                            }
                        }
                    }

                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 90.dp),
                    ) {
                        items(inv.detalle, key = { it.detalle_id }) { d ->
                            FilaChecklist(
                                d = d,
                                soloLectura = !inv.abierto,
                                onMarcarEncontrado = { viewModel.marcarEncontrado(d.detalle_id) },
                                onMarcarNoEncontrado = { viewModel.pedirNota(d.detalle_id) },
                            )
                        }
                    }
                }
            }
        }
    }

    viewModel.detalleParaNota?.let { detalleId ->
        val fila = inv?.detalle?.firstOrNull { it.detalle_id == detalleId }
        var texto by remember(detalleId) { mutableStateOf(fila?.nota ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.cerrarDialogNota() },
            title = { Text("Comentarios") },
            text = {
                Column {
                    if (fila != null) {
                        Text(
                            listOfNotNull(fila.dispositivo_nombre, fila.marca_nombre, fila.modelo_nombre).joinToString(" "),
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            listOfNotNull(
                                fila.serie?.let { "Serie $it" }, fila.codigo_barras?.let { "CB $it" }, fila.num_activo?.let { "N° $it" },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = texto, onValueChange = { texto = it },
                        placeholder = { Text("Retirado por daño…") },
                        label = { Text("¿Por qué no está? (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.guardarNoEncontrado(detalleId, texto) }) { Text("Marcar no encontrado") }
            },
            dismissButton = { TextButton(onClick = { viewModel.cerrarDialogNota() }) { Text("Cancelar") } },
        )
    }

    if (confirmarFinalizar) {
        AlertDialog(
            onDismissRequest = { confirmarFinalizar = false },
            icon = { Icon(Icons.Filled.Check, contentDescription = null) },
            title = { Text("¿Finalizar inventario?") },
            text = {
                Text(
                    "${inv?.total_encontrado ?: 0} encontrado(s) · ${(inv?.total_esperado ?: 0) - (inv?.total_encontrado ?: 0)} no encontrado(s)."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarFinalizar = false
                    viewModel.finalizar { resumen ->
                        onFinalizado("Inventario guardado: ${resumen.encontrados} encontrado(s), ${resumen.no_encontrados} no encontrado(s).")
                    }
                }) { Text("Finalizar") }
            },
            dismissButton = { TextButton(onClick = { confirmarFinalizar = false }) { Text("Seguir revisando") } },
        )
    }

    if (confirmarCancelar) {
        AlertDialog(
            onDismissRequest = { confirmarCancelar = false },
            title = { Text("¿Cancelar inventario?") },
            text = { Text("Se descarta todo lo revisado en esta visita. No se guarda nada en la tienda.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarCancelar = false
                    viewModel.cancelar(onCancelado)
                }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmarCancelar = false }) { Text("Seguir revisando") } },
        )
    }
}

@Composable
private fun FilaChecklist(
    d: InventarioTiendaDetalle,
    soloLectura: Boolean,
    onMarcarEncontrado: () -> Unit,
    onMarcarNoEncontrado: () -> Unit,
) {
    // 3 estados visuales: gris = sin revisar, verde = encontrado, rojo = no encontrado.
    val fondo = when {
        !d.revisado -> Color(0xFFE9ECEF)
        d.encontradoBool -> Color(0xFFD1E7DD)
        else -> Color(0xFFF8D7DA)
    }
    Card(
        onClick = { if (!soloLectura && d.revisado && !d.encontradoBool) onMarcarNoEncontrado() },
        colors = CardDefaults.cardColors(containerColor = fondo),
        modifier = Modifier.fillMaxWidth().background(fondo, RoundedCornerShape(8.dp)),
    ) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(d.dispositivo_nombre, d.marca_nombre, d.modelo_nombre).joinToString(" "),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    listOfNotNull(
                        d.serie?.let { "Serie $it" }, d.codigo_barras?.let { "CB $it" }, d.num_activo?.let { "N° $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = Color.DarkGray,
                )
                if (!d.encontradoBool && d.revisado && !d.nota.isNullOrBlank()) {
                    Text("Nota: ${d.nota}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF842029))
                }
            }
            if (!soloLectura) {
                IconButton(onClick = onMarcarEncontrado) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Encontrado", tint = if (d.revisado && d.encontradoBool) Color(0xFF198754) else Color.Gray)
                }
                IconButton(onClick = onMarcarNoEncontrado) {
                    Icon(Icons.Filled.Cancel, contentDescription = "No encontrado", tint = if (d.revisado && !d.encontradoBool) Color(0xFFDC3545) else Color.Gray)
                }
            } else {
                Icon(
                    if (d.encontradoBool) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (d.encontradoBool) Color(0xFF198754) else Color(0xFFDC3545),
                )
            }
        }
    }
}
