package com.morainet.widget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherCompatibilityTest {

    @Test
    fun profile_sizeForGrid_2x2() {
        val (w, h) = LauncherProfile.PIXEL.sizeForGrid(2, 2)
        assertEquals(180 - 16, w) // 2*90 - 2*8 = 164
        assertEquals(180 - 16, h)
    }

    @Test
    fun profile_sizeForGrid_4x2() {
        val (w, h) = LauncherProfile.PIXEL.sizeForGrid(4, 2)
        assertEquals(360 - 16, w) // 4*90 - 2*8 = 344
        assertEquals(180 - 16, h)
    }

    @Test
    fun check_pixel_noIssuesForStandardSizes() {
        val report = LauncherCompatibilityChecker.check(
            designWidthDp = 164,
            designHeightDp = 164,
            profile = LauncherProfile.PIXEL,
        )
        assertFalse(report.hasIssues)
    }

    @Test
    fun check_oversizedWidget_reportsSizeIssue() {
        val report = LauncherCompatibilityChecker.check(
            designWidthDp = 200,
            designHeightDp = 200,
            profile = LauncherProfile.PIXEL,
        )
        assertTrue(report.hasIssues)
        assertTrue(report.widgetSizeIssues.isNotEmpty())
    }

    @Test
    fun check_samsung_reportsRefreshLimit() {
        val report = LauncherCompatibilityChecker.check(
            designWidthDp = 164,
            designHeightDp = 164,
            profile = LauncherProfile.SAMSUNG,
        )
        assertTrue(report.refreshIssues.any {
            it.contains("30")
        })
    }

    @Test
    fun check_xiaomi_reportsBackgroundRestriction() {
        val report = LauncherCompatibilityChecker.check(
            designWidthDp = 164,
            designHeightDp = 164,
            profile = LauncherProfile.XIAOMI,
        )
        assertTrue(report.refreshIssues.any {
            it.contains("后台") || it.contains("电池")
        })
    }

    @Test
    fun checkAll_returnsFiveReports() {
        val reports = LauncherCompatibilityChecker.checkAll(
            designWidthDp = 164,
            designHeightDp = 164,
        )
        assertEquals(5, reports.size)
    }

    @Test
    fun profile_allProfiles_haveDistinctNames() {
        val names = LauncherProfile.ALL.map { it.manufacturer }
        assertEquals(names.size, names.distinct().size)
    }
}
