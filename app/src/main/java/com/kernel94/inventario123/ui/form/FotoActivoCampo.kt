package com.kernel94.inventario123.ui.form

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

/**
 * Campo de foto opcional del activo (equipo / serie / código de barras — migración 007).
 * Permite tomar una foto nueva con la cámara o elegirla de galería; si no se toca,
 * conserva la foto que ya tenía el activo en el servidor (edición).
 */
@Composable
fun FotoActivoCampo(
    etiqueta: String,
    urlActual: String?,
    uriSeleccionada: Uri?,
    onCambio: (Uri?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var uriCamaraPendiente by remember { mutableStateOf<Uri?>(null) }

    val lanzarCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) onCambio(uriCamaraPendiente)
    }
    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            val uri = crearUriTemporal(context)
            uriCamaraPendiente = uri
            lanzarCamara.launch(uri)
        }
    }
    val lanzarGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onCambio(uri)
    }

    Column(modifier.padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F3F5))
            ) {
                val modeloImagen = uriSeleccionada ?: urlActual
                if (modeloImagen != null) {
                    AsyncImage(model = modeloImagen, contentDescription = etiqueta, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = Color.LightGray, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = {
                val yaConcedido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (yaConcedido) {
                    val uri = crearUriTemporal(context)
                    uriCamaraPendiente = uri
                    lanzarCamara.launch(uri)
                } else {
                    permisoCamara.launch(Manifest.permission.CAMERA)
                }
            }) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cámara")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { lanzarGaleria.launch("image/*") }) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Galería")
            }
        }
    }
}

private fun crearUriTemporal(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File(dir, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}
