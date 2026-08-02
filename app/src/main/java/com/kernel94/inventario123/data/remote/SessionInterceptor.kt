package com.kernel94.inventario123.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class SessionInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionId = runBlocking { sessionManager.sessionIdActual() }
        val requestBuilder = chain.request().newBuilder()
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Accept", "application/json")
        if (!sessionId.isNullOrBlank()) requestBuilder.addHeader("X-Session-Id", sessionId)
        return chain.proceed(requestBuilder.build())
    }
}
