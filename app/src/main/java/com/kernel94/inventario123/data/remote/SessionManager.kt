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
        // Contraseñas recordadas por correo (solo en este dispositivo; mismo nivel
        // de exposición que el session_id que ya se guarda aquí).
        private val KEY_PASSWORDS = stringPreferencesKey("passwords_recordadas")
    }

    private val gson = Gson()
    private val listaType = object : TypeToken<MutableList<CuentaGuardada>>() {}.type
    private val mapaType = object : TypeToken<MutableMap<String, String>>() {}.type

    val sessionIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_SESSION_ID] }
    val tipoFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USUARIO_TIPO] }
    val nombreFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USUARIO_NOMBRE] }

    val cuentasGuardadasFlow: Flow<List<CuentaGuardada>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_CUENTAS_GUARDADAS]
        if (json.isNullOrBlank()) emptyList() else gson.fromJson(json, listaType)
    }

    /** Correos que tienen una contraseña recordada en este dispositivo. */
    val correosConPasswordFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_PASSWORDS]
        if (json.isNullOrBlank()) emptySet()
        else gson.fromJson<MutableMap<String, String>>(json, mapaType).keys
    }

    suspend fun sessionIdActual(): String? = sessionIdFlow.first()

    suspend fun guardarSesion(
        sessionId: String, usuarioId: Int, nombre: String, tipo: String,
        email: String, foto: String?, password: String? = null, recordarPassword: Boolean = false,
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = sessionId
            prefs[KEY_USUARIO_ID] = usuarioId.toString()
            prefs[KEY_USUARIO_NOMBRE] = nombre
            prefs[KEY_USUARIO_TIPO] = tipo

            val lista: MutableList<CuentaGuardada> = prefs[KEY_CUENTAS_GUARDADAS]?.takeIf { it.isNotBlank() }
                ?.let { gson.fromJson(it, listaType) } ?: mutableListOf()
            lista.removeAll { it.email == email }
            lista.add(0, CuentaGuardada(usuarioId, nombre, email, foto))
            prefs[KEY_CUENTAS_GUARDADAS] = gson.toJson(lista.take(5))

            val mapa: MutableMap<String, String> = prefs[KEY_PASSWORDS]?.takeIf { it.isNotBlank() }
                ?.let { gson.fromJson(it, mapaType) } ?: mutableMapOf()
            if (recordarPassword && !password.isNullOrEmpty()) mapa[email] = password else mapa.remove(email)
            prefs[KEY_PASSWORDS] = gson.toJson(mapa)
        }
    }

    suspend fun passwordRecordada(email: String): String? {
        val json = context.dataStore.data.first()[KEY_PASSWORDS] ?: return null
        return gson.fromJson<MutableMap<String, String>>(json, mapaType)[email]
    }

    suspend fun eliminarCuentaGuardada(email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUENTAS_GUARDADAS]?.takeIf { it.isNotBlank() }?.let {
                val lista: MutableList<CuentaGuardada> = gson.fromJson(it, listaType)
                lista.removeAll { c -> c.email == email }
                prefs[KEY_CUENTAS_GUARDADAS] = gson.toJson(lista)
            }
            prefs[KEY_PASSWORDS]?.takeIf { it.isNotBlank() }?.let {
                val mapa: MutableMap<String, String> = gson.fromJson(it, mapaType)
                mapa.remove(email)
                prefs[KEY_PASSWORDS] = gson.toJson(mapa)
            }
        }
    }

    /** Cierra la sesión activa. Conserva las cuentas y contraseñas recordadas. */
    suspend fun cerrarSesion() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SESSION_ID)
            prefs.remove(KEY_USUARIO_ID)
            prefs.remove(KEY_USUARIO_NOMBRE)
            prefs.remove(KEY_USUARIO_TIPO)
        }
    }

    suspend fun haySesionActiva(): Boolean = !sessionIdActual().isNullOrBlank()
}
