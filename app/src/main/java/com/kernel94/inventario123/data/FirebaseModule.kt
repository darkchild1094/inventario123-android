package com.kernel94.inventario123.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Punto único de acceso a Firebase (sin Hilt, mismo patrón que NetworkModule).
 * Copiado tal cual de POS123 (`com.kernel94.pos123.data.FirebaseModule`) — es
 * el canal de sincronización inmediata entre dispositivos (Realtime
 * Database) con Auth anónima solo para pasar las reglas (`auth != null`);
 * el backend PHP sigue siendo la base de datos autoritativa de negocio.
 *
 * Si `app/google-services.json` todavía no existiera, [init] no truena la
 * app: solo deja [database]/[auth] en null y cada repositorio debe tratar
 * eso como "sin sincronización inmediata disponible" (no es el caso aquí:
 * el proyecto Firebase `inventario123-2dec6` ya está creado y el archivo ya
 * está en su lugar).
 */
object FirebaseModule {

    private const val TAG = "FirebaseModule"

    var database: FirebaseDatabase? = null
        private set
    var auth: FirebaseAuth? = null
        private set

    val isConfigured: Boolean get() = database != null

    fun init(context: Context) {
        try {
            val app = FirebaseApp.initializeApp(context) ?: return
            database = FirebaseDatabase.getInstance(app).apply {
                setPersistenceEnabled(true)
            }
            auth = FirebaseAuth.getInstance(app)
            signInAnonimoSiHaceFalta()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase no está configurado todavía (falta google-services.json)", e)
        }
    }

    private fun signInAnonimoSiHaceFalta() {
        val firebaseAuth = auth ?: return
        if (firebaseAuth.currentUser == null) {
            firebaseAuth.signInAnonymously()
                .addOnFailureListener { e -> Log.w(TAG, "Falló el login anónimo de Firebase", e) }
        }
    }

    /**
     * Las reglas de RTDB exigen `auth != null`. El login anónimo de [init] es asíncrono, así que
     * cualquier lectura/escritura debe esperar aquí primero o el servidor la rechaza con
     * "Permission denied" (y esa excepción, si no se atrapa, tumba la app entera).
     */
    suspend fun esperarAuthListo() {
        val firebaseAuth = auth ?: return
        if (firebaseAuth.currentUser != null) return
        // Si el login anónimo nunca amarra (p. ej. no está habilitado en la consola), no nos
        // quedamos colgados para siempre: seguimos y dejamos que la llamada de Firebase truene
        // con su propio error, ya atrapado más arriba en cada repositorio.
        withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine<Unit> { cont ->
                val listener = object : FirebaseAuth.AuthStateListener {
                    override fun onAuthStateChanged(authInstance: FirebaseAuth) {
                        if (authInstance.currentUser != null) {
                            authInstance.removeAuthStateListener(this)
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                }
                firebaseAuth.addAuthStateListener(listener)
                cont.invokeOnCancellation { firebaseAuth.removeAuthStateListener(listener) }
            }
        }
    }
}
