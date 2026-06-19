package com.morainet.widget.workmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRefreshLoggerTest {

    @Test
    fun logScheduled_appendsEntry() {
        WidgetRefreshLogger.clear()
        WidgetRefreshLogger.logScheduled("weather_widget_refresh", "periodic", 15)

        val history = WidgetRefreshLogger.getHistory()
        assertEquals(1, history.size)
        val entry = history.first()
        assertEquals("weather_widget_refresh", entry.workName)
        assertEquals("periodic", entry.trigger)
        assertEquals("scheduled", entry.status)
        assertEquals(15, entry.intervalMinutes)
    }

    @Test
    fun logStart_appendsEntry() {
        WidgetRefreshLogger.clear()
        WidgetRefreshLogger.logStart("WeatherRefreshWorker", "periodic")

        val history = WidgetRefreshLogger.getHistory()
        assertEquals(1, history.size)
        val entry = history.first()
        assertEquals("WeatherRefreshWorker", entry.workName)
        assertEquals("started", entry.status)
    }

    @Test
    fun logSuccess_appendsEntry() {
        WidgetRefreshLogger.clear()
        WidgetRefreshLogger.logSuccess("WeatherRefreshWorker")

        val history = WidgetRefreshLogger.getHistory()
        assertEquals(1, history.size)
        assertEquals("success", history.first().status)
    }

    @Test
    fun logFailure_appendsEntryWithMessage() {
        WidgetRefreshLogger.clear()
        WidgetRefreshLogger.logFailure("WeatherRefreshWorker", RuntimeException("timeout"))

        val history = WidgetRefreshLogger.getHistory()
        assertEquals(1, history.size)
        val entry = history.first()
        assertEquals("failure", entry.status)
        assertEquals("timeout", entry.message)
    }

    @Test
    fun logCancelled_appendsEntry() {
        WidgetRefreshLogger.clear()
        WidgetRefreshLogger.logCancelled("weather_widget_refresh")

        val history = WidgetRefreshLogger.getHistory()
        assertEquals(1, history.size)
        assertEquals("cancelled", history.first().status)
    }

    @Test
    fun history_cappedAt100() {
        WidgetRefreshLogger.clear()
        repeat(150) { i ->
            WidgetRefreshLogger.logStart("Worker$i", "test")
        }

        val history = WidgetRefreshLogger.getHistory()
        assertTrue(history.size <= 100)
    }

    @Test
    fun clear_removesAllEntries() {
        WidgetRefreshLogger.logStart("TestWorker", "test")
        WidgetRefreshLogger.clear()

        val history = WidgetRefreshLogger.getHistory()
        assertTrue(history.isEmpty())
    }
}
