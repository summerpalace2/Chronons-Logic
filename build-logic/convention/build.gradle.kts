plugins {
    `kotlin-dsl`
}
group = "com.chronotask.buildlogic"

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
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("chronotaskApplication") {
            id = "chronotask.android.application"
            implementationClass = "com.chronotask.buildlogic.convention.AndroidApplicationPlugin"
        }
        register("chronotaskLibrary") {
            id = "chronotask.android.library"
            implementationClass = "com.chronotask.buildlogic.convention.AndroidLibraryPlugin"
        }
    }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
