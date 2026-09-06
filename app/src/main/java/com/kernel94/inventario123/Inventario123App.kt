package com.kernel94.inventario123

import android.app.Application
import com.kernel94.inventario123.data.local.PendientesStore
import com.kernel94.inventario123.data.remote.ApiService
import com.kernel94.inventario123.data.remote.ConnectivityObserver
import com.kernel94.inventario123.data.remote.NetworkModule
import com.kernel94.inventario123.data.remote.SessionManager
import com.kernel94.inventario123.data.repository.ActivoRepository
import com.kernel94.inventario123.data.repository.AuthRepository
import com.kernel94.inventario123.data.repository.CatalogoRepository
import com.kernel94.inventario123.data.repository.ExportRepository
import com.kernel94.inventario123.data.repository.ModeloRepository
import com.kernel94.inventario123.data.repository.MovimientoRepository
import com.kernel94.inventario123.data.repository.PendientesRepository
import com.kernel94.inventario123.data.repository.SolicitudRepository
import com.kernel94.inventario123.data.repository.TiendaRepository
import com.kernel94.inventario123.data.repository.UsuarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Inventario123App : Application() {
    lateinit var sessionManager: SessionManager private set
    lateinit var apiService: ApiService private set
    lateinit var authRepository: AuthRepository private set
    lateinit var activoRepository: ActivoRepository private set
    lateinit var catalogoRepository: CatalogoRepository private set
    lateinit var usuarioRepository: UsuarioRepository private set
    lateinit var exportRepository: ExportRepository private set
    lateinit var movimientoRepository: MovimientoRepository private set
    lateinit var tiendaRepository: TiendaRepository private set
    lateinit var modeloRepository: ModeloRepository private set
    lateinit var solicitudRepository: SolicitudRepository private set
    lateinit var pendientesRepository: PendientesRepository private set
    lateinit var connectivityObserver: ConnectivityObserver private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        apiService = NetworkModule.crearApiService(this, sessionManager)
        authRepository = AuthRepository(apiService, sessionManager)
        activoRepository = ActivoRepository(apiService)
        catalogoRepository = CatalogoRepository(apiService, this)
        usuarioRepository = UsuarioRepository(apiService)
        exportRepository = ExportRepository(apiService)
        movimientoRepository = MovimientoRepository(apiService)
        tiendaRepository = TiendaRepository(apiService)
        modeloRepository = ModeloRepository(apiService)
        solicitudRepository = SolicitudRepository(apiService)

        connectivityObserver = ConnectivityObserver(this)
        pendientesRepository = PendientesRepository(PendientesStore(this), activoRepository, connectivityObserver)

        // Vaciar la cola al arrancar y cada vez que vuelva la señal.
        appScope.launch { runCatching { pendientesRepository.sincronizar() } }
        connectivityObserver.alRecuperarSeñal {
            appScope.launch { runCatching { pendientesRepository.sincronizar() } }
        }
    }
}
