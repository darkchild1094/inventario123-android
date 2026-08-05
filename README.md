# Inventario123 — App Android (Kotlin + Jetpack Compose)

App móvil para el sistema de control de activos FEMSA/OXXO/BARA, hermana de la
versión web (repo `Inventario123`). Consume el mismo backend PHP, vía un API
JSON nuevo en `ApiController.php` (ver sección Backend abajo).

## Cómo abrir el proyecto

1. Abre Android Studio (Koala o más reciente) → **Open** → selecciona esta carpeta.
2. Deja que Gradle sincronice (necesita internet la primera vez, para bajar
   Kotlin, Compose, Retrofit, CameraX, ML Kit, etc.).
3. Antes de correrla, edita `app/build.gradle.kts` → `buildConfigField("String", "BASE_URL", ...)`
   y pon la URL real de tu backend. **Importante:** no lleva `/public/` al final —
   tu sitio en alwaysdata ya apunta internamente a esa carpeta, así que la URL
   externa correcta es:
   ```
   "https://fieldserviceplus.alwaysdata.net/inventario123/"
   ```
4. Corre en un emulador o dispositivo físico (mínimo Android 8.0 / API 26).

## Backend (IMPORTANTE — súbelo también)

Esta app depende de una versión reforzada de `app/controllers/ApiController.php`
y de un método nuevo `Activo::ultimoId()`, que ya deberías tener si aplicaste
los últimos archivos que te pasé en el chat para el repo PHP. Si no, la app
no podrá loguearse ni cargar nada. Confírmalo antes de probar.

## Qué SÍ incluye esta versión (fase 1 + fase 2)

- **Login** contra el mismo sistema de usuarios de la web (sesión vía header
  `X-Session-Id`, pensado para clientes móviles — ya estaba soportado en
  `public/index.php`).
- **Listado de activos** con las pestañas Bodega / Mi Stock / Todos (según tu
  rol, igual que en la navbar web), búsqueda en tiempo real, y filtros de
  Negocio → Región → Plaza → Usuario en cascada.
- **Detalle de activo** (pantalla completa) con toda la info, y botones de
  Editar/Eliminar solo si tu rol lo permite (mismas reglas que la web:
  admin todo, coordinador sus plazas, ati lo suyo, fs lo suyo).
- **Crear / Editar activo**, con los mismos campos condicionales según
  estatus que la web (`manejarEstatus()`), y conservando la configuración
  entre un registro y el siguiente (solo se limpian serie/placa/procedencia).
- **Escáner de cámara** (botón junto al campo Serie): lee código de barras/QR
  con ML Kit, y si no encuentra ninguno cae a reconocimiento de texto (OCR)
  para que puedas tocar la línea correcta de la etiqueta. Esto no existe en
  la web — es una funcionalidad nueva, exclusiva de la app.
- **Gestión de usuarios** (admin/coordinador): listar, crear, editar con
  checkboxes de plazas (igual que ya corregimos en la web), eliminar.
- **Exportar a Excel** (botón en la barra superior, solo si tu rol puede
  exportar): descarga el mismo archivo que genera la web y abre el menú de
  compartir de Android (WhatsApp, correo, Drive, guardar, etc.). De paso se
  corrigió un bug de seguridad real en el backend: `ExportController` no
  aplicaba ningún filtro por rol, así que un coordinador o ati podía
  descargar el inventario completo de la empresa en vez de solo el de sus
  plazas asignadas — ya corregido en el repo del backend.
- **Ícono propio** de la app (caja de inventario sobre fondo azul, el mismo
  azul primario de Bootstrap que usa la web).
- **Sesión expirada**: si el servidor responde que la sesión ya no es válida
  (por ejemplo llevas mucho tiempo sin usar la app), te regresa sola a la
  pantalla de Login, sin importar en qué parte de la app estabas.

## Qué falta para una fase 3 (te lo puedo seguir armando)

- Pruebas de extremo a extremo reales en Android Studio — este proyecto se
  escribió fuera de un entorno con SDK de Android, así que aunque el código
  es sintácticamente correcto y sigue el patrón estándar de Compose/Retrofit,
  la primera sincronización de Gradle puede sacar algún detalle menor que
  ajustar (versión de alguna librería, etc.).
- Modo sin conexión / caché local (ahora mismo todo requiere internet).
- Notificaciones push, si algún día las necesitas.

## Arquitectura

- **Sin Hilt/Dagger** — service locator manual simple en `Inventario123App.kt`
  y `ui/common/ViewModelFactory.kt`, para que sea fácil de leer y mantener
  sin tener que aprenderte un framework de inyección de dependencias.
- **Retrofit + Gson** para la red, **DataStore** para persistir la sesión.
- **CameraX + ML Kit** para el escáner (sin dependencias de pago).
- Los modelos de datos (`data/model/`) usan los mismos nombres de campo que
  el JSON del backend (snake_case) a propósito, para evitar bugs de mapeo.
