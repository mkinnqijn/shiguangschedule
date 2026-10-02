package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

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

    val relevantWeeks = selectedWeekNumber..(selectedWeekNumber + 1)
    val visibleWeekendDays = WEEKEND_DAYS.filter { day ->
        courseWeeksByDay[day].orEmpty().any { week -> week in relevantWeeks }
    }

    return WEEKDAYS + visibleWeekendDays
}
