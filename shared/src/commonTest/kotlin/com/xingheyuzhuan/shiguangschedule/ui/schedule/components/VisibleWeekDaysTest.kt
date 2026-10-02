package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import kotlin.test.Test
import kotlin.test.assertEquals

class VisibleWeekDaysTest {

    @Test
    fun noRelevantWeekendCoursesShowsOnlyWeekdays() {
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            resolveVisibleWeekDays(
                showWeekends = true,
                selectedWeekNumber = 3,
                courseWeeksByDay = mapOf(6 to setOf(5), 7 to setOf(2))
            )
        )
    }

    @Test
    fun saturdayCourseShowsOnlySaturday() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            resolveVisibleWeekDays(
                showWeekends = true,
                selectedWeekNumber = 3,
                courseWeeksByDay = mapOf(6 to setOf(3))
            )
        )
    }

    @Test
    fun sundayCourseShowsSundayWithoutEmptySaturday() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 7),
            resolveVisibleWeekDays(
                showWeekends = true,
                selectedWeekNumber = 3,
                courseWeeksByDay = mapOf(7 to setOf(3))
            )
        )
    }

    @Test
    fun bothWeekendCoursesShowBothDays() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6, 7),
            resolveVisibleWeekDays(
                showWeekends = true,
                selectedWeekNumber = 3,
                courseWeeksByDay = mapOf(6 to setOf(3), 7 to setOf(3))
            )
        )
    }

    @Test
    fun nextWeekPreviewTriggersButLaterWeeksDoNot() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            resolveVisibleWeekDays(
                showWeekends = true,
                selectedWeekNumber = 3,
                courseWeeksByDay = mapOf(6 to setOf(4), 7 to setOf(5))
            )
        )
    }

    @Test
    fun disabledWeekendSettingAlwaysShowsOnlyWeekdays() {
        assertEquals(
            listOf(1, 2, 3, 4, 5),
            resolveVisibleWeekDays(
                showWeekends = false,
                selectedWeekNumber = 3,
                courseWeeksByDay = mapOf(6 to setOf(3), 7 to setOf(3))
            )
        )
    }
}
