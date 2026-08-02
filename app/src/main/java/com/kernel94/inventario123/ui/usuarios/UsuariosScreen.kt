package com.kernel94.inventario123.ui.usuarios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.ui.theme.BsPrimary
import kotlinx.coroutines.launch

private val TIPOS_USUARIO = listOf("admin", "coordinador", "fs", "ati")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(viewModel: UsuariosViewModel, onVolver: () -> Unit) {
    LaunchedEffect(Unit) { viewModel.cargar() }
    var usuarioEnEdicion by remember { mutableStateOf<Usuario?>(null) }
    var mostrarFormulario by remember { mutableStateOf(false) }
    var confirmarEliminarId by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Usuarios") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsPrimary, titleContentColor = androidx.compose.ui.graphics.Color.White, navigationIconContentColor = androidx.compose.ui.graphics.Color.White),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { usuarioEnEdicion = null; mostrarFormulario = true }, containerColor = BsPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo usuario", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                viewModel.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(viewModel.error!!) }
                else -> LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.usuarios) { usuario ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(usuario.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                                    Text(usuario.tipo.uppercase(), style = MaterialTheme.typography.labelSmall, color = BsPrimary)
                                }
                                IconButton(onClick = { usuarioEnEdicion = usuario; mostrarFormulario = true }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                                IconButton(onClick = { confirmarEliminarId = usuario.id }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        FormularioUsuarioDialog(
            viewModel = viewModel,
            usuario = usuarioEnEdicion,
            onCerrar = { mostrarFormulario = false },
            onGuardado = { ok, msg ->
                mostrarFormulario = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    if (confirmarEliminarId != null) {
        AlertDialog(
            onDismissRequest = { confirmarEliminarId = null },
            title = { Text("¿Eliminar este usuario?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = confirmarEliminarId!!
                    confirmarEliminarId = null
                    viewModel.eliminarUsuario(id) { _, msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminarId = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioUsuarioDialog(
    viewModel: UsuariosViewModel,
    usuario: Usuario?,
    onCerrar: () -> Unit,
    onGuardado: (Boolean, String) -> Unit,
) {
    var nombre by remember { mutableStateOf(usuario?.nombre ?: "") }
    var email by remember { mutableStateOf(usuario?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(usuario?.tipo ?: "fs") }
    val plazasSeleccionadas = remember { mutableStateListOf<Int>().apply { usuario?.plaza_id?.let { add(it) } } }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (usuario == null) "Nuevo usuario" else "Editar usuario") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text(if (usuario == null) "Contraseña" else "Nueva contraseña (opcional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                Text("Tipo de usuario", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                Row {
                    TIPOS_USUARIO.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            RadioButton(selected = tipo == t, onClick = { tipo = t })
                            Text(t)
                        }
                    }
                }

                Text("Plazas (marca todas las que apliquen, puede ser más de una y de negocios distintos)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                viewModel.catalogos.plazas.forEach { plaza ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = plazasSeleccionadas.contains(plaza.id),
                            onCheckedChange = { marcado ->
                                if (marcado) plazasSeleccionadas.add(plaza.id) else plazasSeleccionadas.remove(plaza.id)
                            }
                        )
                        Text("${plaza.nombre} (${plaza.negocio_nombre ?: ""})")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.guardarUsuario(
                    id = usuario?.id, nombre = nombre, email = email,
                    password = password.ifBlank { null }, tipo = tipo,
                    plazaIds = plazasSeleccionadas.toList(), onListo = onGuardado,
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
