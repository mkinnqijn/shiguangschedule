package com.xingheyuzhuan.shiguangschedule.ui.schedule

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourseHolidayDisplayStateTest {

    @Test
    fun resolvesExactDateFromSelectedWeekAndCourseDay() {
        assertEquals(
            LocalDate(2026, 10, 2),
            resolveCourseDateForWeek(
                semesterStartDate = LocalDate(2026, 8, 31),
                selectedWeekNumber = 5,
                courseDay = 5,
                firstDayOfWeek = 1
            )
        )
    }

    @Test
    fun respectsConfiguredSundayAsFirstDayOfWeek() {
        assertEquals(
            LocalDate(2026, 9, 7),
            resolveCourseDateForWeek(
                semesterStartDate = LocalDate(2026, 9, 2),
                selectedWeekNumber = 2,
                courseDay = 1,
                firstDayOfWeek = 7
            )
        )
    }

    @Test
    fun matchesOnlyTheExactSkippedDate() {
        val skippedDates = setOf("2026-10-02")

        assertTrue(
            isCourseDateSkipped(
                skippedDates = skippedDates,
                semesterStartDate = LocalDate(2026, 8, 31),
                selectedWeekNumber = 5,
                courseDay = 5,
                firstDayOfWeek = 1
            )
        )
        assertFalse(
            isCourseDateSkipped(
                skippedDates = skippedDates,
                semesterStartDate = LocalDate(2026, 8, 31),
                selectedWeekNumber = 6,
                courseDay = 5,
                firstDayOfWeek = 1
            )
        )
    }

    @Test
    fun missingSemesterConfigurationNeverMarksHoliday() {
        assertFalse(
            isCourseDateSkipped(
                skippedDates = setOf("2026-10-02"),
                semesterStartDate = null,
                selectedWeekNumber = 5,
                courseDay = 5,
                firstDayOfWeek = 1
            )
        )
    }
}
