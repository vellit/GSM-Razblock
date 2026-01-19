import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

buildDir = File(System.getProperty("java.io.tmpdir"), "gsm-build")

val keystoreProperties = Properties().apply {
    val keystoreFile = listOf(
        rootProject.file("cert/keystore.properties"),
        rootProject.file("keystore.properties")
    ).firstOrNull { it.exists() }
    keystoreFile?.inputStream()?.use { load(it) }
}

android {
    namespace = "ru.vellit.gsm2g"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.vellit.gsm2g"
        minSdk = 30
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
        setProperty("archivesBaseName", "2GRazblock")
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String? ?: ""
            keyPassword = keystoreProperties["keyPassword"] as String? ?: ""
            storeFile = keystoreProperties["storeFile"]?.let { rootProject.file(it) }
            storePassword = keystoreProperties["storePassword"] as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
