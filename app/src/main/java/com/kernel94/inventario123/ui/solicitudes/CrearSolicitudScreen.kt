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
import com.kernel94.inventario123.ui.firma.FirmaCanvas
import com.kernel94.inventario123.ui.firma.rememberFirmaState
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearSolicitudScreen(
    viewModel: SolicitudesViewModel,
    onVolver: () -> Unit,
    onEnviada: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.cargarFormulario() }
    val seleccion = remember { mutableStateListOf<Int>() }
    var bodegaId by remember { mutableStateOf<Int?>(null) }
    var nota by remember { mutableStateOf("") }
    val firma = rememberFirmaState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.bodegas) {
        if (bodegaId == null) bodegaId = viewModel.bodegas.firstOrNull()?.id
    }

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
            when {
                viewModel.cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                viewModel.misAsignados.isEmpty() -> Text("No tienes activos asignados para trasladar.", color = Color.Gray)
                else -> {
                    Text("Activos a trasladar", fontWeight = FontWeight.Bold)
                    viewModel.misAsignados.forEach { a ->
                        val marcado = seleccion.contains(a.id)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = marcado,
                                onCheckedChange = { if (it) seleccion.add(a.id) else seleccion.remove(a.id) }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    listOfNotNull(a.marca_nombre, a.modelo_nombre).joinToString(" ").ifBlank { "Sin modelo" },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${a.dispositivo_nombre ?: "—"} · Serie: ${a.serie ?: "ninguna"} · Cód: ${a.codigoBarras ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    FiltroDropdown(
                        etiqueta = "Bodega destino",
                        opciones = viewModel.bodegas,
                        seleccionId = bodegaId,
                        idDe = { it.id },
                        nombreDe = { it.nombre },
                        onSeleccion = { bodegaId = it },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = nota, onValueChange = { nota = it },
                        label = { Text("Motivo del traslado") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                    )

                    Text("Tu firma", fontWeight = FontWeight.Bold)
                    FirmaCanvas(firma, Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            val png = firma.exportarPng()
                            if (seleccion.isEmpty()) { scope.launch { snackbar.showSnackbar("Selecciona al menos un activo.") }; return@Button }
                            if (png == null) { scope.launch { snackbar.showSnackbar("Falta tu firma.") }; return@Button }
                            viewModel.crear(seleccion.toList(), bodegaId ?: 0, nota, png) { ok, msg ->
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
    }
}
