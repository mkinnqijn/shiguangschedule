package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

private val WEEKDAYS = (1..5).toList()
private val WEEKEND_DAYS = listOf(6, 7)

/**
 * Returns Monday through Friday plus only the weekend days present in the
 * page's final, already-filtered course blocks.
 */
internal fun resolveVisibleWeekDays(
    showWeekends: Boolean,
    displayedCourseDays: Set<Int>
): List<Int> {
    if (!showWeekends) return WEEKDAYS

    val visibleWeekendDays = WEEKEND_DAYS.filter(displayedCourseDays::contains)

    return WEEKDAYS + visibleWeekendDays
}
