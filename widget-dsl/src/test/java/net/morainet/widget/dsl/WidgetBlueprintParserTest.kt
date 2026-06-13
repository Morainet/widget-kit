package net.morainet.widget.dsl

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetBlueprintParserTest {

    @Test
    fun parseYaml_weatherWidget() {
        val yaml = """
            meta:
              name: WeatherWidget
              description: test
            layout: SINGLE_ENTITY_2X2
            components:
              - type: TEXT
                id: city
                props:
                  text: "Shanghai"
        """.trimIndent()

        val blueprint = WidgetBlueprintParser.parseYaml(yaml)

        assertEquals("WeatherWidget", blueprint.meta.name)
        assertEquals(WidgetLayout.SINGLE_ENTITY_2X2, blueprint.layout)
        assertEquals(1, blueprint.components.size)
        assertEquals("city", blueprint.components.first().id)
        assertEquals("Shanghai", blueprint.components.first().props["text"])
    }

    @Test
    fun parseJson_roundTrip() {
        val json = """
            {
              "meta": { "name": "CounterWidget" },
              "layout": "COUNTER_2X2",
              "components": []
            }
        """.trimIndent()

        val blueprint = WidgetBlueprintParser.parseJson(json)
        val encoded = WidgetBlueprintParser.toJson(blueprint)
        val restored = WidgetBlueprintParser.parseJson(encoded)

        assertEquals(blueprint, restored)
    }
}
