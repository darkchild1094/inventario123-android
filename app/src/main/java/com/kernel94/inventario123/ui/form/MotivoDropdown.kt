package com.kernel94.inventario123.ui.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.kernel94.inventario123.data.model.MOTIVOS_MOVIMIENTO

/** Select de motivo/razón del movimiento. El texto elegido se guarda en movimiento.nota. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivoDropdown(
    seleccion: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = "Motivo",
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = seleccion.ifBlank { "—" },
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            MOTIVOS_MOVIMIENTO.forEach { m ->
                DropdownMenuItem(text = { Text(m) }, onClick = { onSeleccion(m); abierto = false })
            }
        }
    }
}
