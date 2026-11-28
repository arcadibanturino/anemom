plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kelompok.anemom"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kelompok.anemom"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Alamat API backend (harus diakhiri "/"). Bisa ditimpa tanpa mengubah file ini, lewat
        // ~/.gradle/gradle.properties atau gradle.properties lokal:  ANEMOM_BASE_URL=https://.../api/v1/
        val baseUrl = (project.findProperty("ANEMOM_BASE_URL") as String?)
            ?: "https://anemom.sovereigns.my.id/api/v1/"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Model segmentasi konjungtiva: satu kode, dua varian (lihat README-PEMISAHAN.md).
    //   unetDebug / unetRelease : U-Net (dipakai sekarang)
    //   yoloDebug / yoloRelease : YOLOv8 (eksperimen perbandingan; bisa terpasang berdampingan)
    flavorDimensions += "segmenter"
    productFlavors {
        create("unet") {
            dimension = "segmenter"
        }
        create("yolo") {
            dimension = "segmenter"
            applicationIdSuffix = ".yolo"
            versionNameSuffix = "-yolo"
        }
    }

    // Model .tflite di-map langsung dari APK, jadi tidak boleh dikompres.
    androidResources {
        noCompress += "tflite"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core + Compose + Material 3
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // MVVM (ViewModel + lifecycle) dan navigasi
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Local storage (sesi login lokal)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking: Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines + Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Memuat gambar dari server (foto bahan makanan)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Kamera untuk AI Scan Mata
    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Menjalankan model .tflite (U-Net) di HP. LiteRT = nama baru TensorFlow Lite (paket org.tensorflow.lite).
    implementation("com.google.ai.edge.litert:litert:1.4.2")

    // Petunjuk live AI Scan Mata: detektor wajah + kontur mata (model bawaan di APK, jalan offline).
    implementation("com.google.mlkit:face-detection:16.1.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
