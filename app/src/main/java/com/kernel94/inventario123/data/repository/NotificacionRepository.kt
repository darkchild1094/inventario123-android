package com.kernel94.inventario123.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kernel94.inventario123.data.FirebaseModule
import com.kernel94.inventario123.data.local.db.AppDatabase
import com.kernel94.inventario123.data.local.db.NotificacionEntity
import com.kernel94.inventario123.data.model.NotifActivo
import com.kernel94.inventario123.data.model.Notificacion
import com.kernel94.inventario123.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Notificaciones de "Enviar" (Bodega↔Mi stock↔usuario, reemplaza
 * Transferencias/SolicitudTraslado). Offline-first: las pantallas observan
 * Room (nunca la API directo); [sincronizar] la puebla desde el backend y
 * [escucharEnVivo] dispara un refresh inmediato cuando Firebase avisa que
 * llegó algo nuevo — ver FirebaseModule y SyncManager.
 */
class NotificacionRepository(
    private val api: ApiService,
    private val db: AppDatabase,
) {
    private val gson = Gson()
    private val dao get() = db.notificacionDao()
    private val listaActivosType = object : TypeToken<List<NotifActivo>>() {}.type

    fun observar(): Flow<List<Notificacion>> = dao.observarTodas().map { filas -> filas.map(::aModelo) }

    fun observarNoLeidas(): Flow<Int> = dao.observarNoLeidas()

    private fun aModelo(e: NotificacionEntity): Notificacion = Notificacion(
        id = e.id,
        usuario_destino_id = e.usuarioDestinoId,
        de_usuario_id = e.deUsuarioId,
        de_nombre = e.deNombre,
        direccion = e.direccion,
        lugar = e.lugar,
        grupo_id = e.grupoId,
        leida = e.leida,
        creado_en = e.creadoEn,
        activos = runCatching { gson.fromJson<List<NotifActivo>>(e.activosJson, listaActivosType) }.getOrDefault(emptyList()),
    )

    /** Trae del backend y reemplaza la caché local completa (lista chica, no justifica merge incremental). */
    suspend fun sincronizar(): Boolean = try {
        val lista = api.listarNotificaciones()
        val entidades = lista.map { n ->
            NotificacionEntity(
                id = n.id,
                usuarioDestinoId = n.usuario_destino_id,
                deUsuarioId = n.de_usuario_id,
                deNombre = n.de_nombre,
                direccion = n.direccion,
                lugar = n.lugar,
                grupoId = n.grupo_id,
                leida = n.leida,
                creadoEn = n.creado_en,
                activosJson = gson.toJson(n.activos),
            )
        }
        dao.reemplazarTodas(entidades)
        true
    } catch (e: Exception) {
        false
    }

    /** $id null = todas. Marca en Room de inmediato (responsivo), confirma con el backend, y limpia el espejo de Firebase. */
    suspend fun marcarLeida(id: Int?, miUsuarioId: Int) {
        if (id != null) dao.marcarLeidaLocal(id) else dao.marcarTodasLeidasLocal()
        try {
            api.marcarNotificacionLeida(if (id != null) mapOf("id" to id) else emptyMap())
        } catch (_: Exception) { /* Room ya quedó al día; el backend se alcanza en el próximo sync si esto falló. */ }

        // Firebase aquí es un buzón efímero (solo "avisa ya"): se borra el
        // nodo espejo al leerla, igual que hace el backend (ver
        // FirebaseNotifier::borrarAviso) — así el árbol no crece sin límite.
        val ref = FirebaseModule.database?.reference
            ?.child("notificaciones")?.child(miUsuarioId.toString())
        try {
            FirebaseModule.esperarAuthListo()
            if (id != null) ref?.child(id.toString())?.removeValue() else ref?.removeValue()
        } catch (_: Exception) { }
    }

    /**
     * "Enviar" N activos a bodega o a un usuario — inmediato, sin
     * aceptación. Si el backend confirma éxito, la app misma escribe el
     * espejo en Firebase para cada destinatario (canal confiable; el
     * backend también lo intenta pero es de mejor esfuerzo — ver
     * FirebaseNotifier.php). Un fallo al escribir el espejo NO revierte el
     * envío: el activo ya se movió y la notificación ya quedó en MySQL.
     */
    suspend fun enviar(
        activos: List<Int>,
        tipo: String, // "bodega" | "usuario"
        plazaId: Int? = null,
        usuarioId: Int? = null,
    ): Resultado<String> = try {
        val body = buildMap<String, Any?> {
            put("activos", activos)
            put("tipo", tipo)
            if (plazaId != null) put("plaza_id", plazaId)
            if (usuarioId != null) put("usuario_id", usuarioId)
        }
        val r = api.enviarActivos(body)
        if (r.success) {
            escribirAvisosEnFirebase(r.avisos)
            Resultado.Exito(r.message ?: "Enviado.")
        } else {
            Resultado.Error(r.message ?: "No se pudo enviar.")
        }
    } catch (e: Exception) {
        Resultado.Error("No se pudo conectar al servidor.")
    }

    private suspend fun escribirAvisosEnFirebase(avisos: List<com.kernel94.inventario123.data.model.AvisoEnvio>) {
        if (avisos.isEmpty()) return
        try {
            FirebaseModule.esperarAuthListo()
            val raiz = FirebaseModule.database?.reference?.child("notificaciones") ?: return
            for (aviso in avisos) {
                raiz.child(aviso.usuario_destino_id.toString())
                    .child(aviso.notificacion_id.toString())
                    .setValue(aviso.datos)
            }
        } catch (_: Exception) {
            // Mejor esfuerzo: el envío ya se completó en el backend de todas
            // formas; sin este espejo, el destinatario la ve en su próximo
            // sync normal en vez de al instante.
        }
    }
}
