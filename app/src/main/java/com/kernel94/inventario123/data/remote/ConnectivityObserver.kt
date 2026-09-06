package com.kernel94.inventario123.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Observa la conectividad y avisa cuando vuelve internet. */
class ConnectivityObserver(context: Context) {

    private val cm = context.getSystemService(ConnectivityManager::class.java)

    private val _online = MutableStateFlow(hayInternet())
    /** Estado reactivo de conexión, para que la UI muestre "sin conexión". */
    val online: StateFlow<Boolean> = _online

    init {
        // Un callback permanente mantiene [online] al día.
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _online.value = hayInternet() }
            override fun onLost(network: Network) { _online.value = hayInternet() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                _online.value = hayInternet()
            }
        })
    }

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
