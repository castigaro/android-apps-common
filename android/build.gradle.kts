// Gemeinsame Bibliothek der AppSonar-Apps: Basis-Theme, Toolbar-Stil und
// Update-Erkennung. Wird von den Apps über einen relativen Pfad eingebunden
// (siehe settings.gradle.kts der jeweiligen App) — kein eigener Gradle-Wrapper.
//
// Prinzip "Lokal gewinnt": Alle Ressourcen hier sind Defaults. Definiert eine
// App eine Ressource gleichen Namens (Farbe, String, Style), überstimmt die
// App-Version diese Bibliothek.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.castigaro.common"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
