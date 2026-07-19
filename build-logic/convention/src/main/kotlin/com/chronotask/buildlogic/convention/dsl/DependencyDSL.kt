package com.chronotask.buildlogic.convention.dsl

import com.chronotask.buildlogic.convention.config.getLib
import org.gradle.api.Project

fun Project.implementation(lib: String) =
    dependencies.add("implementation", getLib(lib))

fun Project.kspLib(lib: String) =
    dependencies.add("ksp", getLib(lib))

fun Project.kspProject(projectPath: String) {
    dependencies.add("ksp", project(projectPath))
}

fun Project.testImplementation(lib: String) =
    dependencies.add("testImplementation", getLib(lib))

fun Project.androidTestImplementation(lib: String) =
    dependencies.add("androidTestImplementation", getLib(lib))

fun Project.debugImplementation(lib: String) =
    dependencies.add("debugImplementation", getLib(lib))

