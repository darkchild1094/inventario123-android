package com.kernel94.inventario123.ui.listado.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/** Dropdown genérico reutilizado por los 5 filtros (Negocio/Región/Plaza/Usuario/Status) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FiltroDropdown(
    etiqueta: String,
    opciones: List<T>,
    seleccionId: Int?,
    idDe: (T) -> Int,
    nombreDe: (T) -> String,
    onSeleccion: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }
    val textoActual = opciones.firstOrNull { idDe(it) == seleccionId }?.let(nombreDe) ?: "Todos..."

    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }, modifier = modifier) {
        OutlinedTextField(
            value = textoActual,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(text = { Text("Todos...") }, onClick = { onSeleccion(null); expandido = false })
            opciones.forEach { op ->
                DropdownMenuItem(text = { Text(nombreDe(op)) }, onClick = { onSeleccion(idDe(op)); expandido = false })
            }
        }
    }
}
