package com.kernel94.inventario123.ui.usuarios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Usuario
import com.kernel94.inventario123.data.model.rolLabel
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

private val TIPOS = listOf("admin", "coordinador", "ati", "pfs")

private val DESCRIPCION_ROL = mapOf(
    "admin"       to "Todo, en todas las plazas. Incluye usuarios y catálogos.",
    "coordinador" to "Recibe y entrega material: Bodega editable. Ve el stock de sus ingenieros.",
    "ati"         to "Su stock y las tiendas. Bodega y stock de ingenieros, solo lectura.",
    "pfs"         to "Su stock y las tiendas donde trabaja. No ve Bodega.",
)

/**
 * Alta y edición de usuario, en pantalla completa.
 *
 * Antes era un AlertDialog: con tres campos, cuatro roles y una plaza por
 * negocio, el contenido no cabía en la altura que un diálogo concede y los
 * roles —puestos en un Row sin envolver— se salían de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioFormScreen(
    viewModel: UsuariosViewModel,
    usuarioId: Int?,
    onVolver: () -> Unit,
    onGuardado: (String) -> Unit,
) {
    LaunchedEffect(Unit) { if (viewModel.usuarios.isEmpty()) viewModel.cargar() }

    val usuario: Usuario? = remember(usuarioId, viewModel.usuarios) {
        usuarioId?.let { id -> viewModel.usuarios.find { it.id == id } }
    }
    val esNuevo = usuarioId == null

    var nombre by remember(usuario) { mutableStateOf(usuario?.nombre ?: "") }
    var email by remember(usuario) { mutableStateOf(usuario?.email ?: "") }
    var password by remember(usuario) { mutableStateOf("") }
    var verPassword by remember { mutableStateOf(false) }
    var tipo by remember(usuario) { mutableStateOf(usuario?.tipo ?: "pfs") }

    // Se premarcan TODAS las plazas asignadas. El servidor borra y reinserta la
    // lista, así que guardar con una lista incompleta le quita plazas al usuario.
    var plazas by remember(usuario) {
        mutableStateOf(
            usuario?.plaza_ids?.toSet()
                ?.ifEmpty { setOfNotNull(usuario.plaza_id) }
                ?: emptySet()
        )
    }

    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val faltaNombre = nombre.isBlank()
    val faltaEmail = email.isBlank() || !email.contains("@")
    val faltaPassword = esNuevo && password.isBlank()
    val faltaPlaza = plazas.isEmpty()
    val puedeGuardar = !faltaNombre && !faltaEmail && !faltaPassword && !faltaPlaza && !guardando

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esNuevo) "Nuevo usuario" else "Editar usuario", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            guardando = true; error = null
                            viewModel.guardarUsuario(
                                id = usuarioId, nombre = nombre.trim(), email = email.trim(),
                                password = password.ifBlank { null }, tipo = tipo,
                                plazaIds = plazas.toList(),
                            ) { ok, msg ->
                                guardando = false
                                if (ok) onGuardado(msg) else error = msg
                            }
                        },
                        enabled = puedeGuardar,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BsPrimary),
                    ) {
                        if (guardando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text(if (esNuevo) "Crear usuario" else "Guardar cambios")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F3F5))
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!esNuevo && usuario == null) {
                Text("No se encontró ese usuario.", color = Color.Gray)
                return@Column
            }

            Seccion("Datos de la persona") {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre completo *") },
                    isError = nombre.isNotEmpty() && faltaNombre,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Correo *") },
                    supportingText = { Text("Con este correo entra a la app") },
                    isError = email.isNotEmpty() && faltaEmail,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }

            Seccion("Contraseña") {
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text(if (esNuevo) "Contraseña *" else "Nueva contraseña") },
                    supportingText = {
                        Text(
                            if (esNuevo) "Mínimo que puedas dictarle a la persona."
                            else "Déjala vacía para no cambiarla."
                        )
                    },
                    visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { verPassword = !verPassword }) {
                            Icon(
                                if (verPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (verPassword) "Ocultar" else "Ver",
                            )
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }

            // Un rol por renglón, con su explicación: en un Row los cuatro se
            // salían de la pantalla y además no se entendía qué daba cada uno.
            Seccion("Rol") {
                TIPOS.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        RadioButton(selected = tipo == t, onClick = { tipo = t })
                        Column(Modifier.padding(top = 12.dp)) {
                            Text(rolLabel(t), fontWeight = FontWeight.SemiBold)
                            Text(
                                DESCRIPCION_ROL[t] ?: "",
                                style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                            )
                        }
                    }
                }
            }

            Seccion("Plazas") {
                Text(
                    "Marca todas donde trabaja. Puede ser más de una, y de negocios distintos.",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                )
                if (faltaPlaza) {
                    Text(
                        "Hace falta al menos una.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                viewModel.catalogos.plazas.forEach { plaza ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = plaza.id in plazas,
                            onCheckedChange = { marcado ->
                                plazas = if (marcado) plazas + plaza.id else plazas - plaza.id
                            },
                        )
                        Text(
                            plaza.negocio_nombre?.let { "${plaza.nombre} · $it" } ?: plaza.nombre,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

/** Bloque con título, para que el formulario se lea por partes y no como una lista larga. */
@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = BsPrimary,
        )
        Surface(color = Color.White, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { contenido() }
        }
    }
}
