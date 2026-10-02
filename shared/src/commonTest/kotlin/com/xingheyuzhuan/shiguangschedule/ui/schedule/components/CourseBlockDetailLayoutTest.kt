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
