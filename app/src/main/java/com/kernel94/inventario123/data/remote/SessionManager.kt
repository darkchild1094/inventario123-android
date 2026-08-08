package com.kernel94.inventario123.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kernel94.inventario123.data.model.CuentaGuardada
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
        private val KEY_CUENTAS_GUARDADAS = stringPreferencesKey("cuentas_guardadas")
    }

    private val gson = Gson()

    val sessionIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_SESSION_ID] }
    val tipoFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USUARIO_TIPO] }
    val nombreFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USUARIO_NOMBRE] }

    val cuentasGuardadasFlow: Flow<List<CuentaGuardada>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_CUENTAS_GUARDADAS]
        if (json.isNullOrBlank()) emptyList()
        else {
            val type = object : TypeToken<List<CuentaGuardada>>() {}.type
            gson.fromJson(json, type)
        }
    }

    suspend fun sessionIdActual(): String? = sessionIdFlow.first()

    suspend fun guardarSesion(sessionId: String, usuarioId: Int, nombre: String, tipo: String, email: String, foto: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = sessionId
            prefs[KEY_USUARIO_ID] = usuarioId.toString()
            prefs[KEY_USUARIO_NOMBRE] = nombre
            prefs[KEY_USUARIO_TIPO] = tipo

            // Actualizar lista de cuentas guardadas
            val json = prefs[KEY_CUENTAS_GUARDADAS]
            val type = object : TypeToken<List<CuentaGuardada>>() {}.type
            val lista: MutableList<CuentaGuardada> = if (json.isNullOrBlank()) mutableListOf() 
                else gson.fromJson(json, type)
            
            lista.removeAll { it.email == email }
            lista.add(0, CuentaGuardada(usuarioId, nombre, email, foto))
            prefs[KEY_CUENTAS_GUARDADAS] = gson.toJson(lista.take(5)) // Guardar las últimas 5
        }
    }

    suspend fun eliminarCuentaGuardada(email: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_CUENTAS_GUARDADAS]
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<List<CuentaGuardada>>() {}.type
                val lista: MutableList<CuentaGuardada> = gson.fromJson(json, type)
                lista.removeAll { it.email == email }
                prefs[KEY_CUENTAS_GUARDADAS] = gson.toJson(lista)
            }
        }
    }

    suspend fun cerrarSesion() { context.dataStore.edit { prefs ->
        prefs.remove(KEY_SESSION_ID)
        prefs.remove(KEY_USUARIO_ID)
        prefs.remove(KEY_USUARIO_NOMBRE)
        prefs.remove(KEY_USUARIO_TIPO)
    } }
    suspend fun haySesionActiva(): Boolean = !sessionIdActual().isNullOrBlank()
}
