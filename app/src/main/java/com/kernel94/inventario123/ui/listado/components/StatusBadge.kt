package com.kernel94.inventario123.ui.listado.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernel94.inventario123.data.model.Activo
import com.kernel94.inventario123.ui.theme.*

/** Mismo mapeo de colores que _detalle_activo.php / _resultados.php en la web */
fun colorStatus(status: String): Color = when (status) {
    "en_bodega" -> StatusEnBodega
    "en_uso" -> StatusEnUso
    "baja" -> StatusBaja
    "garantia" -> StatusGarantia
    "asignado" -> StatusAsignado
    else -> BsDark
}

@Composable
fun StatusBadge(activo: Activo, modifier: Modifier = Modifier) {
    val color = colorStatus(activo.status)
    Text(
        text = activo.statusLabel,
        color = BsWhite,
        fontSize = 12.sp,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
