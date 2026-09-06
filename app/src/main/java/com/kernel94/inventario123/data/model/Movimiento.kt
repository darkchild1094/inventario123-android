package com.kernel94.inventario123.data.model

/**
 * Bitácora de un activo (tabla `movimiento`, migración 002). Alimenta la
 * pestaña Historial y la línea de tiempo del detalle. Espeja el SELECT de
 * App\Models\Movimiento::selectBase() en el web.
 */
data class Movimiento(
    val id: Int = 0,
    val activo_id: Int? = null,
    val evento: String = "",
    val status_anterior: String? = null,
    val status_nuevo: String? = null,
    val stock_anterior_id: Int? = null,
    val stock_nuevo_id: Int? = null,
    val tienda_id: Int? = null,
    val plaza_id: Int? = null,
    val activo_relacionado_id: Int? = null,
    val grupo_id: String? = null,
    val usuario_id: Int? = null,
    val nota: String? = null,
    val creado_en: String? = null,
    val activo_serie: String? = null,
    val activo_codigo_barras: String? = null,
    val activo_num_activo: String? = null,
    val modelo_nombre: String? = null,
    val dispositivo_nombre: String? = null,
    val tienda_nombre: String? = null,
    val actor_nombre: String? = null,
    val relacionado_serie: String? = null,
    val stock_ant_tipo: String? = null,
    val stock_ant_nombre: String? = null,
    val stock_new_tipo: String? = null,
    val stock_new_nombre: String? = null,
) {
    val eventoLabel: String get() = EVENTOS[evento] ?: evento

    val hayCambioStatus: Boolean
        get() = !status_anterior.isNullOrBlank() || !status_nuevo.isNullOrBlank()

    val hayCambioStock: Boolean
        get() = !stock_new_nombre.isNullOrBlank() && stock_ant_nombre != stock_new_nombre

    companion object {
        val EVENTOS = linkedMapOf(
            "alta" to "Alta",
            "cambio_status" to "Cambio de estatus",
            "cambio_stock" to "Cambio de stock",
            "reemplazo_entra" to "Entra por reemplazo",
            "reemplazo_sale" to "Sale por reemplazo",
            "edicion" to "Edición",
            "baja" to "Baja",
            "eliminacion" to "Eliminación",
        )
    }
}

data class ListadoMovimientosResponse(
    val movimientos: List<Movimiento> = emptyList(),
    val paginacion: Paginacion = Paginacion(),
)
