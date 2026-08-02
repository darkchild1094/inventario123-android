package com.kernel94.inventario123

import android.app.Application
import com.kernel94.inventario123.data.remote.ApiService
import com.kernel94.inventario123.data.remote.NetworkModule
import com.kernel94.inventario123.data.remote.SessionManager
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.ExportRepository
import com.kernel94.inventario123.data.repository.UsuarioRepository

class Inventario123App : Application() {
    lateinit var sessionManager: SessionManager private set
    lateinit var apiService: ApiService private set
    lateinit var authRepository: AuthRepository private set
    lateinit var activoRepository: ActivoRepository private set
    lateinit var catalogoRepository: CatalogoRepository private set
    lateinit var usuarioRepository: UsuarioRepository private set
    lateinit var exportRepository: ExportRepository private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        apiService = NetworkModule.crearApiService(this, sessionManager)
        authRepository = AuthRepository(apiService, sessionManager)
        activoRepository = ActivoRepository(apiService)
        catalogoRepository = CatalogoRepository(apiService)
        usuarioRepository = UsuarioRepository(apiService)
        exportRepository = ExportRepository(apiService)
    }
}
