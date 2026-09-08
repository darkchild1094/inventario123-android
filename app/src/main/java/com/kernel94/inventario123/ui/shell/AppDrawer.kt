package com.kernel94.inventario123.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kernel94.inventario123.data.model.Perfil
import com.kernel94.inventario123.data.model.rolLabel
import com.kernel94.inventario123.data.remote.Urls
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsPrimary

fun iconoModulo(clave: String): ImageVector = when (clave) {
    "dashboard" -> Icons.Filled.Dashboard
    "consulta"  -> Icons.Filled.QrCodeScanner
    "tiendas"   -> Icons.Filled.Store
    "bodega"    -> Icons.Filled.Warehouse
    "mi_stock"  -> Icons.Filled.Handyman
    "stock_pfs" -> Icons.Filled.Groups
    "ati"       -> Icons.Filled.ManageAccounts
    "usuarios"  -> Icons.Filled.People
    else        -> Icons.Filled.Inventory2
}

/**
 * Contenido del menú lateral, compartido por el Dashboard y las pantallas de
 * listado por módulo. `moduloActivo` = clave del módulo que se está viendo
 * (o "dashboard"), para resaltar el ítem.
 */
@Composable
fun AppDrawerContent(
    perfil: Perfil?,
    moduloActivo: String,
    solicitudesPendientes: Int = 0,
    onModulo: (String) -> Unit,
    onDashboard: () -> Unit,
    onConsulta: () -> Unit,
    onHistorial: () -> Unit,
    onTraslados: () -> Unit,
    onPendientes: () -> Unit,
    onModelos: () -> Unit,
    onUsuarios: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val permisos = perfil?.permisos
    ModalDrawerSheet(drawerContainerColor = Color.White) {
        Column(Modifier.fillMaxWidth().background(BsDark).padding(20.dp)) {
            AsyncImage(
                model = perfil?.usuario?.foto?.let { Urls.usuarioFoto(it) } ?: "file:///android_asset/logo_login.png",
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f)),
            )
            Spacer(Modifier.height(10.dp))
            Text(perfil?.usuario?.nombre ?: "Usuario", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
            Text(perfil?.usuario?.plaza_nombre ?: "Inventario123", style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f))
            permisos?.tipo?.takeIf { it.isNotBlank() }?.let {
                Text(rolLabel(it), style = MaterialTheme.typography.labelSmall, color = BsPrimary)
            }
        }
        Spacer(Modifier.height(8.dp))

        perfil?.modulos.orEmpty().forEach { m ->
            NavigationDrawerItem(
                icon = { Icon(iconoModulo(m.clave), contentDescription = null) },
                label = { Text(m.etiqueta) },
                selected = moduloActivo == m.clave,
                onClick = {
                    when (m.clave) {
                        "dashboard" -> onDashboard()
                        "consulta"  -> onConsulta()
                        "usuarios"  -> onUsuarios()
                        else        -> onModulo(m.clave)
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp))

        if (permisos?.puedeVerHistorial == true) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.History, contentDescription = null) },
                label = { Text("Historial") }, selected = false,
                onClick = onHistorial, modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        if (permisos?.puedeVerTraslados == true) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                label = { Text("Traslados a bodega") },
                badge = { if (solicitudesPendientes > 0) Text(solicitudesPendientes.toString(), fontWeight = FontWeight.Bold, color = BsPrimary) },
                selected = false, onClick = onTraslados, modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.CloudUpload, contentDescription = null) },
            label = { Text("Altas pendientes de envío") }, selected = false,
            onClick = onPendientes, modifier = Modifier.padding(horizontal = 12.dp),
        )
        if (permisos?.puedeGestionarModelos == true) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Category, contentDescription = null) },
                label = { Text("Catálogo de modelos") }, selected = false,
                onClick = onModelos, modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            label = { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) },
            selected = false, onClick = onCerrarSesion,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
