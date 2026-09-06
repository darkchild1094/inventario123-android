package com.kernel94.inventario123.data.model

import java.util.UUID

/**
 * Un alta de activo capturada sin señal, en cola para enviarse al recuperar
 * internet. Se persiste como JSON en filesDir; las fotos se guardan comprimidas
 * a archivos locales para que sobrevivan al cierre de la app.
 *
 * estado: pendiente → enviando → enviado (con serverId) | error (con mensaje)
 */
data class ActivoPendiente(
    val localId: String = UUID.randomUUID().toString(),
    val campos: Map<String, String> = emptyMap(),
    val fotoEquipoPath: String? = null,
    val fotoSeriePath: String? = null,
    val fotoActivoPath: String? = null,
    val estado: String = "pendiente",
    val serverId: Int? = null,
    val error: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
) {
    val titulo: String
        get() = listOfNotNull(
            campos["serie"]?.takeIf { it.isNotBlank() },
            campos["codigo_barras"]?.takeIf { it.isNotBlank() },
            campos["num_activo"]?.takeIf { it.isNotBlank() },
        ).firstOrNull() ?: "Activo sin identificar"
}
