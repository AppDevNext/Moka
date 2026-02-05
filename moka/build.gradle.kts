plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

android {
    namespace = "com.moka"

    defaultConfig {
        minSdk = 21
        compileSdk = 36
        targetSdkVersion(36)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "useTestStorageService" to "true",
            ),
        )
    }
    publishing {
        singleVariant("release") {}
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
    api("androidx.test.espresso:espresso-contrib:3.7.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.10")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // waiter
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("junit:junit:4.13.2")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
                pom {
                    licenses {
                        license {
                            name = "Apache License Version 2.0"
                            url = "https://github.com/AppDevNext/Moka/blob/master/LICENSE"
                        }
                    }
                }
            }
        }
    }
}

