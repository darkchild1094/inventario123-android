package com.kernel94.inventario123.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kernel94.inventario123.Inventario123App
import com.kernel94.inventario123.ui.auth.LoginScreen
import com.kernel94.inventario123.ui.auth.LoginViewModel
import com.kernel94.inventario123.ui.common.ViewModelFactory
import com.kernel94.inventario123.ui.detalle.DetalleActivoScreen
import com.kernel94.inventario123.ui.detalle.DetalleViewModel
import com.kernel94.inventario123.ui.form.CrearEditarActivoScreen
import com.kernel94.inventario123.ui.form.CrearEditarActivoViewModel
import com.kernel94.inventario123.ui.historial.HistorialScreen
import com.kernel94.inventario123.ui.historial.HistorialViewModel
import com.kernel94.inventario123.ui.listado.ListadoScreen
import com.kernel94.inventario123.ui.listado.ListadoViewModel
import com.kernel94.inventario123.ui.scanner.EscanerScreen
import com.kernel94.inventario123.ui.tiendas.TiendasScreen
import com.kernel94.inventario123.ui.tiendas.TiendasViewModel
import com.kernel94.inventario123.ui.usuarios.UsuariosScreen
import com.kernel94.inventario123.ui.usuarios.UsuariosViewModel
import kotlinx.coroutines.launch

@Composable
fun Inventario123NavGraph(app: Inventario123App, sesionActivaInicial: Boolean) {
    val navController = rememberNavController()
    val factory = remember { ViewModelFactory(app) }
    val scope = rememberCoroutineScope()
    var serieEscaneada by remember { mutableStateOf<String?>(null) }
    var codigoEscaneado by remember { mutableStateOf<String?>(null) }

    // Si cualquier llamada al API responde 401 (sesión expirada/invalida en el
    // servidor), SessionInterceptor lo notifica aquí y regresamos a Login sin
    // importar en qué pantalla estaba el usuario, limpiando todo el historial.
    LaunchedEffect(Unit) {
        com.kernel94.inventario123.data.remote.SessionExpiredNotifier.eventos.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val irALogin: () -> Unit = {
        scope.launch {
            app.authRepository.logout()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (sesionActivaInicial) Screen.Dashboard.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val vm: LoginViewModel = viewModel(factory = factory)
            LoginScreen(vm) {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }

        // Enrutado de un módulo a su pantalla (Tiendas tiene lista propia).
        val abrirModulo: (String) -> Unit = { clave ->
            when (clave) {
                "tiendas"  -> navController.navigate(Screen.Tiendas.route)
                "usuarios" -> navController.navigate(Screen.Usuarios.route)
                "consulta" -> navController.navigate(Screen.Consulta.route)
                else       -> navController.navigate(Screen.Modulo.crear(clave))
            }
        }
        // FAB "crear" según módulo: tiendas -> form de 3 modos; resto -> alta.
        val abrirCrear: (String, Int?) -> Unit = { modulo, tiendaId ->
            if (modulo == "tiendas") navController.navigate(Screen.TiendaMov.crear(tiendaId))
            else navController.navigate(Screen.Crear.crear(modulo.ifBlank { null }, tiendaId))
        }

        composable(Screen.Dashboard.route) {
            val vm: com.kernel94.inventario123.ui.dashboard.DashboardViewModel = viewModel(factory = factory)
            com.kernel94.inventario123.ui.dashboard.DashboardScreen(
                viewModel = vm,
                onAbrirInventario = { navController.navigate(Screen.Listado.route) },
                onAbrirHistorial = { navController.navigate(Screen.Historial.route) },
                onAbrirTraslados = { navController.navigate(Screen.Solicitudes.route) },
                onAbrirPendientes = { navController.navigate(Screen.Pendientes.route) },
                onCerrarSesion = irALogin,
                onAbrirModulo = abrirModulo,
                onAbrirConsulta = { navController.navigate(Screen.Consulta.route) },
                onAbrirModelos = { navController.navigate(Screen.Modelos.route) },
                onAbrirUsuarios = { navController.navigate(Screen.Usuarios.route) },
            )
        }

        composable(Screen.Consulta.route) {
            val vm: com.kernel94.inventario123.ui.consulta.ConsultaViewModel = viewModel(factory = factory)
            com.kernel94.inventario123.ui.consulta.ConsultaScreen(
                viewModel = vm,
                onVolver = { navController.popBackStack() },
                onAbrirDetalle = { id -> navController.navigate(Screen.Detalle.crear(id)) },
                onAbrirEscaner = { navController.navigate(Screen.Escaner.crear("codigo")) },
                serieEscaneada = codigoEscaneado ?: serieEscaneada,
                onSerieConsumida = { codigoEscaneado = null; serieEscaneada = null },
            )
        }

        composable(
            Screen.Modulo.route,
            arguments = listOf(
                navArgument("modulo") { type = NavType.StringType },
                navArgument("tiendaId") { type = NavType.IntType; defaultValue = 0 },
            )
        ) { backStackEntry ->
            val moduloArg = backStackEntry.arguments?.getString("modulo") ?: ""
            val tiendaArg = backStackEntry.arguments?.getInt("tiendaId") ?: 0
            val vm: ListadoViewModel = viewModel(factory = factory)
            ListadoScreen(
                viewModel = vm,
                modulo = moduloArg,
                tiendaId = tiendaArg.takeIf { it > 0 },
                onAbrirDetalle = { id -> navController.navigate(Screen.Detalle.crear(id)) },
                onEditar = { id -> navController.navigate(Screen.Editar.crear(id)) },
                onCrearNuevo = { abrirCrear(moduloArg, tiendaArg.takeIf { it > 0 }) },
                onCerrarSesion = irALogin,
                onAbrirHistorial = { navController.navigate(Screen.Historial.route) },
                onAbrirTiendas = { navController.navigate(Screen.Tiendas.route) },
                onAbrirModelos = { navController.navigate(Screen.Modelos.route) },
                onAbrirSolicitudes = { navController.navigate(Screen.Solicitudes.route) },
                onAbrirPendientes = { navController.navigate(Screen.Pendientes.route) },
                onAbrirModulo = abrirModulo,
                onAbrirConsulta = { navController.navigate(Screen.Consulta.route) },
                onAbrirDashboard = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } },
                onAbrirUsuarios = { navController.navigate(Screen.Usuarios.route) },
            )
        }

        composable(
            Screen.TiendaMov.route,
            arguments = listOf(navArgument("tiendaId") { type = NavType.IntType; defaultValue = 0 }),
        ) { backStackEntry ->
            val tId = backStackEntry.arguments?.getInt("tiendaId") ?: 0
            val vm: com.kernel94.inventario123.ui.form.TiendaMovViewModel = viewModel(factory = factory)
            com.kernel94.inventario123.ui.form.TiendaMovScreen(
                viewModel = vm,
                tiendaFijaId = tId.takeIf { it > 0 },
                onVolver = { navController.popBackStack() },
                onAbrirEscanerSerie = { navController.navigate(Screen.Escaner.crear("serie", vm.prefijoEscanerSerie(), vm.ocrEscanerSerie())) },
                onAbrirEscanerCodigo = { navController.navigate(Screen.Escaner.crear("codigo")) },
                serieEscaneada = serieEscaneada,
                codigoEscaneado = codigoEscaneado,
                onSerieConsumida = { serieEscaneada = null },
                onCodigoConsumido = { codigoEscaneado = null },
            )
        }

        composable(Screen.Pendientes.route) {
            val vm: com.kernel94.inventario123.ui.pendientes.PendientesViewModel = viewModel(factory = factory)
            com.kernel94.inventario123.ui.pendientes.PendientesScreen(viewModel = vm, onVolver = { navController.popBackStack() })
        }

        composable(Screen.Listado.route) {
            val vm: ListadoViewModel = viewModel(factory = factory)
            ListadoScreen(
                viewModel = vm,
                onAbrirDetalle = { id -> navController.navigate(Screen.Detalle.crear(id)) },
                onEditar = { id -> navController.navigate(Screen.Editar.crear(id)) },
                onCrearNuevo = { navController.navigate(Screen.Crear.crear()) },
                onCerrarSesion = irALogin,
                onAbrirHistorial = { navController.navigate(Screen.Historial.route) },
                onAbrirTiendas = { navController.navigate(Screen.Tiendas.route) },
                onAbrirModelos = { navController.navigate(Screen.Modelos.route) },
                onAbrirSolicitudes = { navController.navigate(Screen.Solicitudes.route) },
                onAbrirPendientes = { navController.navigate(Screen.Pendientes.route) },
                onAbrirModulo = abrirModulo,
                onAbrirConsulta = { navController.navigate(Screen.Consulta.route) },
                onAbrirDashboard = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } },
                onAbrirUsuarios = { navController.navigate(Screen.Usuarios.route) },
            )
        }

        composable(
            Screen.Detalle.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val vm: DetalleViewModel = viewModel(factory = factory)
            DetalleActivoScreen(
                viewModel = vm, activoId = id,
                onVolver = { navController.popBackStack() },
                onEditar = { editId -> navController.navigate(Screen.Editar.crear(editId)) },
            )
        }

        composable(
            Screen.Crear.route,
            arguments = listOf(
                navArgument("modulo") { type = NavType.StringType; defaultValue = "" },
                navArgument("tiendaUsoId") { type = NavType.IntType; defaultValue = 0 },
            )
        ) { backStackEntry ->
            val moduloArg = backStackEntry.arguments?.getString("modulo").orEmpty()
            val tiendaUsoArg = backStackEntry.arguments?.getInt("tiendaUsoId") ?: 0
            val vm: CrearEditarActivoViewModel = viewModel(factory = factory)
            CrearEditarActivoScreen(
                viewModel = vm, idActivoAEditar = null,
                moduloContexto = moduloArg.takeIf { it.isNotBlank() },
                tiendaUsoContexto = tiendaUsoArg.takeIf { it > 0 },
                onVolver = { navController.popBackStack() },
                onAbrirEscanerSerie = {
                    navController.navigate(Screen.Escaner.crear("serie", vm.prefijoEscanerSerie(), vm.ocrEscanerSerie()))
                },
                onAbrirEscanerCodigo = { navController.navigate(Screen.Escaner.crear("codigo")) },
                serieEscaneada = serieEscaneada,
                codigoEscaneado = codigoEscaneado,
                onSerieConsumida = { serieEscaneada = null },
                onCodigoConsumido = { codigoEscaneado = null },
            )
        }

        composable(
            Screen.Editar.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val vm: CrearEditarActivoViewModel = viewModel(factory = factory)
            CrearEditarActivoScreen(
                viewModel = vm, idActivoAEditar = id,
                onVolver = { navController.popBackStack() },
                onAbrirEscanerSerie = {
                    navController.navigate(Screen.Escaner.crear("serie", vm.prefijoEscanerSerie(), vm.ocrEscanerSerie()))
                },
                onAbrirEscanerCodigo = { navController.navigate(Screen.Escaner.crear("codigo")) },
                serieEscaneada = serieEscaneada,
                codigoEscaneado = codigoEscaneado,
                onSerieConsumida = { serieEscaneada = null },
                onCodigoConsumido = { codigoEscaneado = null },
            )
        }

        composable(
            Screen.Escaner.route,
            arguments = listOf(
                navArgument("target") { type = NavType.StringType },
                navArgument("prefijo") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("modoRegulador") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val target = backStackEntry.arguments?.getString("target") ?: "serie"
            val prefijo = backStackEntry.arguments?.getString("prefijo")
            val modoRegulador = backStackEntry.arguments?.getBoolean("modoRegulador") ?: false

            val instruccion = when {
                target != "serie" -> "Apunta al CÓDIGO DE BARRAS del activo"
                prefijo != null -> "Código de barras pequeño (empieza con \"${prefijo.replace(",", "\" o \"")}\"). Acércate bien; usa zoom o linterna si hace falta."
                modoRegulador -> "Apunta a la etiqueta: se captura el texto después de SERIE: o S/N:"
                else -> "Apunta al código de barras o a la etiqueta de SERIE"
            }
            EscanerScreen(
                instruccion = instruccion,
                filtroPrefijo = prefijo,
                modoRegulador = modoRegulador,
                target = target,
                onCodigoDetectado = { codigo ->
                    if (target == "serie") serieEscaneada = codigo
                    else codigoEscaneado = codigo
                    navController.popBackStack()
                },
                onCerrar = { navController.popBackStack() },
            )
        }

        composable(Screen.Historial.route) {
            val vm: HistorialViewModel = viewModel(factory = factory)
            HistorialScreen(viewModel = vm, onVolver = { navController.popBackStack() })
        }

        composable(Screen.Tiendas.route) {
            val vm: TiendasViewModel = viewModel(factory = factory)
            TiendasScreen(
                viewModel = vm,
                onVolver = { navController.popBackStack() },
                onAbrirTienda = { id -> navController.navigate(Screen.Modulo.crear("tiendas", id)) },
            )
        }

        composable(Screen.Usuarios.route) {
            val vm: UsuariosViewModel = viewModel(factory = factory)
            UsuariosScreen(viewModel = vm, onVolver = { navController.popBackStack() })
        }

        composable(Screen.Modelos.route) {
            val vm: com.kernel94.inventario123.ui.modelos.ModelosViewModel = viewModel(factory = factory)
            com.kernel94.inventario123.ui.modelos.ModelosScreen(viewModel = vm, onVolver = { navController.popBackStack() })
        }

        composable(Screen.Solicitudes.route) {
            val vm: com.kernel94.inventario123.ui.solicitudes.SolicitudesViewModel = viewModel(factory = factory)
            com.kernel94.inventario123.ui.solicitudes.SolicitudesScreen(
                viewModel = vm,
                onVolver = { navController.popBackStack() },
                onAbrirDetalle = { id -> navController.navigate(Screen.SolicitudDetalle.crear(id)) },
                onNueva = { navController.navigate(Screen.CrearSolicitud.route) },
            )
        }

        composable(Screen.CrearSolicitud.route) {
            val vm: com.kernel94.inventario123.ui.solicitudes.SolicitudesViewModel = viewModel(factory = factory)
            com.kernel94.inventario123.ui.solicitudes.CrearSolicitudScreen(
                viewModel = vm,
                onVolver = { navController.popBackStack() },
                onEnviada = { navController.popBackStack() },
            )
        }

        composable(
            Screen.SolicitudDetalle.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val vm: com.kernel94.inventario123.ui.solicitudes.SolicitudesViewModel = viewModel(factory = factory)
            com.kernel94.inventario123.ui.solicitudes.SolicitudDetalleScreen(
                viewModel = vm, solicitudId = id,
                onVolver = { navController.popBackStack() },
            )
        }
    }
}
