package com.kernel94.inventario123.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.data.model.Plaza

/**
 * Pestañas de "mi plaza actual" para los formularios de alta: evita que el
 * usuario tenga que elegir Unidad de negocio + Plaza a mano cada vez — solo
 * aparece si tiene más de una plaza asignada (usuario_plaza en el backend).
 */
@Composable
fun PlazaTabs(
    opciones: List<Plaza>,
    seleccionId: Int?,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indiceSeleccionado = opciones.indexOfFirst { it.id == seleccionId }.let { if (it >= 0) it else 0 }
    ScrollableTabRow(selectedTabIndex = indiceSeleccionado, modifier = modifier, edgePadding = 0.dp) {
        opciones.forEachIndexed { i, plaza ->
            Tab(
                selected = i == indiceSeleccionado,
                onClick = { onSeleccion(plaza.id) },
                text = {
                    Text(
                        if (plaza.negocio_nombre != null) "${plaza.nombre} (${plaza.negocio_nombre})" else plaza.nombre,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                },
            )
        }
    }
}
