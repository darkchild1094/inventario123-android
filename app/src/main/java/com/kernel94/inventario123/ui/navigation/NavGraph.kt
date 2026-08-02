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
                onAbrirEscaner = { navController.navigate(Screen.Escaner.route) },
                codigoEscaneado = codigoEscaneado,
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
                onAbrirEscaner = { navController.navigate(Screen.Escaner.route) },
                codigoEscaneado = codigoEscaneado,
                onCodigoConsumido = { codigoEscaneado = null },
            )
        }

        composable(Screen.Escaner.route) {
            EscanerScreen(
                onCodigoDetectado = { codigo -> codigoEscaneado = codigo; navController.popBackStack() },
                onCerrar = { navController.popBackStack() },
            )
        }

        composable(Screen.Usuarios.route) {
            val vm: UsuariosViewModel = viewModel(factory = factory)
            UsuariosScreen(viewModel = vm, onVolver = { navController.popBackStack() })
        }
    }
}
