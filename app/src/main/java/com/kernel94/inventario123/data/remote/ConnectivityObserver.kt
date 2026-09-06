package com.kernel94.inventario123.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/** Observa la conectividad y avisa cuando vuelve internet. */
class ConnectivityObserver(context: Context) {

    private val cm = context.getSystemService(ConnectivityManager::class.java)

    fun hayInternet(): Boolean {
        val red = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(red) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Registra un callback que se dispara cada vez que hay una red disponible. */
    fun alRecuperarSeñal(accion: () -> Unit) {
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { accion() }
        })
    }
}
