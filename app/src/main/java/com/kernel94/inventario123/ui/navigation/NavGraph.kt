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
import com.kernel94.inventario123.ui.listado.ListadoScreen
import com.kernel94.inventario123.ui.listado.ListadoViewModel
import com.kernel94.inventario123.ui.scanner.EscanerScreen
import com.kernel94.inventario123.ui.usuarios.UsuariosScreen
import com.kernel94.inventario123.ui.usuarios.UsuariosViewModel
import kotlinx.coroutines.launch

@Composable
fun Inventario123NavGraph(app: Inventario123App, sesionActivaInicial: Boolean) {
    val navController = rememberNavController()
    val factory = remember { ViewModelFactory(app) }
    var serieEscaneada by remember { mutableStateOf<String?>(null) }
    var placaEscaneada by remember { mutableStateOf<String?>(null) }

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

    NavHost(
        navController = navController,
        startDestination = if (sesionActivaInicial) Screen.Listado.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val vm: LoginViewModel = viewModel(factory = factory)
            LoginScreen(vm) {
                navController.navigate(Screen.Listado.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Listado.route) {
            val vm: ListadoViewModel = viewModel(factory = factory)
            ListadoScreen(
                viewModel = vm,
                onAbrirDetalle = { id -> navController.navigate(Screen.Detalle.crear(id)) },
                onEditar = { id -> navController.navigate(Screen.Editar.crear(id)) },
                onCrearNuevo = { navController.navigate(Screen.Crear.route) },
                onCerrarSesion = {
                    kotlinx.coroutines.MainScope().launch {
                        app.authRepository.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                },
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

        composable(Screen.Crear.route) {
            val vm: CrearEditarActivoViewModel = viewModel(factory = factory)
            CrearEditarActivoScreen(
                viewModel = vm, idActivoAEditar = null,
                onVolver = { navController.popBackStack() },
                onAbrirEscanerSerie = {
                    // Detecta UPS/Regulador por NOMBRE del dispositivo (no por id
                    // hardcodeado, que podría no coincidir entre entornos).
                    val nombreDispositivo = vm.nombreDispositivoSeleccionado()?.uppercase() ?: ""
                    val prefijo = if (nombreDispositivo.contains("UPS")) "3S,SM" else null
                    val modoRegulador = nombreDispositivo.contains("REGULADOR")
                    navController.navigate(Screen.Escaner.crear("serie", prefijo, modoRegulador))
                },
                onAbrirEscanerPlaca = { navController.navigate(Screen.Escaner.crear("placa")) },
                serieEscaneada = serieEscaneada,
                placaEscaneada = placaEscaneada,
                onSerieConsumida = { serieEscaneada = null },
                onPlacaConsumida = { placaEscaneada = null },
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
                    val nombreDispositivo = vm.nombreDispositivoSeleccionado()?.uppercase() ?: ""
                    val prefijo = if (nombreDispositivo.contains("UPS")) "3S,SM" else null
                    val modoRegulador = nombreDispositivo.contains("REGULADOR")
                    navController.navigate(Screen.Escaner.crear("serie", prefijo, modoRegulador))
                },
                onAbrirEscanerPlaca = { navController.navigate(Screen.Escaner.crear("placa")) },
                serieEscaneada = serieEscaneada,
                placaEscaneada = placaEscaneada,
                onSerieConsumida = { serieEscaneada = null },
                onPlacaConsumida = { placaEscaneada = null },
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
                target != "serie" -> "Apunta al código de barras de la PLACA / ACTIVO FIJO"
                prefijo != null -> "Código de barras pequeño (empieza con \"${prefijo.replace(",", "\" o \"")}\"). Acércate bien; usa zoom o linterna si hace falta."
                modoRegulador -> "Apunta a la etiqueta: se captura el texto después de SERIE: o S/N:"
                else -> "Apunta al código de barras o a la etiqueta de SERIE"
            }
            EscanerScreen(
                instruccion = instruccion,
                filtroPrefijo = prefijo,
                modoRegulador = modoRegulador,
                onCodigoDetectado = { codigo ->
                    if (target == "serie") serieEscaneada = codigo
                    else placaEscaneada = codigo
                    navController.popBackStack()
                },
                onCerrar = { navController.popBackStack() },
            )
        }

        composable(Screen.Usuarios.route) {
            val vm: UsuariosViewModel = viewModel(factory = factory)
            UsuariosScreen(viewModel = vm, onVolver = { navController.popBackStack() })
        }
    }
}
