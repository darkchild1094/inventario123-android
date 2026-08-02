package com.kernel94.inventario123.ui.listado.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.ui.theme.BsBorder

@Composable
fun ActivoCard(
    activo: Activo,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BsBorder),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    activo.dispositivo_nombre ?: "Equipo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(activo)
            }
            Spacer(Modifier.height(6.dp))
            Text("Serie: ${activo.serie ?: "—"}", style = MaterialTheme.typography.bodyMedium)
            if (!activo.placa.isNullOrBlank()) {
                Text("Placa: ${activo.placa}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(activo.modelo_nombre ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Store, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(4.dp))
                Text(activo.plaza_nombre ?: "—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (activo.stock_tipo == "usuario") Icons.Filled.Person else Icons.Filled.Warehouse,
                    contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(4.dp))
                Text(activo.asignadoOBodega, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }

            if (activo.puedeEditar || activo.puedeEliminar) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (activo.puedeEditar) {
                        IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                    }
                    if (activo.puedeEliminar) {
                        IconButton(onClick = onEliminar) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                    }
                }
            }
        }
    }
}
