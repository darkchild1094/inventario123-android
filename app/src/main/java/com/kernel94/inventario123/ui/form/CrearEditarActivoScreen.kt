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
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.ui.listado.components.FiltroDropdown
import com.kernel94.inventario123.ui.theme.BsPrimary

private val ESTATUS_OPCIONES = listOf(
    "en_bodega" to "En Bodega", "en_uso" to "En Uso", "baja" to "Baja",
    "garantia" to "Garantía", "asignado" to "Asignado",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearEditarActivoScreen(
    viewModel: CrearEditarActivoViewModel,
    idActivoAEditar: Int?,
    onVolver: () -> Unit,
    onAbrirEscanerSerie: () -> Unit,
    onAbrirEscanerPlaca: () -> Unit,
    serieEscaneada: String?,
    placaEscaneada: String?,
    onSerieConsumida: () -> Unit,
    onPlacaConsumida: () -> Unit,
) {
    LaunchedEffect(idActivoAEditar) { viewModel.iniciar(idActivoAEditar) }

    LaunchedEffect(serieEscaneada) {
        if (!serieEscaneada.isNullOrBlank()) {
            viewModel.serie = serieEscaneada
            onSerieConsumida()
        }
    }

    LaunchedEffect(placaEscaneada) {
        if (!placaEscaneada.isNullOrBlank()) {
            viewModel.placa = placaEscaneada
            onPlacaConsumida()
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
                value = viewModel.placa,
                onValueChange = { viewModel.placa = it },
                label = { Text("Placa / Activo Fijo") },
                trailingIcon = {
                    IconButton(onClick = onAbrirEscanerPlaca) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear placa")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            Text("Estatus", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
            Column {
                ESTATUS_OPCIONES.forEach { (valor, etiqueta) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = viewModel.status == valor, onClick = { viewModel.status = valor })
                        Text(etiqueta)
                    }
                }
            }

            // Campos condicionales, igual que manejarEstatus() en la web
            if (viewModel.requiereAsignadoUsuario()) {
                FiltroDropdown(
                    etiqueta = "Asignado a", opciones = viewModel.usuariosAsignables,
                    seleccionId = viewModel.asignadoUsuarioId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { viewModel.asignadoUsuarioId = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
            if (viewModel.requiereTiendaUso()) {
                FiltroDropdown(
                    etiqueta = "Tienda en uso", opciones = viewModel.catalogos.tiendas.filter { it.plaza_id == viewModel.plazaId },
                    seleccionId = viewModel.tiendaUsoId, idDe = { it.id }, nombreDe = { it.nombre },
                    onSeleccion = { viewModel.tiendaUsoId = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            FiltroDropdown(
                etiqueta = "Procedencia (tienda de origen)", opciones = viewModel.catalogos.tiendas.filter { it.plaza_id == viewModel.plazaId },
                seleccionId = viewModel.procedenciaTiendaId, idDe = { it.id }, nombreDe = { it.nombre },
                onSeleccion = { viewModel.procedenciaTiendaId = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.guardar(onExito = {}) },
                enabled = !viewModel.guardando,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BsPrimary)
            ) {
                if (viewModel.guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (idActivoAEditar == null) "Guardar y registrar otro" else "Guardar cambios")
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
