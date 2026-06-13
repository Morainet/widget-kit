package net.morainet.widget.dsl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.yaml.snakeyaml.Yaml

/**
 * 解析 YAML/JSON 格式的 Widget Blueprint。
 */
object WidgetBlueprintParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseJson(source: String): WidgetBlueprint {
        return json.decodeFromString<WidgetBlueprint>(source)
    }

    fun parseYaml(source: String): WidgetBlueprint {
        val yaml = Yaml()
        @Suppress("UNCHECKED_CAST")
        val map = yaml.load<Map<String, Any?>>(source)
        val element = mapToJsonElement(map)
        return json.decodeFromJsonElement(WidgetBlueprint.serializer(), element)
    }

    fun toJson(blueprint: WidgetBlueprint): String {
        return json.encodeToString(blueprint)
    }

    private fun mapToJsonElement(map: Map<String, Any?>): JsonElement {
        return JsonObject(
            map.mapValues { (_, value) -> anyToJsonElement(value) },
        )
    }

    private fun listToJsonElement(list: List<*>): JsonElement {
        return JsonArray(list.map { anyToJsonElement(it) })
    }

    private fun anyToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                mapToJsonElement(value as Map<String, Any?>)
            }
            is List<*> -> listToJsonElement(value)
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
    }
}
