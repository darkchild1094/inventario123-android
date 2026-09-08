package com.kernel94.inventario123.ui.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.remote.Urls
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary

private val ESTATUS_OPCIONES = listOf(
    "en_bodega" to "En Bodega", "en_uso" to "En Uso", "baja" to "Baja",
    "garantia" to "Garantía", "asignado" to "Asignado",
)

private val SALIDA_OPCIONES = listOf(
    "asignado" to "Asignar a usuario", "en_bodega" to "Enviar a bodega",
    "garantia" to "Garantía", "baja" to "Baja",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearEditarActivoScreen(
    viewModel: CrearEditarActivoViewModel,
    idActivoAEditar: Int?,
    onVolver: () -> Unit,
    onAbrirEscanerSerie: () -> Unit,
    onAbrirEscanerCodigo: () -> Unit,
    serieEscaneada: String?,
    codigoEscaneado: String?,
    onSerieConsumida: () -> Unit,
    onCodigoConsumido: () -> Unit,
    moduloContexto: String? = null,
    tiendaUsoContexto: Int? = null,
) {
    LaunchedEffect(idActivoAEditar) { viewModel.iniciar(idActivoAEditar, moduloContexto, tiendaUsoContexto) }

    LaunchedEffect(serieEscaneada) {
        if (!serieEscaneada.isNullOrBlank()) {
            viewModel.serie = serieEscaneada
            onSerieConsumida()
        }
    }

    LaunchedEffect(codigoEscaneado) {
        if (!codigoEscaneado.isNullOrBlank()) {
            viewModel.codigoBarras = codigoEscaneado
            onCodigoConsumido()
        }
    }

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
                title = { Text(if (idActivoAEditar == null) "Registrar activo" else "Editar activo") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = androidx.compose.ui.graphics.Color.White, navigationIconContentColor = androidx.compose.ui.graphics.Color.White),
            )
        }
    ) { padding ->
        if (viewModel.cargando) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val online by viewModel.online.collectAsState()
            if (!online && viewModel.idEdicion == null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Text(
                        "Sin conexión: el activo se guardará en la cola y se enviará solo al recuperar internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            // Negocio / Plaza (solo si el rol puede elegir, igual que la web)
            if (viewModel.perfil?.permisos?.puedeFiltrarPorPlaza == true) {
                FiltroDropdown(
                    etiqueta = "Unidad de negocio", opciones = viewModel.catalogos.negocios,
                    seleccionId = viewModel.negocioId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { viewModel.onNegocioChange(it) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                FiltroDropdown(
                    etiqueta = "Plaza", opciones = viewModel.plazasFiltradas,
                    seleccionId = viewModel.plazaId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { viewModel.onPlazaChange(it) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            FiltroDropdown(
                etiqueta = "Dispositivo", opciones = viewModel.catalogos.dispositivos,
                seleccionId = viewModel.dispositivoId, idDe = { it.id }, nombreDe = { it.nombre },
                onSeleccion = { viewModel.onDispositivoChange(it) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            FiltroDropdown(
                etiqueta = "Modelo", opciones = viewModel.modelosFiltrados,
                seleccionId = viewModel.modeloId, idDe = { it.id }, nombreDe = { it.nombre },
                onSeleccion = { viewModel.modeloId = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            OutlinedTextField(
                value = viewModel.serie,
                onValueChange = { viewModel.serie = it },
                label = { Text("Serie *") },
                trailingIcon = {
                    IconButton(onClick = onAbrirEscanerSerie) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear serie")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            OutlinedTextField(
                value = viewModel.codigoBarras,
                onValueChange = { viewModel.codigoBarras = it },
                label = { Text("Código de barras") },
                trailingIcon = {
                    IconButton(onClick = onAbrirEscanerCodigo) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código de barras")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            OutlinedTextField(
                value = viewModel.numActivo,
                onValueChange = { viewModel.numActivo = it },
                label = { Text("N° de activo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            // Módulos compactos (bodega / mi_stock / stock_pfs / ati): el estatus
            // queda fijo por el módulo, no se muestran los radios.
            val estatusFijoPorModulo = moduloContexto in listOf("bodega", "mi_stock", "stock_pfs", "ati")
            if (estatusFijoPorModulo) {
                val txt = when (moduloContexto) {
                    "bodega" -> "Destino: En bodega"
                    "mi_stock" -> "Destino: A mi stock"
                    "stock_pfs" -> "Destino: Stock de ingeniero (PFS)"
                    "ati" -> "Destino: Stock de ATI"
                    else -> ""
                }
                Text(txt, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp))
            } else {
                Text("Estatus", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                // Un ingeniero (pfs) editando un activo suyo 'asignado' NO puede mandarlo
                // a bodega directo: debe usar "Traslados a bodega" (firma del coordinador).
                val ocultarEnBodega = viewModel.perfil?.permisos?.tipo == "pfs" &&
                    viewModel.idEdicion != null && viewModel.status == "asignado"
                Column {
                    ESTATUS_OPCIONES.forEach { (valor, etiqueta) ->
                        if (ocultarEnBodega && valor == "en_bodega") return@forEach
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = viewModel.status == valor, onClick = { viewModel.onStatusChange(valor) })
                            Text(etiqueta)
                        }
                    }
                }
                if (ocultarEnBodega) {
                    Text(
                        "Para mandar este equipo a bodega usa \"Traslados a bodega\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = BsPrimary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                    )
                }
            }

            // Campos condicionales, igual que manejarEstatus() / ActivoGuardado en la web.
            // En "mi_stock" el destino soy yo -> no se muestra el selector de usuario.
            if (viewModel.requiereAsignadoUsuario() && moduloContexto != "mi_stock") {
                FiltroDropdown(
                    etiqueta = if (moduloContexto == "stock_pfs" || moduloContexto == "ati") "Ingeniero *" else "Asignado a",
                    opciones = if (moduloContexto == "stock_pfs") viewModel.usuariosAsignables.filter { it.tipo == "pfs" }
                               else if (moduloContexto == "ati") viewModel.usuariosAsignables.filter { it.tipo == "ati" }
                               else viewModel.usuariosAsignables,
                    seleccionId = viewModel.asignadoUsuarioId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { viewModel.asignadoUsuarioId = it },
                    etiquetaNula = "Yo mismo",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            if (viewModel.requiereTiendaUso()) {
                FiltroDropdown(
                    etiqueta = "Tienda en uso *", opciones = viewModel.catalogos.tiendas.filter { it.plaza_id == viewModel.plazaId },
                    seleccionId = viewModel.tiendaUsoId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { viewModel.onTiendaUsoChange(it) },
                    etiquetaNula = "Seleccione tienda...",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                if (viewModel.tiendaUsoId != null && viewModel.dispositivoId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = viewModel.reemplazoOtraCategoria,
                            onCheckedChange = { viewModel.onReemplazoOtraCategoriaChange(it) }
                        )
                        Text("Reemplazar equipo de otra categoría", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (viewModel.reemplazosDisponibles.isNotEmpty()) {
                    FiltroDropdown(
                        etiqueta = "¿Reemplaza a?",
                        opciones = viewModel.reemplazosDisponibles,
                        seleccionId = viewModel.reemplazaActivoId, idDe = { it.id },
                        nombreDe = {
                            val marcaModelo = listOfNotNull(it.marca_nombre, it.modelo_nombre).joinToString(" ")
                            val prefijo = if (it.dispositivo_id != null && it.dispositivo_id != viewModel.dispositivoId && it.dispositivo_nombre != null)
                                "[${it.dispositivo_nombre}] " else ""
                            "$prefijo$marcaModelo · ${it.codigoBarras ?: "(sin código de barras)"}"
                        },
                        onSeleccion = { viewModel.onReemplazaChange(it) },
                        etiquetaNula = "— Ninguno (equipo adicional) —",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    if (viewModel.reemplazaActivoId != null) {
                        Text(
                            "Equipo que sale — destino",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.salidaSerie,
                            onValueChange = { viewModel.salidaSerie = it },
                            label = { Text("Serie del equipo que sale") },
                            supportingText = { Text("Corrige si está mal.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.salidaCodigoBarras,
                            onValueChange = { viewModel.salidaCodigoBarras = it },
                            label = { Text("Código de barras del equipo que sale") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        Column {
                            SALIDA_OPCIONES.forEach { (valor, etiqueta) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = viewModel.salidaDestino == valor,
                                        onClick = { viewModel.salidaDestino = valor }
                                    )
                                    Text(etiqueta)
                                }
                            }
                        }
                        when (viewModel.salidaDestino) {
                            "asignado" -> FiltroDropdown(
                                etiqueta = "Usuario que recibe", opciones = viewModel.usuariosAsignables,
                                seleccionId = viewModel.salidaUsuarioId, idDe = { it.id }, nombreDe = { it.nombre },
                                onSeleccion = { viewModel.salidaUsuarioId = it },
                                etiquetaNula = "Yo mismo",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                            "garantia", "baja" -> FiltroDropdown(
                                etiqueta = "ATI responsable del que sale", opciones = viewModel.atisPlaza,
                                seleccionId = viewModel.salidaAtiUsuarioId, idDe = { it.id }, nombreDe = { it.nombre },
                                onSeleccion = { viewModel.salidaAtiUsuarioId = it },
                                etiquetaNula = "ATI de la tienda (automático)",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            if (viewModel.requiereAti()) {
                FiltroDropdown(
                    etiqueta = "ATI responsable", opciones = viewModel.atisPlaza,
                    seleccionId = viewModel.atiUsuarioId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { viewModel.atiUsuarioId = it },
                    etiquetaNula = "ATI de la tienda (automático)",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                Text(
                    "Garantía y baja quedan en el stock de este ATI.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            FiltroDropdown(
                etiqueta = "Procedencia (tienda de origen)", opciones = viewModel.catalogos.tiendas.filter { it.plaza_id == viewModel.plazaId },
                seleccionId = viewModel.procedenciaTiendaId, idDe = { it.id }, nombreDe = { it.nombre },
                onSeleccion = { viewModel.procedenciaTiendaId = it },
                etiquetaNula = "¿De qué tienda proviene?",
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            OutlinedTextField(
                value = viewModel.motivo,
                onValueChange = { viewModel.motivo = it.take(255) },
                label = { Text("Motivo del movimiento (opcional)") },
                supportingText = { Text("Se guarda en el historial junto con este movimiento.") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            Text("Fotos (opcional)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            FotoActivoCampo(
                etiqueta = "Foto del equipo",
                urlActual = viewModel.fotoEquipoActual?.let { Urls.activoFotoThumb(it) },
                uriSeleccionada = viewModel.fotoEquipoUri,
                onCambio = { viewModel.fotoEquipoUri = it },
                modifier = Modifier.fillMaxWidth()
            )
            FotoActivoCampo(
                etiqueta = "Foto de la serie",
                urlActual = viewModel.fotoSerieActual?.let { Urls.activoFotoThumb(it) },
                uriSeleccionada = viewModel.fotoSerieUri,
                onCambio = { viewModel.fotoSerieUri = it },
                modifier = Modifier.fillMaxWidth()
            )
            FotoActivoCampo(
                etiqueta = "Foto del código de barras",
                urlActual = viewModel.fotoActivoActual?.let { Urls.activoFotoThumb(it) },
                uriSeleccionada = viewModel.fotoActivoUri,
                onCambio = { viewModel.fotoActivoUri = it },
                modifier = Modifier.fillMaxWidth()
            )

            val context = LocalContext.current
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.guardar(context, onExito = {}) },
                enabled = !viewModel.guardando,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BsPrimary)
            ) {
                if (viewModel.guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        when {
                            idActivoAEditar != null -> "Guardar cambios"
                            !online -> "Guardar sin conexión"
                            else -> "Guardar y registrar otro"
                        }
                    )
                }
            }
            if (idActivoAEditar != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
