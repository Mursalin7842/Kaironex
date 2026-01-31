import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.1.0"
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val geminiKey = localProperties.getProperty("GeminiAPI") ?: ""

kotlin {
    jvmToolchain(17)
    androidTarget {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += "-Xexpect-actual-classes"
            }
        }
    }
    
    jvm {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += "-Xexpect-actual-classes"
            }
        }
    }
    
    sourceSets {

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.preview)
            implementation(compose.materialIconsExtended)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Firebase (Keep existing)
            implementation(libs.firebase.database)
            implementation(libs.firebase.common)

            // Ktor (The Brain)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.websockets)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            // Koin (DI)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.koin.core)

            // Voyager (Navigation)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

//            // WebView
//            implementation(libs.compose.webview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.firebase.database.ktx)
            implementation(libs.ktor.android)
            implementation(libs.koin.android)
//            implementation(libs.sqldelight.android)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation(libs.jna)
            implementation(libs.jna.platform)
            implementation(libs.firebase.admin)
            implementation(libs.ktor.okhttp)
//            implementation(libs.kcef)
            
            // Explicitly added Ktor plugins for JVM runtime (Using direct JVM artifacts to fix NoClassDefFoundError)
            implementation("io.ktor:ktor-client-content-negotiation-jvm:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")

////            implementation(libs.sqldelight.sqlite)

        }
    }
}



android {
    namespace = "com.mursaline.kaironex"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mursaline.kaironex"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
//    packaging {
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }
//        jniLibs {
//            useLegacyPackaging = true
//            pickFirsts += "**/libandroidx.graphics.path.so"
//        }
//    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.mursaline.kaironex.MainKt"
        jvmArgs += listOf(
            "-DGEMINI_API_KEY=$geminiKey",
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.mursaline.kaironex"
            packageVersion = "1.0.0"
        }
    }
}

//sqldelight {
//    databases {
//        create("KaironexDatabase") {
//            packageName.set("com.mursaline.kaironex.db")
//        }
//    }
//}
