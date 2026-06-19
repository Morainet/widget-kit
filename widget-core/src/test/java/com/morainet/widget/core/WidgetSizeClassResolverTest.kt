package com.morainet.widget.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizeClassResolverTest {

    @Test
    fun resolve_small() {
        assertEquals(WidgetSizeClass.SMALL, WidgetSizeClassResolver.resolve(180, 55))
    }

    @Test
    fun resolve_medium() {
        assertEquals(WidgetSizeClass.MEDIUM, WidgetSizeClassResolver.resolve(180, 110))
    }

    @Test
    fun resolve_large() {
        assertEquals(WidgetSizeClass.LARGE, WidgetSizeClassResolver.resolve(360, 110))
    }
}
