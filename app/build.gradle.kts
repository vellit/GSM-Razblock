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
        versionCode = 3
        versionName = "1.1.1"
        setProperty("archivesBaseName", "2GRazblock")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    testOptions {
        managedDevices {
            devices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel4Api30").apply {
                    device = "Pixel 4"
                    apiLevel = 30
                    systemImageSource = "google_apis"
                }
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6Api33").apply {
                    device = "Pixel 6"
                    apiLevel = 33
                    systemImageSource = "google_apis"
                }
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel3aApi31").apply {
                    device = "Pixel 3a"
                    apiLevel = 31
                    systemImageSource = "google_apis"
                }
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel7Api34").apply {
                    device = "Pixel 7"
                    apiLevel = 34
                    systemImageSource = "google_apis"
                }
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel7Api35").apply {
                    device = "Pixel 7"
                    apiLevel = 35
                    systemImageSource = "google_apis"
                }
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel7Api36").apply {
                    device = "Pixel 7"
                    apiLevel = 36
                    systemImageSource = "google_apis"
                }
            }
            groups {
                maybeCreate("ciGroup").apply {
                    targetDevices.addAll(
                        listOf(
                            devices["pixel4Api30"],
                            devices["pixel3aApi31"],
                            devices["pixel6Api33"],
                            devices["pixel7Api34"],
                            devices["pixel7Api35"],
                            devices["pixel7Api36"]
                        )
                    )
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
