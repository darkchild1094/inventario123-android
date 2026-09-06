package com.kernel94.inventario123.ui.solicitudes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.ui.firma.FirmaCanvas
import com.kernel94.inventario123.ui.firma.rememberFirmaState
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

private val DESTINOS = listOf(
    "asignado"  to "Traspaso a otro ingeniero",
    "en_bodega" to "Devolución a bodega",
    "baja"      to "Baja",
    "garantia"  to "Garantía",
)
private val AYUDA = mapOf(
    "asignado"  to "Lo firma el ingeniero que recibe.",
    "en_bodega" to "Lo aprueba y firma un coordinador.",
    "baja"      to "Solo lo aprueba y firma el ATI.",
    "garantia"  to "Lo firman el ATI y un coordinador (doble firma).",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearSolicitudScreen(
    viewModel: SolicitudesViewModel,
    onVolver: () -> Unit,
    onEnviada: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.cargarFormulario() }
    val seleccion = remember { mutableStateListOf<Int>() }
    var destino by remember { mutableStateOf("asignado") }
    var origenTienda by remember { mutableStateOf(false) }
    var bodegaId by remember { mutableStateOf<Int?>(null) }
    var tiendaId by remember { mutableStateOf<Int?>(null) }
    var ingenieroId by remember { mutableStateOf<Int?>(null) }
    var nota by remember { mutableStateOf("") }
    val firma = rememberFirmaState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.bodegas) { if (bodegaId == null) bodegaId = viewModel.bodegas.firstOrNull()?.id }
    LaunchedEffect(tiendaId) { tiendaId?.let { viewModel.cargarActivosTienda(it) } }
    LaunchedEffect(origenTienda, destino) { seleccion.clear() }

    val activos: List<Activo> = if (origenTienda) viewModel.activosTienda else viewModel.misAsignados

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Nueva solicitud") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("¿Qué movimiento?", fontWeight = FontWeight.Bold)
            FiltroDropdown(
                etiqueta = "Tipo de movimiento",
                opciones = DESTINOS,
                seleccionId = DESTINOS.indexOfFirst { it.first == destino }.takeIf { it >= 0 },
                idDe = { DESTINOS.indexOf(it) },
                nombreDe = { it.second },
                onSeleccion = { idx -> idx?.let { destino = DESTINOS[it].first } },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(AYUDA[destino] ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Text("¿De dónde sale el equipo?", fontWeight = FontWeight.Bold)
            Row {
                FilterChip(selected = !origenTienda, onClick = { origenTienda = false }, label = { Text("De mi stock") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = origenTienda, onClick = { origenTienda = true }, label = { Text("Instalado en tienda") })
            }
            if (origenTienda) {
                FiltroDropdown(
                    etiqueta = "Tienda de origen",
                    opciones = viewModel.tiendas,
                    seleccionId = tiendaId,
                    idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { tiendaId = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (destino == "en_bodega") {
                FiltroDropdown(
                    etiqueta = "Bodega destino", opciones = viewModel.bodegas,
                    seleccionId = bodegaId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { bodegaId = it }, modifier = Modifier.fillMaxWidth(),
                )
            }
            if (destino == "asignado") {
                FiltroDropdown(
                    etiqueta = "Ingeniero que recibe", opciones = viewModel.ingenieros,
                    seleccionId = ingenieroId, idDe = { it.id }, nombreDe = { "${it.nombre} (${it.tipo})" },
                    onSeleccion = { ingenieroId = it }, modifier = Modifier.fillMaxWidth(),
                )
            }

            Text("Activos", fontWeight = FontWeight.Bold)
            when {
                viewModel.cargando -> CircularProgressIndicator(Modifier.padding(8.dp))
                activos.isEmpty() -> Text(
                    if (origenTienda) "Selecciona una tienda con equipo en uso." else "No tienes equipo asignado.",
                    color = Color.Gray, style = MaterialTheme.typography.bodySmall,
                )
                else -> activos.forEach { a ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = seleccion.contains(a.id),
                            onCheckedChange = { if (it) seleccion.add(a.id) else seleccion.remove(a.id) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(listOfNotNull(a.marca_nombre, a.modelo_nombre).joinToString(" ").ifBlank { "Sin modelo" }, fontWeight = FontWeight.SemiBold)
                            Text("${a.dispositivo_nombre ?: "—"} · Serie: ${a.serie ?: "ninguna"} · Cód: ${a.codigoBarras ?: "—"}",
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }

            OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Motivo") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)

            Text("Tu firma", fontWeight = FontWeight.Bold)
            FirmaCanvas(firma, Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val png = firma.exportarPng()
                    if (seleccion.isEmpty()) { scope.launch { snackbar.showSnackbar("Selecciona al menos un activo.") }; return@Button }
                    if (png == null) { scope.launch { snackbar.showSnackbar("Falta tu firma.") }; return@Button }
                    viewModel.crear(
                        destino = destino,
                        origenTipo = if (origenTienda) "tienda" else "asignado",
                        activos = seleccion.toList(), nota = nota, firmaPng = png,
                        bodegaId = bodegaId, tiendaId = tiendaId, ingenieroId = ingenieroId,
                    ) { ok, msg ->
                        scope.launch { snackbar.showSnackbar(msg) }
                        if (ok) onEnviada()
                    }
                },
                enabled = !viewModel.enviando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (viewModel.enviando) "Enviando…" else "Enviar solicitud")
            }
        }
    }
}
