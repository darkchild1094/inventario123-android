package com.kernel94.inventario123.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "inventario123_session")

class SessionManager(private val context: Context) {
    companion object {
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_USUARIO_NOMBRE = stringPreferencesKey("usuario_nombre")
        private val KEY_USUARIO_TIPO = stringPreferencesKey("usuario_tipo")
        private val KEY_USUARIO_ID = stringPreferencesKey("usuario_id")
    }

    val sessionIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_SESSION_ID] }
    val tipoFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USUARIO_TIPO] }
    val nombreFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USUARIO_NOMBRE] }

    suspend fun sessionIdActual(): String? = sessionIdFlow.first()

    suspend fun guardarSesion(sessionId: String, usuarioId: Int, nombre: String, tipo: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = sessionId
            prefs[KEY_USUARIO_ID] = usuarioId.toString()
            prefs[KEY_USUARIO_NOMBRE] = nombre
            prefs[KEY_USUARIO_TIPO] = tipo
        }
    }

    suspend fun cerrarSesion() { context.dataStore.edit { it.clear() } }
    suspend fun haySesionActiva(): Boolean = !sessionIdActual().isNullOrBlank()
}
