package com.kernel94.inventario123.data.sync

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.kernel94.inventario123.data.FirebaseModule
import com.kernel94.inventario123.data.repository.NotificacionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Escucha Firebase Realtime Database para refrescar Room DE INMEDIATO en
 * cuanto algo cambia (en vez de esperar al próximo ciclo de
 * [SyncWorker]/WorkManager). Por ahora cubre solo Notificaciones — el
 * único caso donde "que llegue al instante" importa de verdad; conforme se
 * construyan Bodega/Mi stock/Tiendas se añaden más `escuchar*` aquí mismo.
 *
 * Se arranca una vez por sesión (con el usuario_id ya conocido) y se detiene
 * al cerrar sesión — ver Inventario123App/AuthRepository.
 */
class SyncManager(
    private val notificacionRepository: NotificacionRepository,
) {
    private var refNotificaciones: DatabaseReference? = null
    private var listenerNotificaciones: ValueEventListener? = null

    fun iniciar(usuarioId: Int, scope: CoroutineScope) {
        detener() // por si ya había uno de una sesión anterior

        val db = FirebaseModule.database ?: return
        val ref = db.reference.child("notificaciones").child(usuarioId.toString())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // No hace falta leer el contenido del snapshot: más simple y
                // menos propenso a desincronizarse es solo refrescar desde
                // el backend, que es la fuente de verdad real (MySQL).
                scope.launch { notificacionRepository.sincronizar() }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("SyncManager", "Escucha de notificaciones cancelada: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        refNotificaciones = ref
        listenerNotificaciones = listener
    }

    fun detener() {
        listenerNotificaciones?.let { refNotificaciones?.removeEventListener(it) }
        refNotificaciones = null
        listenerNotificaciones = null
    }
}
