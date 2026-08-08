package com.kernel94.inventario123.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Listado : Screen("listado")
    object Detalle : Screen("detalle/{id}") { fun crear(id: Int) = "detalle/$id" }
    object Crear : Screen("crear_activo")
    object Editar : Screen("editar_activo/{id}") { fun crear(id: Int) = "editar_activo/$id" }
    object Escaner : Screen("escaner/{target}") { fun crear(target: String) = "escaner/$target" }
    object Usuarios : Screen("usuarios")
}
