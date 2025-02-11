import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    // Initialize an instance of XCFramework to bundle and manage iOS frameworks
    val xcf = XCFramework()

    // Define the absolute paths to the framework files for both arm64 (device) and simulator architectures
    val frameworkPath = project.file("$rootDir/iosApp/DynamsoftBarcodeReader.xcframework").absolutePath
    val frameworkPathArm64 = "$frameworkPath/ios-arm64/"
    val frameworkPathSimulator = "$frameworkPath/ios-arm64_x86_64-simulator/"

    // Function to configure interop for a specified Kotlin Native target
    fun configureInterop(target: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget, frameworkPath: String) {
        target.compilations.getByName("main") {
            val dynamsoftBarcodeReader by cinterops.creating {
                defFile("$rootDir/iosApp/DynamsoftBarcodeReader.def")
                compilerOpts("-framework", "DynamsoftBarcodeReader", "-F$frameworkPath")
                extraOpts += listOf("-compiler-option", "-fmodules")
            }
        }
        target.binaries.all {
            linkerOpts("-framework", "DynamsoftBarcodeReader", "-F$frameworkPath")
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        val currentFrameworkPath = if (iosTarget.name.contains("arm64")) frameworkPathArm64 else frameworkPathSimulator
        configureInterop(iosTarget, currentFrameworkPath)
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            xcf.add(this)
        }
    }
    
    sourceSets {

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.accompanist.permissions)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.dynamsoft.barcode.reader)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.material3.android)
    debugImplementation(compose.uiTooling)
}

