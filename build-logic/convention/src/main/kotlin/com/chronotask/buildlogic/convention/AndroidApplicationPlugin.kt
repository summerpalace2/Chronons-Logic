package com.chronotask.buildlogic.convention

import com.chronotask.buildlogic.convention.config.androidCommonConvention
import com.chronotask.buildlogic.convention.config.getBundle
import com.chronotask.buildlogic.convention.config.getLib
import com.chronotask.buildlogic.convention.config.getIntVersion
import com.chronotask.buildlogic.convention.config.versionLibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("com.google.devtools.ksp")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
                androidCommonConvention(this)
                defaultConfig {
                    applicationId = versionLibs.findVersion("applicationId").get().requiredVersion
                    targetSdk = getIntVersion("targetSdk")
                    versionCode = getIntVersion("versionCode")
                    versionName = versionLibs.findVersion("versionName").get().requiredVersion
                }
                buildFeatures {
                    buildConfig = true
                }
                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                }
            }

            dependencies {
                add("implementation", project(":chronotask-components:navigation:core"))
                add("ksp", project(":chronotask-components:navigation:processor"))
                val composeBom = platform(getLib("androidx-compose-bom"))
                add("implementation", composeBom)
                add("androidTestImplementation", composeBom)
                add("implementation", getBundle("projectBaseCompose"))
            }
        }
    }
}
