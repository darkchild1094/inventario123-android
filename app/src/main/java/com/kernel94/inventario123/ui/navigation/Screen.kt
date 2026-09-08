package com.kernel94.inventario123.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Listado : Screen("listado")
    // Listado de un módulo (Bodega, Mi Stock, Stock PFS, ATI, Tiendas con tienda_id).
    object Modulo : Screen("modulo/{modulo}?tiendaId={tiendaId}") {
        fun crear(modulo: String, tiendaId: Int? = null) =
            "modulo/$modulo?tiendaId=${tiendaId ?: 0}"
    }
    object Consulta : Screen("consulta")
    // Form simple del módulo Tiendas: Instalación / Retiro / Reemplazo.
    object TiendaMov : Screen("tienda_mov?tiendaId={tiendaId}") {
        fun crear(tiendaId: Int? = null) = "tienda_mov?tiendaId=${tiendaId ?: 0}"
    }
    object Detalle : Screen("detalle/{id}") { fun crear(id: Int) = "detalle/$id" }
    object Crear : Screen("crear_activo?modulo={modulo}&tiendaUsoId={tiendaUsoId}") {
        fun crear(modulo: String? = null, tiendaUsoId: Int? = null) =
            "crear_activo?modulo=${modulo ?: ""}&tiendaUsoId=${tiendaUsoId ?: 0}"
    }
    object Editar : Screen("editar_activo/{id}") { fun crear(id: Int) = "editar_activo/$id" }
    object Escaner : Screen("escaner/{target}?prefijo={prefijo}&modoRegulador={modoRegulador}") {
        fun crear(target: String, prefijo: String? = null, modoRegulador: Boolean = false) =
            "escaner/$target?prefijo=$prefijo&modoRegulador=$modoRegulador"
    }
    object Usuarios : Screen("usuarios")
    object Historial : Screen("historial")
    object Tiendas : Screen("tiendas")
    object Modelos : Screen("modelos")
    object Pendientes : Screen("pendientes")
    object Solicitudes : Screen("solicitudes")
    object CrearSolicitud : Screen("crear_solicitud")
    object SolicitudDetalle : Screen("solicitud/{id}") { fun crear(id: Int) = "solicitud/$id" }
}
