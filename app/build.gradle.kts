plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kernel94.inventario123"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kernel94.inventario123"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.2.0"

        // Servidor productivo (alwaysdata). El path es case-sensitive: /Inventario123/ con "I" mayúscula.
        buildConfigField("String", "BASE_URL", "\"https://inventario123.alwaysdata.net/Inventario123/public/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// WorkManager 2.9.1 tira de lifecycle-service 2.9.4; el proyecto está alineado a
// lifecycle 2.8.4 (WorkManager 2.9.x funciona con 2.8.x). Fijamos las transitivas
// a 2.8.4 para no arrastrar todo el tren de 2.9.
configurations.all {
    resolutionStrategy {
        force(
            "androidx.lifecycle:lifecycle-service:2.8.4",
            "androidx.lifecycle:lifecycle-livedata-ktx:2.8.4",
            "androidx.lifecycle:lifecycle-runtime:2.8.4",
            "androidx.lifecycle:lifecycle-runtime-ktx:2.8.4",
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
