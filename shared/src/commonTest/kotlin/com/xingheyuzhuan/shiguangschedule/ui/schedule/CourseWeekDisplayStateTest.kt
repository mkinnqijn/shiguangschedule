package com.xingheyuzhuan.shiguangschedule.ui.schedule

import kotlin.test.Test
import kotlin.test.assertEquals

class CourseWeekDisplayStateTest {

    @Test
    fun currentWeekCourseDisplaysNormally() {
        assertEquals(
            CourseWeekDisplayState.CURRENT_WEEK,
            resolveCourseWeekDisplayState(setOf(4), selectedWeekNumber = 4)
        )
    }

    @Test
    fun nextWeekCourseDisplaysAsPreview() {
        assertEquals(
            CourseWeekDisplayState.NEXT_WEEK_PREVIEW,
            resolveCourseWeekDisplayState(setOf(5), selectedWeekNumber = 4)
        )
    }

    @Test
    fun courseStartingTwoWeeksLaterIsHidden() {
        assertEquals(
            CourseWeekDisplayState.HIDDEN,
            resolveCourseWeekDisplayState(setOf(6, 7, 8), selectedWeekNumber = 4)
        )
    }

    @Test
    fun endedCourseIsHidden() {
        assertEquals(
            CourseWeekDisplayState.HIDDEN,
            resolveCourseWeekDisplayState(setOf(1, 2, 3), selectedWeekNumber = 4)
        )
    }

    @Test
    fun alternatingWeekCourseOnlyPreviewsImmediatelyBeforeActiveWeek() {
        val oddWeeks = (1..15 step 2).toSet()
        val evenWeeks = (2..16 step 2).toSet()

        assertEquals(
            CourseWeekDisplayState.NEXT_WEEK_PREVIEW,
            resolveCourseWeekDisplayState(oddWeeks, selectedWeekNumber = 2)
        )
        assertEquals(
            CourseWeekDisplayState.CURRENT_WEEK,
            resolveCourseWeekDisplayState(oddWeeks, selectedWeekNumber = 3)
        )
        assertEquals(
            CourseWeekDisplayState.HIDDEN,
            resolveCourseWeekDisplayState(oddWeeks, selectedWeekNumber = 16)
        )
        assertEquals(
            CourseWeekDisplayState.NEXT_WEEK_PREVIEW,
            resolveCourseWeekDisplayState(evenWeeks, selectedWeekNumber = 3)
        )
        assertEquals(
            CourseWeekDisplayState.CURRENT_WEEK,
            resolveCourseWeekDisplayState(evenWeeks, selectedWeekNumber = 4)
        )
    }

    @Test
    fun stagedTimeChangeOnlyShowsCurrentAndNextSegments() {
        val earlyTimeWeeks = (1..8).toSet()
        val lateTimeWeeks = (9..16).toSet()

        assertEquals(
            CourseWeekDisplayState.HIDDEN,
            resolveCourseWeekDisplayState(lateTimeWeeks, selectedWeekNumber = 7)
        )
        assertEquals(
            CourseWeekDisplayState.CURRENT_WEEK,
            resolveCourseWeekDisplayState(earlyTimeWeeks, selectedWeekNumber = 8)
        )
        assertEquals(
            CourseWeekDisplayState.NEXT_WEEK_PREVIEW,
            resolveCourseWeekDisplayState(lateTimeWeeks, selectedWeekNumber = 8)
        )
        assertEquals(
            CourseWeekDisplayState.HIDDEN,
            resolveCourseWeekDisplayState(earlyTimeWeeks, selectedWeekNumber = 9)
        )
        assertEquals(
            CourseWeekDisplayState.CURRENT_WEEK,
            resolveCourseWeekDisplayState(lateTimeWeeks, selectedWeekNumber = 9)
        )
    }

    @Test
    fun oneTimeCourseOnlyAppearsDuringItsWeekAndTheWeekBefore() {
        val oneTimeCourseWeek = setOf(10)

        assertEquals(
            CourseWeekDisplayState.HIDDEN,
            resolveCourseWeekDisplayState(oneTimeCourseWeek, selectedWeekNumber = 8)
        )
        assertEquals(
            CourseWeekDisplayState.NEXT_WEEK_PREVIEW,
            resolveCourseWeekDisplayState(oneTimeCourseWeek, selectedWeekNumber = 9)
        )
        assertEquals(
            CourseWeekDisplayState.CURRENT_WEEK,
            resolveCourseWeekDisplayState(oneTimeCourseWeek, selectedWeekNumber = 10)
        )
        assertEquals(
            CourseWeekDisplayState.HIDDEN,
            resolveCourseWeekDisplayState(oneTimeCourseWeek, selectedWeekNumber = 11)
        )
    }
}
