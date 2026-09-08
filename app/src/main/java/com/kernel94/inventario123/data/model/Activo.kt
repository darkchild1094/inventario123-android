package com.kernel94.inventario123.data.model

import com.google.gson.annotations.SerializedName

data class Activo(
    val id: Int = 0,
    val serie: String? = null,
    // La columna `activo.placa` se renombró a `activo.codigo_barras` en el web
    // (migración 005). `num_activo` es nuevo (migración 006).
    @SerializedName("codigo_barras") val codigoBarras: String? = null,
    @SerializedName("num_activo") val numActivo: String? = null,
    // Fotos del activo (migración 007): nombre de archivo en /uploads/, o null.
    val foto_equipo: String? = null,
    val foto_serie: String? = null,
    val foto_activo: String? = null,
    val modelo_id: Int? = null,
    val status: String = "en_bodega",
    val procedencia_tienda_id: Int? = null,
    val tienda_uso_id: Int? = null,
    val stock_id: Int? = null,
    val fecha_alta: String? = null,
    val fecha_modificacion: String? = null,
    val modelo_nombre: String? = null,
    val marca_nombre: String? = null,
    val dispositivo_id: Int? = null,
    val dispositivo_nombre: String? = null,
    val area_nombre: String? = null,
    val stock_tipo: String? = null,
    val usuario_stock_id: Int? = null,
    val usuario_nombre: String? = null,
    val bodega_stock_id: Int? = null,
    val bodega_nombre: String? = null,
    // Stock por tienda (migración 001): un activo "en uso" vive en el stock de su tienda.
    val tienda_stock_id: Int? = null,
    val tienda_stock_nombre: String? = null,
    val plaza_id: Int? = null,
    val plaza_nombre: String? = null,
    val region_nombre: String? = null,
    val negocio_nombre: String? = null,
    val tienda_uso_nombre: String? = null,
    val procedencia_nombre: String? = null,
    val puedeEditar: Boolean = false,
    val puedeEliminar: Boolean = false,
) {
    val statusLabel: String
        get() = when (status) {
            "en_bodega" -> "En Bodega"; "en_uso" -> "En Uso"; "baja" -> "Baja"
            "garantia" -> "Garantía"; "asignado" -> "Asignado"; else -> "Desconocido"
        }

    /** Etiqueta del renglón de ubicación, mismo criterio que _detalle_activo.php */
    val ubicacionLabel: String
        get() = when (stock_tipo) {
            "usuario" -> "Asignado a"
            "tienda" -> "Stock de tienda"
            else -> "Bodega"
        }

    /** Valor del renglón de ubicación según el tipo de stock. */
    val ubicacionValor: String
        get() = when (stock_tipo) {
            "usuario" -> usuario_nombre ?: "—"
            "tienda" -> tienda_stock_nombre ?: "—"
            else -> bodega_nombre ?: "—"
        }

    @Deprecated("Renombrado a codigoBarras", ReplaceWith("codigoBarras"))
    val placa: String? get() = codigoBarras

    val asignadoOBodega: String get() = ubicacionValor
}

data class Paginacion(
    val pagina_actual: Int = 1, val total_paginas: Int = 1,
    val total_resultados: Int = 0, val por_pagina: Int = 20,
)

data class ListadoActivosResponse(
    val activos: List<Activo> = emptyList(),
    val paginacion: Paginacion = Paginacion(),
    val vista: String? = null,
    val modulo: String? = null,
    val moduloEditable: Boolean = false,
)
