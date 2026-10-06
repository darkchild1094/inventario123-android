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

private val TIPOS_USUARIO = listOf("admin", "coordinador", "pfs", "ati")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(
    viewModel: UsuariosViewModel,
    onVolver: () -> Unit,
    /** Abre el formulario en pantalla completa; null = alta nueva. */
    onEditarUsuario: (Int?) -> Unit = {},
    mensajeInicial: String? = null,
) {
    LaunchedEffect(Unit) { viewModel.cargar() }
    var confirmarEliminarId by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(mensajeInicial) {
        mensajeInicial?.let { snackbarHostState.showSnackbar(it); viewModel.cargar() }
    }

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
            FloatingActionButton(onClick = { onEditarUsuario(null) }, containerColor = BsPrimary) {
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
                                IconButton(onClick = { onEditarUsuario(usuario.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                                IconButton(onClick = { confirmarEliminarId = usuario.id }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                            }
                        }
                    }
                }
            }
        }
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
