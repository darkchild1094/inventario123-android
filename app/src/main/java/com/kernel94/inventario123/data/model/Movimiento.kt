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
    val marca_nombre: String? = null,
    val dispositivo_nombre: String? = null,
    val tienda_nombre: String? = null,
    val actor_nombre: String? = null,
    val relacionado_serie: String? = null,
    val stock_ant_tipo: String? = null,
    val stock_ant_nombre: String? = null,
    val stock_new_tipo: String? = null,
    val stock_new_nombre: String? = null,
    // Datos ya resueltos por Movimiento::enriquecer() en el servidor: se prefiere
    // el snapshot congelado en datos_json y se cae a las columnas vivas del JOIN.
    val eq_dispositivo: String? = null,
    val eq_marca: String? = null,
    val eq_modelo: String? = null,
    val eq_serie: String? = null,
    val eq_codigo_barras: String? = null,
    val eq_num_activo: String? = null,
    val eq_status: String? = null,
    val rel_dispositivo: String? = null,
    val rel_marca: String? = null,
    val rel_modelo: String? = null,
    val rel_serie: String? = null,
    val rel_codigo_barras: String? = null,
    val rel_num_activo: String? = null,
) {
    val eventoLabel: String get() = EVENTOS[evento] ?: evento

    val hayCambioStatus: Boolean
        get() = !status_anterior.isNullOrBlank() || !status_nuevo.isNullOrBlank()

    val hayCambioStock: Boolean
        get() = !stock_new_nombre.isNullOrBlank() && stock_ant_nombre != stock_new_nombre

    // ── Presentación del equipo (y del equipo relacionado en reemplazos) ──────

    private fun d(v: String?): String = v?.trim()?.takeIf { it.isNotEmpty() } ?: "—"

    private fun titulo(disp: String?, marca: String?, modelo: String?): String {
        val mm = listOfNotNull(marca?.trim()?.ifEmpty { null }, modelo?.trim()?.ifEmpty { null }).joinToString(" ")
        val ds = disp?.trim().orEmpty()
        return when {
            ds.isNotEmpty() && mm.isNotEmpty() -> "$ds · $mm"
            ds.isNotEmpty() -> ds
            mm.isNotEmpty() -> mm
            else -> "—"
        }
    }

    val equipoTitulo: String
        get() = titulo(eq_dispositivo ?: dispositivo_nombre, eq_marca ?: marca_nombre, eq_modelo ?: modelo_nombre)

    val equipoIds: String
        get() = "S/N: ${d(eq_serie ?: activo_serie)}  ·  CB: ${d(eq_codigo_barras ?: activo_codigo_barras)}  ·  AF: ${d(eq_num_activo ?: activo_num_activo)}"

    val tieneRelacionado: Boolean
        get() = !rel_serie.isNullOrBlank() || !rel_codigo_barras.isNullOrBlank() ||
            !rel_num_activo.isNullOrBlank() || !rel_modelo.isNullOrBlank() ||
            !rel_dispositivo.isNullOrBlank() || !relacionado_serie.isNullOrBlank()

    val relTitulo: String
        get() = titulo(rel_dispositivo, rel_marca, rel_modelo)

    val relIds: String
        get() = "S/N: ${d(rel_serie ?: relacionado_serie)}  ·  CB: ${d(rel_codigo_barras)}  ·  AF: ${d(rel_num_activo)}"

    val relLabel: String
        get() = when (evento) {
            "reemplazo_entra" -> "Sale (equipo retirado)"
            "reemplazo_sale" -> "Entra (equipo instalado)"
            else -> "Equipo relacionado"
        }

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
