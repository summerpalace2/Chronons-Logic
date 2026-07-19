package com.chronotask.buildlogic.convention.dsl

import org.gradle.api.plugins.PluginManager

fun PluginManager.id(alias: String) {
    apply(alias)
}
