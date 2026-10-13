package com.kernel94.inventario123.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    // Todo listado pasa por Modulo. La ruta "listado" (navegación vieja por
    // `vista=`, con reglas de permiso distintas a las de `modulo=`) se eliminó.
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
    // Alta/edicion de usuario en pantalla completa (antes era un dialogo
    // donde el contenido no cabia). id=0 -> alta nueva.
    object UsuarioForm : Screen("usuario_form/{id}") { fun crear(id: Int?) = "usuario_form/${id ?: 0}" }
    object Tiendas : Screen("tiendas")
    // Alta/edición de tienda. id=0 -> alta nueva.
    object TiendaForm : Screen("tienda_form/{id}") { fun crear(id: Int?) = "tienda_form/${id ?: 0}" }
    // Checklist de inventario POR TIENDA (migración 036) — no confundir con
    // InventarioBodega/InventarioStock (auditoría mensual, otro mecanismo).
    object InventarioTienda : Screen("inventario_tienda/{tiendaId}?nombre={nombre}") {
        fun crear(tiendaId: Int, nombre: String) = "inventario_tienda/$tiendaId?nombre=${java.net.URLEncoder.encode(nombre, "UTF-8")}"
    }
    object Modelos : Screen("modelos")
    // Notificaciones de "Enviar" (reemplaza Historial/Transferencias/
    // SolicitudTraslado/Altas pendientes de envío — ver rediseño).
    object Notificaciones : Screen("notificaciones")
    // Inventario físico de bodega (auditoría por escaneo, migración 027).
    object InventarioBodega : Screen("inventario_bodega")
    object InventarioBodegaDetalle : Screen("inventario_bodega/{id}") { fun crear(id: Int) = "inventario_bodega/$id" }
    // Inventario físico de stock personal (Mi Stock / Stock PFS, migración 029).
    // Reusa InventarioBodegaDetalle para la pantalla de escaneo (es genérica).
    object InventarioStock : Screen("inventario_stock")
    // RENTEC: proyectos de Renovación Tecnológica (migración 028).
    // Línea de tiempo con pestañas Bodega/Usuario/Tienda (net-new del rediseño).
    object Movimientos : Screen("movimientos")
    object Rentec : Screen("rentec")
    object RentecDetalle : Screen("rentec/{id}") { fun crear(id: Int) = "rentec/$id" }
}
