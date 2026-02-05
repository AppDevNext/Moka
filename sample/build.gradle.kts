plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "com.sample.app"

    defaultConfig {
        minSdk = 21
        compileSdk = 36
        targetSdk { version = release(36) }

        applicationId = "com.sample.app"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "useTestStorageService" to "true",
            ),
        )
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro"),
                ),
            )
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        managedDevices {
            localDevices {
                // run with ../gradlew nexusOneApi30DebugAndroidTest
                create("nexusOneApi30") {
                    // A lower resolution device is used here for better emulator performance
                    device = "Nexus One"
                    apiLevel = 30
                    // Also use the AOSP ATD image for better emulator performance
                    // The androidx.test screenshot APIs will automatically enable hardware rendering
                    // to take a screenshot
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.3.10")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.multidex:multidex:2.0.1")

    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("com.github.AppDevNext.Logcat:LogcatCoreLib:3.4")
    androidTestUtil("androidx.test.services:test-services:1.6.0")

    androidTestImplementation(project (":moka"))
}
