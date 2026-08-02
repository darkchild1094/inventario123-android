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
 * También detecta sesión expirada/inválida (401 del servidor): limpia la
 * sesión local guardada y notifica a la UI para regresar a Login, sin
 * importar en qué pantalla estaba el usuario cuando pasó.
 */
class SessionInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionId = runBlocking { sessionManager.sessionIdActual() }
        val requestBuilder = chain.request().newBuilder()
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Accept", "application/json")

        if (!sessionId.isNullOrBlank()) {
            requestBuilder.addHeader("X-Session-Id", sessionId)
        }

        val response = chain.proceed(requestBuilder.build())

        // No aplica al propio endpoint de login (ahí un 401 es "credenciales
        // incorrectas", no "sesión expirada" — no debe disparar el logout global).
        val esLogin = chain.request().url.queryParameter("action") == "login"
        if (response.code == 401 && !esLogin && !sessionId.isNullOrBlank()) {
            runBlocking { sessionManager.cerrarSesion() }
            SessionExpiredNotifier.notificar()
        }

        return response
    }
}
