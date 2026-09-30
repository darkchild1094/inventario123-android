package com.kernel94.inventario123.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Listado : Screen("listado")
    // Listado de un módulo (Bodega, Mi Stock, Stock PFS, ATI, Tiendas con tienda_id).
    object Modulo : Screen("modulo/{modulo}?tiendaId={tiendaId}&usuarioId={usuarioId}") {
        fun crear(modulo: String, tiendaId: Int? = null, usuarioId: Int? = null) =
            "modulo/$modulo?tiendaId=${tiendaId ?: 0}&usuarioId=${usuarioId ?: 0}"
    }
    // Landing de "Stock PFS": lista de ingenieros con 1+ activos a su nombre.
    object StockPfsLista : Screen("stock_pfs_lista")
    object Consulta : Screen("consulta")
    // Form simple del módulo Tiendas: Instalación / Retiro / Reemplazo.
    object TiendaMov : Screen(
        "tienda_mov?tiendaId={tiendaId}&proyectoRentecId={proyectoRentecId}&proyectoRentecFolio={proyectoRentecFolio}"
    ) {
        fun crear(tiendaId: Int? = null, proyectoRentecId: Int? = null, proyectoRentecFolio: String? = null) =
            "tienda_mov?tiendaId=${tiendaId ?: 0}&proyectoRentecId=${proyectoRentecId ?: 0}&proyectoRentecFolio=${proyectoRentecFolio ?: ""}"
    }
    object Detalle : Screen("detalle/{id}") { fun crear(id: Int) = "detalle/$id" }
    object Crear : Screen(
        "crear_activo?modulo={modulo}&tiendaUsoId={tiendaUsoId}&proyectoRentecId={proyectoRentecId}&proyectoRentecFolio={proyectoRentecFolio}"
    ) {
        fun crear(
            modulo: String? = null, tiendaUsoId: Int? = null,
            proyectoRentecId: Int? = null, proyectoRentecFolio: String? = null,
        ) = "crear_activo?modulo=${modulo ?: ""}&tiendaUsoId=${tiendaUsoId ?: 0}" +
            "&proyectoRentecId=${proyectoRentecId ?: 0}&proyectoRentecFolio=${proyectoRentecFolio ?: ""}"
    }
    object Editar : Screen("editar_activo/{id}") { fun crear(id: Int) = "editar_activo/$id" }
    object Escaner : Screen(
        "escaner/{target}?prefijo={prefijo}&modoRegulador={modoRegulador}" +
            "&zoomAlto={zoomAlto}&cbLongitud={cbLongitud}&cbSoloDigitos={cbSoloDigitos}"
    ) {
        fun crear(
            target: String, prefijo: String? = null, modoRegulador: Boolean = false,
            zoomAlto: Boolean = false, cbLongitud: Int = 8, cbSoloDigitos: Boolean = true,
        ) = "escaner/$target?prefijo=$prefijo&modoRegulador=$modoRegulador" +
            "&zoomAlto=$zoomAlto&cbLongitud=$cbLongitud&cbSoloDigitos=$cbSoloDigitos"
    }
    object Usuarios : Screen("usuarios")
    object Historial : Screen("historial")
    object Tiendas : Screen("tiendas")
    object Modelos : Screen("modelos")
    object Pendientes : Screen("pendientes")
    object Solicitudes : Screen("solicitudes")
    object CrearSolicitud : Screen("crear_solicitud")
    object SolicitudDetalle : Screen("solicitud/{id}") { fun crear(id: Int) = "solicitud/$id" }
    // Inventario físico de bodega (auditoría por escaneo, migración 027).
    object InventarioBodega : Screen("inventario_bodega")
    object InventarioBodegaDetalle : Screen("inventario_bodega/{id}") { fun crear(id: Int) = "inventario_bodega/$id" }
    // Inventario físico de stock personal (Mi Stock / Stock PFS, migración 029).
    // Reusa InventarioBodegaDetalle para la pantalla de escaneo (es genérica).
    object InventarioStock : Screen("inventario_stock")
    // RENTEC: proyectos de Renovación Tecnológica (migración 028).
    object Rentec : Screen("rentec")
    object RentecDetalle : Screen("rentec/{id}") { fun crear(id: Int) = "rentec/$id" }
}
