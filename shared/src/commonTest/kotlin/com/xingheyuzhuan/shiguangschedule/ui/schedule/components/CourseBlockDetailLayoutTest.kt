package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourseBlockDetailLayoutTest {

    private val locationSingleLineWidth = 52f
    private val contentHeight = 130f
    private val heightBeforeDetails = 31.2f
    private val detailLineHeight = 10f

    @Test
    fun normalCourseKeepsItsExactColor() {
        val normalColor = androidx.compose.ui.graphics.Color(0.8f, 0.4f, 0.2f)
        val backgroundColor = androidx.compose.ui.graphics.Color(1f, 1f, 1f)

        assertEquals(
            normalColor,
            resolveCourseBlockBaseColor(normalColor, backgroundColor, false)
        )
    }

    @Test
    fun lightPreviewColorMixesCourseAndCurrentBackground() {
        val previewColor = resolveCourseBlockBaseColor(
            normalCourseColor = androidx.compose.ui.graphics.Color(0.8f, 0.4f, 0.2f),
            scheduleBackgroundColor = androidx.compose.ui.graphics.Color(1f, 1f, 1f),
            showNextWeekPreview = true
        )

        assertEquals(0.88f, previewColor.red, 0.002f)
        assertEquals(0.64f, previewColor.green, 0.002f)
        assertEquals(0.52f, previewColor.blue, 0.002f)
    }

    @Test
    fun darkPreviewColorMixesCourseAndCurrentBackground() {
        val previewColor = resolveCourseBlockBaseColor(
            normalCourseColor = androidx.compose.ui.graphics.Color(0.2f, 0.4f, 0.8f),
            scheduleBackgroundColor = androidx.compose.ui.graphics.Color(0.05f, 0.05f, 0.05f),
            showNextWeekPreview = true
        )

        assertEquals(0.14f, previewColor.red, 0.002f)
        assertEquals(0.26f, previewColor.green, 0.002f)
        assertEquals(0.5f, previewColor.blue, 0.002f)
    }

    @Test
    fun nextWeekChipOnlyReservesItsCompactMeasuredHeight() {
        assertEquals(0f, nextWeekChipReservedHeightDp(false, false, 1f))
        assertEquals(13f, nextWeekChipReservedHeightDp(true, false, 1f))
        assertEquals(17.5f, nextWeekChipReservedHeightDp(true, false, 1.5f))
        assertEquals(0f, nextWeekChipReservedHeightDp(true, true, 1f))
    }

    @Test
    fun fiveColumnsKeepSingleLineLocationAndTeacher() {
        val layout = resolveForColumnCount(5)

        assertEquals(1, layout.locationMaxLines)
        assertTrue(layout.teacherVisible)
    }

    @Test
    fun sixColumnsWrapLocationAndHideTeacher() {
        val layout = resolveForColumnCount(6)

        assertEquals(2, layout.locationMaxLines)
        assertFalse(layout.teacherVisible)
    }

    @Test
    fun sevenColumnsWrapLocationAndHideTeacher() {
        val layout = resolveForColumnCount(7)

        assertEquals(2, layout.locationMaxLines)
        assertFalse(layout.teacherVisible)
    }

    private fun resolveForColumnCount(columnCount: Int): CourseBlockDetailLayout {
        val gridWidth = 360f - 40f
        val outerAndInnerHorizontalPadding = 10f
        val availableTextWidth = gridWidth / columnCount - outerAndInnerHorizontalPadding

        return resolveCourseBlockDetailLayout(
            locationVisible = true,
            teacherAvailable = true,
            locationSingleLineWidthPx = locationSingleLineWidth,
            availableTextWidthPx = availableTextWidth,
            contentHeightPx = contentHeight,
            heightBeforeDetailsPx = heightBeforeDetails,
            detailLineHeightPx = detailLineHeight
        )
    }
}
