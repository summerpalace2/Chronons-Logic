plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.devtools.ksp.processing)
    implementation(libs.devtools.kotlinpoet)
    implementation(libs.devtools.kotlinpoet.ksp)
    implementation(libs.auto.service.annotations)
    implementation(libs.kotlinx.serialization.json)
}