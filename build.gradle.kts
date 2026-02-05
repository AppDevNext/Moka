import org.gradle.internal.jvm.Jvm

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    dependencies {
        classpath("com.google.gms:google-services:4.4.4")
        classpath("com.android.tools.build:gradle:9.0.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    }
}

println("Gradle uses Java ${Jvm.current()}")

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
