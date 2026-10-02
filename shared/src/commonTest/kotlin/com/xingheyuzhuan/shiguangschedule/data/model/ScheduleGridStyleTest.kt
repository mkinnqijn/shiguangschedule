package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleGridStyleTest {

    @Test
    fun defaultColorMapsKeepOriginalIndexesAndAppendEightColors() {
        val originalColors = listOf(
            DualColor(Color(0xFFFFCC99), Color(0xFF663300)),
            DualColor(Color(0xFFFFE699), Color(0xFF664D00)),
            DualColor(Color(0xFFE6FF99), Color(0xFF4D6600)),
            DualColor(Color(0xFFCCFF99), Color(0xFF336600)),
            DualColor(Color(0xFF99FFB3), Color(0xFF00661A)),
            DualColor(Color(0xFF99FFE6), Color(0xFF00664D)),
            DualColor(Color(0xFF99FFFF), Color(0xFF006666)),
            DualColor(Color(0xFF99E6FF), Color(0xFF004D66)),
            DualColor(Color(0xFFB399FF), Color(0xFF1A0066)),
            DualColor(Color(0xFFFF99E6), Color(0xFF66004D)),
            DualColor(Color(0xFFFF99CC), Color(0xFF660033)),
            DualColor(Color(0xFFFF99B3), Color(0xFF66001A)),
        )

        assertEquals(20, ScheduleGridStyle.DEFAULT_COLOR_MAPS.size)
        assertEquals(originalColors, ScheduleGridStyle.DEFAULT_COLOR_MAPS.take(12))
    }

    @Test
    fun storedTwelveColorPaletteKeepsItsValuesAndGetsNewDefaults() {
        val customizedFirstColor = DualColor(Color(0xFFABCDEF), Color(0xFF123456))
        val storedColors = ScheduleGridStyle.DEFAULT_COLOR_MAPS.take(12).toMutableList().apply {
            this[0] = customizedFirstColor
        }

        val resolvedColors = resolveCourseColorMaps(storedColors)

        assertEquals(20, resolvedColors.size)
        assertEquals(storedColors, resolvedColors.take(12))
        assertEquals(
            ScheduleGridStyle.DEFAULT_COLOR_MAPS.drop(12),
            resolvedColors.drop(12)
        )
    }
}
