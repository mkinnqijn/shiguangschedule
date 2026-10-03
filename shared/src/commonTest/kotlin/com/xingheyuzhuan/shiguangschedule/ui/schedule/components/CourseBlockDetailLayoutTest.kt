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
            resolveCourseBlockBaseColor(
                normalCourseColor = normalColor,
                scheduleBackgroundColor = backgroundColor,
                showNextWeekPreview = false
            )
        )
    }

    @Test
    fun lightPreviewColorMixesCourseAndCurrentBackground() {
        val previewColor = resolveCourseBlockBaseColor(
            normalCourseColor = androidx.compose.ui.graphics.Color(0.8f, 0.4f, 0.2f),
            scheduleBackgroundColor = androidx.compose.ui.graphics.Color(1f, 1f, 1f),
            showNextWeekPreview = true
        )

        assertEquals(0.8139f, previewColor.red, 0.002f)
        assertEquals(0.5899f, previewColor.green, 0.002f)
        assertEquals(0.4779f, previewColor.blue, 0.002f)
    }

    @Test
    fun darkPreviewColorMixesCourseAndCurrentBackground() {
        val previewColor = resolveCourseBlockBaseColor(
            normalCourseColor = androidx.compose.ui.graphics.Color(0.2f, 0.4f, 0.8f),
            scheduleBackgroundColor = androidx.compose.ui.graphics.Color(0.05f, 0.05f, 0.05f),
            showNextWeekPreview = true
        )

        assertEquals(0.1811f, previewColor.red, 0.002f)
        assertEquals(0.2931f, previewColor.green, 0.002f)
        assertEquals(0.5171f, previewColor.blue, 0.002f)
    }

    @Test
    fun nextWeekChipHasStableTopSpaceIndependentFromBodyOffset() {
        val headerHeight = nextWeekHeaderReservedHeightDp(true, 1f)

        assertEquals(15f, headerHeight)
        assertEquals(15f, resolveCourseBodyTopPaddingDp(true, false, 4f, 6f, headerHeight))
        assertEquals(80f, resolveCourseBodyTopPaddingDp(true, false, 4f, 80f, headerHeight))
        assertEquals(15f, resolveCourseBodyTopPaddingDp(true, true, 4f, 80f, headerHeight))
        assertEquals(6f, resolveCourseBodyTopPaddingDp(false, false, 4f, 6f, headerHeight))
    }

    @Test
    fun nextWeekPreviewWeakensAlphaWithoutChangingNormalCourse() {
        assertEquals(0.82f, resolveCourseBlockAlpha(1f, true))
        assertEquals(0.41f, resolveCourseBlockAlpha(0.5f, true))
        assertEquals(0.5f, resolveCourseBlockAlpha(0.5f, false))
    }

    @Test
    fun nextWeekPreviewWeakensContentAndChipTogether() {
        assertEquals(0.55f, NEXT_WEEK_TEXT_ALPHA_FACTOR)
        assertEquals(0.44f, NEXT_WEEK_SECONDARY_TEXT_ALPHA_FACTOR)
        assertEquals(0.11f, NEXT_WEEK_CHIP_BACKGROUND_ALPHA)
        assertEquals(0.47f, NEXT_WEEK_CHIP_TEXT_ALPHA)
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
