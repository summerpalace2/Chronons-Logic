package com.chronotask.buildlogic.convention

import com.chronotask.buildlogic.convention.config.getBundle
import com.chronotask.buildlogic.convention.config.getLib
import com.chronotask.buildlogic.convention.dsl.implementation
import com.chronotask.buildlogic.convention.dsl.kspLib
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

fun Project.useCompose(includeBase: Boolean = path != ":chronotask-components:ui") {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extensions.configure<com.android.build.api.dsl.CommonExtension<*, *, *, *, *, *>>("android") {
        buildFeatures { compose = true }
    }
    dependencies {
        add("implementation", platform(getLib("androidx-compose-bom")))
        add("implementation", getLib("androidx-compose-ui"))
        add("implementation", getLib("androidx-compose-ui-graphics"))
        add("implementation", getLib("androidx-compose-ui-tooling-preview"))
        add("implementation", getLib("androidx-compose-material3"))
        add("implementation", getLib("androidx-activity-compose"))
        add("implementation", getLib("androidx-compose-material-icons-extended"))
        add("implementation", getLib("androidx-compose-foundation"))
        add("implementation", getLib("androidx-animation-core"))
        add("implementation", getLib("androidx-navigation3-runtime"))
        add("implementation", getLib("androidx-navigation3-ui"))
        add("implementation", getLib("androidx-lifecycle-viewmodel-navigation3"))
        add("implementation", getLib("savedstate-compose"))
        if (includeBase) {
            add("implementation", project(":chronotask-components:ui"))
        }
    }
}

fun Project.useNav(includeBase: Boolean = path != ":chronotask-components:navigation:core") {
    pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    pluginManager.apply("com.google.devtools.ksp")
    implementation("auto-service-annotations")
    kspLib("auto-service-ksp")
    if (includeBase) {
        dependencies.add("implementation", project(":chronotask-components:navigation:core"))
        dependencies.add("ksp", project(":chronotask-components:navigation:processor"))
    }
}

fun Project.useDB(includeBase: Boolean = path != ":chronotask-components:database") {
    pluginManager.apply("com.google.devtools.ksp")
    implementation("androidx-room-runtime")
    implementation("androidx-room-ktx")
    kspLib("androidx-room-compiler")
    if (includeBase) {
        dependencies.add("implementation", project(":chronotask-components:database"))
    }
}
