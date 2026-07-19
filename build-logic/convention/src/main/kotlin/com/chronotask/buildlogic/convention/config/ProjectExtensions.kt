package com.chronotask.buildlogic.convention.config

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.versionLibs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.getLib(alias: String) = versionLibs.findLibrary(alias).get()
fun Project.getBundle(alias: String) = versionLibs.findBundle(alias).get()
fun Project.getIntVersion(alias: String) = versionLibs.findVersion(alias).get().requiredVersion.toInt()
