package com.morainet.widget.dsl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    @Test
    fun parseYaml_withTheme() {
        val yaml = """
            meta:
              name: ThemedWidget
            layout: COUNTER_2X2
            components: []
            theme:
              style: material_you
              primaryColor: "#FF0000"
              useDynamicColor: true
        """.trimIndent()

        val blueprint = WidgetBlueprintParser.parseYaml(yaml)
        assertNotNull(blueprint.theme)
        assertEquals("material_you", blueprint.theme!!.style)
        assertEquals("#FF0000", blueprint.theme!!.primaryColor)
        assertEquals(true, blueprint.theme!!.useDynamicColor)
    }

    @Test
    fun parseYaml_withAnimations() {
        val yaml = """
            meta:
              name: AnimatedWidget
            layout: LIST_4X2
            components:
              - type: TEXT
                id: title
                props:
                  text: "Hello"
            animations:
              - preset: FADE
                targetId: title
                durationMs: 500
        """.trimIndent()

        val blueprint = WidgetBlueprintParser.parseYaml(yaml)
        assertEquals(1, blueprint.animations.size)
        assertEquals("FADE", blueprint.animations.first().preset)
        assertEquals("title", blueprint.animations.first().targetId)
        assertEquals(500, blueprint.animations.first().durationMs)
    }

    @Test
    fun parseYaml_withProgressAndChart() {
        val yaml = """
            meta:
              name: DataWidget
            layout: SINGLE_ENTITY_2X2
            components:
              - type: PROGRESS
                id: cpu
                props:
                  value: "75"
                  max: "100"
              - type: CHART
                id: weekly
                props:
                  values: "10,25,18,42,30,55,20"
        """.trimIndent()

        val blueprint = WidgetBlueprintParser.parseYaml(yaml)
        assertEquals(2, blueprint.components.size)
        assertEquals(ComponentType.PROGRESS, blueprint.components[0].type)
        assertEquals("75", blueprint.components[0].props["value"])
        assertEquals(ComponentType.CHART, blueprint.components[1].type)
        assertEquals("10,25,18,42,30,55,20", blueprint.components[1].props["values"])
    }
}
