package com.chronotask.buildlogic.convention

import com.chronotask.buildlogic.convention.config.androidCommonConvention
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {
                androidCommonConvention(this)
            }
        }
    }
}
