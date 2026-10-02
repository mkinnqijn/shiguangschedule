package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleGridStyleTest {

    @Test
    fun defaultColorMapsKeepOriginalTwentyIndexesAndAppendFourColors() {
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
            DualColor(Color(0xFFA7C8F2), Color(0xFF2D4F73)),
            DualColor(Color(0xFFB9D8F2), Color(0xFF325D73)),
            DualColor(Color(0xFF9FD5D0), Color(0xFF2D6461)),
            DualColor(Color(0xFFB7D5B2), Color(0xFF446343)),
            DualColor(Color(0xFFC8CDD4), Color(0xFF4D535C)),
            DualColor(Color(0xFFEFAAA5), Color(0xFF7A3F3B)),
            DualColor(Color(0xFFEDB574), Color(0xFF74501F)),
            DualColor(Color(0xFFC9B5DF), Color(0xFF5D4773)),
        )

        assertEquals(24, ScheduleGridStyle.DEFAULT_COLOR_MAPS.size)
        assertEquals(originalColors, ScheduleGridStyle.DEFAULT_COLOR_MAPS.take(20))
    }

    @Test
    fun storedTwelveColorPaletteKeepsItsValuesAndGetsNewDefaults() {
        val customizedFirstColor = DualColor(Color(0xFFABCDEF), Color(0xFF123456))
        val storedColors = ScheduleGridStyle.DEFAULT_COLOR_MAPS.take(12).toMutableList().apply {
            this[0] = customizedFirstColor
        }

        val resolvedColors = resolveCourseColorMaps(storedColors)

        assertEquals(24, resolvedColors.size)
        assertEquals(storedColors, resolvedColors.take(12))
        assertEquals(
            ScheduleGridStyle.DEFAULT_COLOR_MAPS.drop(12),
            resolvedColors.drop(12)
        )
    }

    @Test
    fun storedTwentyColorPaletteKeepsItsValuesAndGetsFourNewDefaults() {
        val customizedColor = DualColor(Color(0xFFABCDEF), Color(0xFF123456))
        val storedColors = ScheduleGridStyle.DEFAULT_COLOR_MAPS.take(20).toMutableList().apply {
            this[19] = customizedColor
        }

        val resolvedColors = resolveCourseColorMaps(storedColors)

        assertEquals(24, resolvedColors.size)
        assertEquals(storedColors, resolvedColors.take(20))
        assertEquals(ScheduleGridStyle.DEFAULT_COLOR_MAPS.drop(20), resolvedColors.drop(20))
    }

    @Test
    fun displayOrderContainsEveryStableIndexExactlyOnce() {
        assertEquals(
            listOf(
                9, 10, 11, 20, 17, 0,
                18, 21, 1, 2, 3, 15,
                4, 5, 22, 14, 6, 7,
                13, 12, 23, 8, 19, 16
            ),
            courseColorDisplayOrder(24)
        )
        assertEquals((0 until 24).toList(), courseColorDisplayOrder(24).sorted())
    }

    @Test
    fun everyColorIndexCanBeUpdatedAndPersisted() {
        val markerLight = Color(0xFFABCDEF)
        val markerDark = Color(0xFF123456)

        for (index in 0 until 24) {
            val withLightUpdate = updateCourseColorMap(
                ScheduleGridStyle.DEFAULT_COLOR_MAPS,
                index,
                markerLight,
                isDark = false
            )
            val withBothUpdates = updateCourseColorMap(
                withLightUpdate,
                index,
                markerDark,
                isDark = true
            )
            val proto = ScheduleGridStyle(courseColorMaps = withBothUpdates).toProto()
            val persistedBytes = ScheduleGridStyleProto.ADAPTER.encode(proto)
            val restored = ScheduleGridStyleProto.ADAPTER.decode(persistedBytes).toCompose()

            assertEquals(markerLight.toArgb(), restored.courseColorMaps[index].light.toArgb())
            assertEquals(markerDark.toArgb(), restored.courseColorMaps[index].dark.toArgb())
            assertEquals(24, restored.courseColorMaps.size)
        }
    }
}
