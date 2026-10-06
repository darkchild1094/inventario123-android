package com.kernel94.inventario123.ui.form

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Piezas compartidas por los dos formularios que mueven equipo
 * (CrearEditarActivoScreen y TiendaMovScreen), para que un reemplazo se vea
 * igual en los dos y no haya dos dialectos del mismo bloque.
 *
 * Un color por papel, para distinguir las secciones de un vistazo:
 * azul lo que entra, verde lo que sale.
 */
val AZUL_INSTALA = Color(0xFF0D6EFD)
val VERDE_RETIRO = Color(0xFF198754)
val ROJO_AVISO = Color(0xFFDC3545)

/**
 * Recuadro que agrupa los campos de UN equipo, con su título tenue arriba. En un
 * reemplazo pasan dos cosas a la vez —entra un equipo y sale otro— y sin el
 * marco los campos de ambos quedaban en una sola columna corrida: era fácil
 * escanear la serie del que sale en el campo del que entra.
 *
 * @param titulo    qué equipo es ("Equipo que se INSTALA")
 * @param subtitulo qué va a pasar con él, en palabras llanas
 */
@Composable
fun SeccionEquipo(
    titulo: String,
    subtitulo: String,
    acento: Color,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = acento,
            letterSpacing = 1.sp,
        )
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, acento.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                contenido()
            }
        }
    }
}

/**
 * Las tres fotos de un equipo, siempre las mismas tres y en el mismo orden, para
 * que el técnico no tenga que acordarse de cuáles tocan: el aparato, su etiqueta
 * de serie y su código de barras. Numeradas porque se toman en ese orden.
 */
@Composable
fun FotosDelEquipo(
    equipoUri: Uri?, onEquipo: (Uri?) -> Unit,
    serieUri: Uri?, onSerie: (Uri?) -> Unit,
    codigoUri: Uri?, onCodigo: (Uri?) -> Unit,
    equipoUrlActual: String? = null,
    serieUrlActual: String? = null,
    codigoUrlActual: String? = null,
) {
    FotoActivoCampo(
        etiqueta = "1. Foto del equipo completo", urlActual = equipoUrlActual,
        uriSeleccionada = equipoUri, onCambio = onEquipo,
        modifier = Modifier.fillMaxWidth(),
    )
    FotoActivoCampo(
        etiqueta = "2. Foto de la etiqueta de SERIE", urlActual = serieUrlActual,
        uriSeleccionada = serieUri, onCambio = onSerie,
        modifier = Modifier.fillMaxWidth(),
    )
    FotoActivoCampo(
        etiqueta = "3. Foto del CÓDIGO DE BARRAS", urlActual = codigoUrlActual,
        uriSeleccionada = codigoUri, onCambio = onCodigo,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * N° de activo (placa de activo fijo) en SOLO LECTURA. Se muestra para que el
 * técnico la coteje con la etiqueta del equipo; no se edita desde la app: la
 * asigna activo fijo y el servidor nunca deja cambiar una placa ya puesta.
 * No se dibuja nada si el equipo todavía no tiene placa.
 */
@Composable
fun PlacaActivoFijo(numActivo: String?) {
    val valor = numActivo?.trim().orEmpty()
    if (valor.isEmpty()) return
    OutlinedTextField(
        value = valor,
        onValueChange = { },
        readOnly = true,
        enabled = false,
        label = { Text("N° de activo") },
        supportingText = { Text("Asignado por activo fijo; no se edita aquí") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
