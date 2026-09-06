package com.kernel94.inventario123.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega X-Session-Id (para que public/index.php reconozca la sesión PHP sin
 * depender de cookies) y X-Requested-With (para que los controladores
 * respondan JSON en vez de HTML/redirects, mismo mecanismo que ya usa el
 * front-end web para sus llamadas AJAX).
 *
 * Ante un 401 del servidor NO cierra sesión al primer intento: reintenta la
 * misma petición una vez (un blip de red o una carrera no deben tirar la
 * sesión). Solo si el reintento también da 401 limpia la sesión local y
 * notifica a la UI para volver a Login.
 */
class SessionInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionId = runBlocking { sessionManager.sessionIdActual() }

        fun construir() = chain.request().newBuilder()
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "application/json")
            .apply { if (!sessionId.isNullOrBlank()) header("X-Session-Id", sessionId) }
            .build()

        var response = chain.proceed(construir())

        // No aplica al propio endpoint de login (ahí un 401 es "credenciales
        // incorrectas", no "sesión expirada" — no debe disparar el logout global).
        val esLogin = chain.request().url.queryParameter("action") == "login"
        if (response.code == 401 && !esLogin && !sessionId.isNullOrBlank()) {
            // Segundo intento tras una pequeña pausa.
            response.close()
            try { Thread.sleep(400) } catch (_: InterruptedException) {}
            response = chain.proceed(construir())

            if (response.code == 401) {
                runBlocking { sessionManager.cerrarSesion() }
                SessionExpiredNotifier.notificar()
            }
        }

        return response
    }
}
