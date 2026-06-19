package com.morainet.widget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizeClassResolverTest {

    @Test
    fun resolve_small_2x1() {
        assertEquals(WidgetSizeClass.SMALL, WidgetSizeClassResolver.resolve(180, 55))
    }

    @Test
    fun resolve_small_tiny() {
        assertEquals(WidgetSizeClass.SMALL, WidgetSizeClassResolver.resolve(90, 50))
    }

    @Test
    fun resolve_medium_2x2() {
        assertEquals(WidgetSizeClass.MEDIUM, WidgetSizeClassResolver.resolve(180, 110))
    }

    @Test
    fun resolve_medium_boundary() {
        // 180*110 = 19800, 刚好是 SMALL/MEDIUM 边界
        assertEquals(WidgetSizeClass.MEDIUM, WidgetSizeClassResolver.resolve(180, 110))
        // 180*56 = 10080 > 180*55=9900，所以是 MEDIUM
        assertEquals(WidgetSizeClass.MEDIUM, WidgetSizeClassResolver.resolve(180, 56))
    }

    @Test
    fun resolve_large_4x2() {
        assertEquals(WidgetSizeClass.LARGE, WidgetSizeClassResolver.resolve(360, 110))
    }

    @Test
    fun resolve_large_4x4() {
        assertEquals(WidgetSizeClass.LARGE, WidgetSizeClassResolver.resolve(360, 220))
    }
}
