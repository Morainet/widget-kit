package com.morainet.widget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WidgetActionsTest {

    @Test
    fun actionRefresh_constant() {
        assertEquals("com.morainet.widget.action.REFRESH", WidgetActions.ACTION_REFRESH)
    }

    @Test
    fun actionRetry_constant() {
        assertEquals("com.morainet.widget.action.RETRY", WidgetActions.ACTION_RETRY)
    }

    @Test
    fun extraWidgetId_constant() {
        assertEquals("com.morainet.widget.extra.WIDGET_ID", WidgetActions.EXTRA_WIDGET_ID)
    }

    @Test
    fun constants_are_nonEmpty() {
        assertNotNull(WidgetActions.ACTION_REFRESH)
        assertNotNull(WidgetActions.ACTION_RETRY)
        assertNotNull(WidgetActions.EXTRA_WIDGET_ID)
    }
}
