package com.kernel94.inventario123.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cuando cualquier llamada al API responde 401 (sesión inválida o expirada
 * en el servidor), SessionInterceptor emite aquí. MainActivity/NavGraph lo
 * escuchan para mandar de regreso a Login limpiando el stack de navegación,
 * sin importar en qué pantalla estaba el usuario.
 */
object SessionExpiredNotifier {
    private val _eventos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val eventos: SharedFlow<Unit> = _eventos.asSharedFlow()

    fun notificar() {
        _eventos.tryEmit(Unit)
    }
}
