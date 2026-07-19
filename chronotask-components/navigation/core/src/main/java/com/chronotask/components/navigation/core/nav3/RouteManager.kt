package com.chronotask.components.navigation.core.nav3

import java.util.ServiceLoader

internal val appNavCollectors: Map<String, AppNavCollector<*>> by lazy { loadCollectors() }

fun ensureCollectorsInitialized() {
    appNavCollectors.size
}

private fun loadCollectors(): Map<String, AppNavCollector<*>> {
    val result = mutableMapOf<String, AppNavCollector<*>>()
    val loader = ServiceLoader.load(AppNavCollector::class.java, AppNavCollector::class.java.classLoader)
    for (collector in loader) {
        result[collector.route] = collector
    }
    return result
}
