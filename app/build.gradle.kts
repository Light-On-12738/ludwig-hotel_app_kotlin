plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.ludwighotel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ludwighotel"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Android y Compose
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navegación
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Imágenes remotas, por ejemplo imagen_habitacion desde Supabase
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Serialización Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // Cliente HTTP requerido por Supabase
    implementation("io.ktor:ktor-client-android:3.1.1")

    // Compatibilidad Java para minSdk 24
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}