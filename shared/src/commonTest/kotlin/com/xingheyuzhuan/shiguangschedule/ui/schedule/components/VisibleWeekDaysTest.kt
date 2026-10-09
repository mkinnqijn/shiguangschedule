package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import com.xingheyuzhuan.shiguangschedule.ui.schedule.CourseWeekDisplayState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.resolveCourseWeekDisplayState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlin.test.Test
import kotlin.test.assertEquals

class VisibleWeekDaysTest {

    @Test
    fun noRelevantWeekendCoursesShowsOnlyWeekdays() {
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            resolveVisibleWeekDays(
                showWeekends = true,
                displayedCourseDays = setOf(1, 3, 5)
            )
        )
    }

    @Test
    fun adjustedCourseOn2026October10ShowsSaturday() {
        val adjustedDay = LocalDate(2026, 10, 10).dayOfWeek.isoDayNumber

        assertEquals(6, adjustedDay)
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            resolveVisibleWeekDays(
                showWeekends = true,
                displayedCourseDays = setOf(2, 4, adjustedDay)
            )
        )
    }

    @Test
    fun sundayCourseShowsSundayWithoutEmptySaturday() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 7),
            resolveVisibleWeekDays(
                showWeekends = true,
                displayedCourseDays = setOf(7)
            )
        )
    }

    @Test
    fun bothWeekendCoursesShowBothDays() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6, 7),
            resolveVisibleWeekDays(
                showWeekends = true,
                displayedCourseDays = setOf(6, 7)
            )
        )
    }

    @Test
    fun removingAdjustedWeekendCourseHidesItsColumnAgain() {
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            resolveVisibleWeekDays(
                showWeekends = true,
                displayedCourseDays = setOf(1, 2, 3, 4, 5)
            )
        )
    }

    @Test
    fun currentAndNextWeekWeekendCoursesAreIncludedButLaterCoursesAreNot() {
        val selectedWeek = 5
        val currentWeekDays = displayedDaysForWeek(
            selectedWeek = selectedWeek,
            courseWeeksByDay = mapOf(6 to setOf(5))
        )
        val previewWeekDays = displayedDaysForWeek(
            selectedWeek = selectedWeek,
            courseWeeksByDay = mapOf(6 to setOf(7), 7 to setOf(6))
        )

        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            resolveVisibleWeekDays(true, currentWeekDays)
        )
        assertEquals(
            listOf(1, 2, 3, 4, 5, 7),
            resolveVisibleWeekDays(true, previewWeekDays)
        )
    }

    @Test
    fun disabledWeekendSettingAlwaysShowsOnlyWeekdays() {
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            resolveVisibleWeekDays(
                showWeekends = false,
                displayedCourseDays = setOf(6, 7)
            )
        )
    }

    private fun displayedDaysForWeek(
        selectedWeek: Int,
        courseWeeksByDay: Map<Int, Set<Int>>
    ): Set<Int> = courseWeeksByDay
        .filterValues { weeks ->
            resolveCourseWeekDisplayState(weeks, selectedWeek) != CourseWeekDisplayState.HIDDEN
        }
        .keys
}
