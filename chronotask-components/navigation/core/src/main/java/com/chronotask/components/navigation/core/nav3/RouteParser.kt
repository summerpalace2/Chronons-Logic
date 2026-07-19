package com.chronotask.components.navigation.core.nav3

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object RouteParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class CompiledRoute(
        val regex: Regex,
        val paramNames: List<String>,
        val collector: AppNavCollector<*>
    )

    private val compiledRoutes: List<CompiledRoute> by lazy {
        ensureCollectorsInitialized()
        appNavCollectors.values.map { collector ->
            val template = collector.route
            val placeholderRegex = "\\{([^}]+)\\}".toRegex()
            val paramNames = placeholderRegex.findAll(template).map { it.groupValues[1] }.toList()
            val regexPattern = "^" + template.replace(placeholderRegex, "([^/]+)") + "$"
            CompiledRoute(regex = regexPattern.toRegex(), paramNames = paramNames, collector = collector)
        }
    }

    fun parse(actualRoute: String): AppNavArgument? {
        for (compiled in compiledRoutes) {
            val matchResult = compiled.regex.find(actualRoute)
            if (matchResult != null) {
                val jsonMap = mutableMapOf<String, JsonPrimitive>()
                compiled.paramNames.forEachIndexed { index, paramName ->
                    jsonMap[paramName] = JsonPrimitive(matchResult.groupValues[index + 1])
                }
                val jsonObject = JsonObject(jsonMap)
                return try {
                    json.decodeFromJsonElement(
                        deserializer = compiled.collector.argumentSerializer,
                        element = jsonObject
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }
}
