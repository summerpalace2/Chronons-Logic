package com.chronotask.buildlogic.convention.config

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

fun Project.androidCommonConvention(extension: CommonExtension<*, *, *, *, *, *>) {
    val namespace = path
        .removePrefix(":")
        .replaceFirst("chronotask-", "")
        .replace("-", ".")
        .replace(":", ".")
        .let { "com.chronotask.$it" }

    extension.apply {
        this.namespace = namespace
        compileSdk = getIntVersion("compileSdk")
        defaultConfig {
            minSdk = getIntVersion("minSdk")
        }

        compileOptions {
            sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
            targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                excludes += "/META-INF/gradle/incremental.annotation.processors"
                excludes += "**/attach_hotspot_windows.dll"
                excludes += "META-INF/licenses/**"
            }
        }
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>("kotlin") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    dependencies {
        add("implementation", getBundle("projectBase"))
        add("testImplementation", getBundle("projectBaseTest"))
        if (path != ":chronotask-components:common") {
            add("implementation", project(":chronotask-components:common"))
        }
    }
}
