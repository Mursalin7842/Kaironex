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
val appwriteEndpoint = localProperties.getProperty("AppwriteEndpoint") ?: "https://nyc.cloud.appwrite.io/v1"
val appwriteProject = localProperties.getProperty("AppwriteProject") ?: ""
val appwriteDatabase = localProperties.getProperty("AppwriteDatabase") ?: ""
val appwriteFunctionId = localProperties.getProperty("AppwriteFunctionId") ?: ""
val appwriteApiKey = localProperties.getProperty("AppwriteApiKey") ?: ""

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
            implementation("io.ktor:ktor-client-core:3.0.0")
            implementation("io.ktor:ktor-client-cio:3.0.0")
            implementation("io.ktor:ktor-client-websockets:3.0.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            
            // Koin (DI)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.koin.core)

            // Voyager (Navigation)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            // FileKit (Picker)
            implementation(libs.filekit.compose)

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
            implementation("androidx.webkit:webkit:1.10.0")
//            implementation(libs.sqldelight.android)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation(libs.jna)
            implementation(libs.jna.platform)
            implementation(libs.firebase.admin)
            implementation(libs.ktor.okhttp)

            // Ktor dependencies for JVM (explicit to fix NoClassDefFoundError)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // SLF4J implementation (fixes SLF4J warning)
            implementation("org.slf4j:slf4j-simple:2.0.9")

            // KCEF for WebView
            implementation(libs.kcef)

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
        buildConfigField("String", "APPWRITE_ENDPOINT", "\"$appwriteEndpoint\"")
        buildConfigField("String", "APPWRITE_PROJECT", "\"$appwriteProject\"")
        buildConfigField("String", "APPWRITE_DATABASE", "\"$appwriteDatabase\"")
        buildConfigField("String", "APPWRITE_FUNCTION_ID", "\"$appwriteFunctionId\"")
        buildConfigField("String", "APPWRITE_API_KEY", "\"$appwriteApiKey\"")
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
            "-DAPPWRITE_ENDPOINT=$appwriteEndpoint",
            "-DAPPWRITE_PROJECT=$appwriteProject",
            "-DAPPWRITE_DATABASE=$appwriteDatabase",
            "-DAPPWRITE_FUNCTION_ID=$appwriteFunctionId",
            "-DAPPWRITE_API_KEY=$appwriteApiKey",
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
