package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import com.xingheyuzhuan.shiguangschedule.ui.schedule.CourseWeekDisplayState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.resolveCourseWeekDisplayState

private val WEEKDAYS = (1..5).toList()
private val WEEKEND_DAYS = listOf(6, 7)

/**
 * Returns Monday through Friday plus only the weekend days that have a course
 * in the selected week or the immediately following preview week.
 */
internal fun resolveVisibleWeekDays(
    showWeekends: Boolean,
    selectedWeekNumber: Int?,
    courseWeeksByDay: Map<Int, Set<Int>>
): List<Int> {
    if (!showWeekends || selectedWeekNumber == null) return WEEKDAYS

    val visibleWeekendDays = WEEKEND_DAYS.filter { day ->
        resolveCourseWeekDisplayState(
            courseWeeks = courseWeeksByDay[day].orEmpty(),
            selectedWeekNumber = selectedWeekNumber
        ) != CourseWeekDisplayState.HIDDEN
    }

    return WEEKDAYS + visibleWeekendDays
}
