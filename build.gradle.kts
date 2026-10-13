plugins {
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    // Firebase (Realtime Database + Auth anónima) y Room (offline-first),
    // mismo patrón que POS123 — ver data/FirebaseModule.kt y data/local/db/.
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
}
