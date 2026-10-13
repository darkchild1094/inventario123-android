package com.kernel94.inventario123.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel94.inventario123.BuildConfig
import com.kernel94.inventario123.data.model.DashPorNombre
import com.kernel94.inventario123.data.model.VersionApp
import com.kernel94.inventario123.ui.shell.AppDrawerContent
import com.kernel94.inventario123.ui.theme.BsDark
import com.kernel94.inventario123.ui.theme.BsCanvas
import com.kernel94.inventario123.ui.theme.BsPrimary
import com.kernel94.inventario123.ui.theme.BsSuccess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAbrirNotificaciones: () -> Unit,
    onCerrarSesion: () -> Unit,
    onAbrirModulo: (String) -> Unit = {},
    onAbrirConsulta: () -> Unit = {},
    onAbrirModelos: () -> Unit = {},
    onAbrirUsuarios: () -> Unit = {},
    onAbrirRentecDetalle: (Int) -> Unit = {},
) {
    LaunchedEffect(Unit) { viewModel.cargar() }
    val r = viewModel.resumen
    val p = viewModel.perfil
    val noLeidas by viewModel.notificacionesNoLeidas.collectAsState()
    val nombre = p?.usuario?.nombre?.split(" ")?.firstOrNull() ?: "Usuario"
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    fun cerrarYHacer(a: () -> Unit) { scope.launch { drawerState.close() }; a() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                perfil = p,
                moduloActivo = "dashboard",
                notificacionesNoLeidas = noLeidas,
                onModulo = { m -> cerrarYHacer { onAbrirModulo(m) } },
                onDashboard = { scope.launch { drawerState.close() } },
                onConsulta = { cerrarYHacer(onAbrirConsulta) },
                onNotificaciones = { cerrarYHacer(onAbrirNotificaciones) },
                onModelos = { cerrarYHacer(onAbrirModelos) },
                onUsuarios = { cerrarYHacer(onAbrirUsuarios) },
                onCerrarSesion = { cerrarYHacer(onCerrarSesion) },
            )
        }
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = Color.White)
                    }
                },
                title = {
                    Column {
                        Text("Hola, $nombre", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "${com.kernel94.inventario123.data.model.rolLabel(p?.permisos?.tipo)} · ${p?.usuario?.plaza_nombre ?: ""}",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .75f),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BsDark, titleContentColor = Color.White),
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().background(BsCanvas)
                .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                viewModel.cargando && r == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                viewModel.error != null && r == null -> Text(viewModel.error!!, color = Color.Gray)
                r != null -> {
                    val claves = p?.modulos.orEmpty().map { it.clave }.toSet()

                    // 4 tarjetas navegables (handoff §4 Dashboard), cada una con
                    // su color — solo las que el rol realmente tiene (ej. pfs no
                    // ve Bodega).
                    data class Tarjeta(
                        val etiqueta: String, val valor: String, val caption: String,
                        val icono: androidx.compose.ui.graphics.vector.ImageVector,
                        val fondo: Color, val onClick: () -> Unit,
                    )
                    val tarjetas = buildList {
                        if ("mi_stock" in claves) add(Tarjeta(
                            "Mi stock", "${r.por_modulo["mi_stock"] ?: 0}", "activos asignados a mí",
                            Icons.Filled.Inventory2, BsPrimary, { onAbrirModulo("mi_stock") },
                        ))
                        if ("bodega" in claves) add(Tarjeta(
                            "Bodega", "${r.por_modulo["bodega"] ?: 0}", "en ${r.bodega_por_plaza.size} plaza(s)",
                            Icons.Filled.Warehouse, Color(0xFF6C757D), { onAbrirModulo("bodega") },
                        ))
                        if ("tiendas" in claves) add(Tarjeta(
                            "Tiendas", "${r.tiendas_total}", "${r.por_modulo["tiendas"] ?: 0} activos instalados",
                            Icons.Filled.Store, Color(0xFF198754), { onAbrirModulo("tiendas") },
                        ))
                        if ("rentec" in claves) {
                            val abiertos = viewModel.proyectosRentec.count { it.abierto }
                            val finalizados = viewModel.proyectosRentec.size - abiertos
                            add(Tarjeta(
                                "RENTEC", "${viewModel.proyectosRentec.size}", "$abiertos abierto(s) · $finalizados finalizado(s)",
                                Icons.Filled.Autorenew, BsDark, { onAbrirModulo("rentec") },
                            ))
                        }
                    }
                    if (tarjetas.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tarjetas.chunked(2).forEach { fila ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    fila.forEach { t ->
                                        Card(
                                            onClick = t.onClick,
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = t.fondo),
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(t.etiqueta, fontWeight = FontWeight.Medium, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                                    Icon(t.icono, contentDescription = null, tint = Color.White)
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(t.valor, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                                                Text(t.caption, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                            }
                                        }
                                    }
                                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Avance RENTEC: % global de los proyectos abiertos + barra
                    // por proyecto (toca → detalle).
                    if ("rentec" in claves && viewModel.proyectosRentec.any { it.abierto }) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Avance RENTEC", fontWeight = FontWeight.Bold)
                                    Text("${viewModel.avanceRentecGlobal}%", fontWeight = FontWeight.Bold)
                                }
                                BarraProgreso(viewModel.avanceRentecGlobal, color = BsSuccess)
                                Text(
                                    "${viewModel.proyectosRentec.filter { it.abierto }.sumOf { it.instalados }} de " +
                                        "${viewModel.proyectosRentec.filter { it.abierto }.sumOf { it.recibidos + it.instalados }} activos instalados en proyectos abiertos",
                                    style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                                )
                                Spacer(Modifier.height(10.dp))
                                viewModel.proyectosRentec.filter { it.abierto }.forEach { proy ->
                                    Column(
                                        Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                            .clickable { onAbrirRentecDetalle(proy.id) },
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(proy.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                            Text("${proy.avancePct}%", fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            "${proy.folio} · ${proy.plaza_nombre ?: "—"} · ${proy.instalados} de ${proy.recibidos + proy.instalados} instalados",
                                            style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        BarraProgreso(proy.avancePct, alto = 4, color = BsSuccess)
                                    }
                                }
                            }
                        }
                    }

                    // Inventario de tiendas: N de M con checklist registrado +
                    // las primeras pendientes (la lista completa vive en Tiendas).
                    if ("tiendas" in claves && r.tiendas_total > 0) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Inventario de tiendas", fontWeight = FontWeight.Bold)
                                    Text("${r.tiendas_con_inventario}/${r.tiendas_total}", fontWeight = FontWeight.Bold, color = Color.Gray)
                                }
                                Spacer(Modifier.height(6.dp))
                                BarraProgreso(if (r.tiendas_total > 0) r.tiendas_con_inventario * 100 / r.tiendas_total else 0)
                                if (viewModel.tiendasPendientes.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    viewModel.tiendasPendientes.forEach { t ->
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column {
                                                Text(t.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    listOfNotNull(t.cr_tienda?.let { "CR $it" }, t.plaza_nombre).joinToString(" · "),
                                                    style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                                                )
                                            }
                                            AssistChip(onClick = { onAbrirModulo("tiendas") }, label = { Text("Pendiente") })
                                        }
                                    }
                                }
                                TextButton(onClick = { onAbrirModulo("tiendas") }, modifier = Modifier.align(Alignment.Start)) { Text("Ver todas") }
                            }
                        }
                    }

                    if (noLeidas > 0) {
                        Card(
                            onClick = onAbrirNotificaciones,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                        ) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color(0xFF664D03))
                                Spacer(Modifier.width(10.dp))
                                Text("$noLeidas notificación(es) sin leer", color = Color(0xFF664D03), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Bodega por plaza (barras grises) — solo si el rol ve Bodega y hay más de una plaza.
                    if (r.bodega_por_plaza.size > 1) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Bodega por plaza", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                BarrasPorNombre(r.bodega_por_plaza, color = Color(0xFF6C757D))
                            }
                        }
                    }

                    // Mi stock por tipo (barras azules) — lo propio, siempre que tenga algo.
                    if ("mi_stock" in claves && r.mi_stock_por_tipo.isNotEmpty()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Mi stock por tipo", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                BarrasPorNombre(r.mi_stock_por_tipo, color = BsPrimary)
                            }
                        }
                    }

                    // "Actualizar app": visible a TODOS los roles (subir el APK
                    // sigue siendo solo de admin, desde el panel web — esto es
                    // de solo lectura, para que cualquier técnico sepa si le
                    // falta actualizar sin tener que preguntar).
                    VersionAppCard(viewModel.versionApp)

                    // Accesos rápidos (handoff §4: Consulta y Usuarios).
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Card(
                            onClick = onAbrirConsulta, modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                        ) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = BsPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Consulta", fontWeight = FontWeight.Bold)
                            }
                        }
                        if ("usuarios" in claves) {
                            Card(
                                onClick = onAbrirUsuarios, modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                            ) {
                                Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.People, contentDescription = null, tint = BsPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Usuarios", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
    }
}

/**
 * Sección "Actualizar app": compara la versión instalada (BuildConfig)
 * contra la última subida por un admin (ApiController::obtenerUltimaVersionApp).
 * No se muestra nada mientras no haya respuesta o no exista ningún APK
 * subido — nunca aparece a medias ni con un estado falso.
 */
@Composable
private fun VersionAppCard(versionApp: VersionApp?) {
    if (versionApp?.hay_version != true) return
    val context = LocalContext.current
    val hayNueva = versionApp.version_code > BuildConfig.VERSION_CODE

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (hayNueva) Color(0xFFD1E7DD) else Color.White),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PhoneAndroid, contentDescription = null,
                    tint = if (hayNueva) Color(0xFF0F5132) else Color.Gray,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Actualizar app", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (hayNueva) "Versión ${versionApp.version_name} disponible · tienes la ${BuildConfig.VERSION_NAME}"
                        else "Estás en la última versión (${BuildConfig.VERSION_NAME})",
                        style = MaterialTheme.typography.labelSmall, color = Color.Gray,
                    )
                }
            }
            if (hayNueva) {
                if (!versionApp.notas.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(versionApp.notas, style = MaterialTheme.typography.bodySmall, color = Color(0xFF0F5132), maxLines = 3)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val url = versionApp.url_descarga ?: return@Button
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Descargar actualización")
                }
            }
        }
    }
}

@Composable
private fun BarrasPorNombre(datos: List<DashPorNombre>, color: Color = BsPrimary) {
    val max = (datos.maxOfOrNull { it.n } ?: 1).coerceAtLeast(1)
    datos.forEach { d ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(d.nombre, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text("${d.n}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE9ECEF))) {
            Box(Modifier.fillMaxWidth(d.n.toFloat() / max).fillMaxHeight().background(color))
        }
    }
}

@Composable
private fun BarraProgreso(pct: Int, alto: Int = 6, color: Color = BsPrimary) {
    Box(Modifier.fillMaxWidth().height(alto.dp).clip(RoundedCornerShape((alto / 2).dp)).background(Color(0xFFE9ECEF))) {
        Box(Modifier.fillMaxWidth((pct / 100f).coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}
