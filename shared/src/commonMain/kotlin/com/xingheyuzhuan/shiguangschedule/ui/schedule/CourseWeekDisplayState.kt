package com.xingheyuzhuan.shiguangschedule.ui.schedule

internal enum class CourseWeekDisplayState {
    CURRENT_WEEK,
    NEXT_WEEK_PREVIEW,
    HIDDEN
}

internal fun resolveCourseWeekDisplayState(
    courseWeeks: Iterable<Int>,
    selectedWeekNumber: Int
): CourseWeekDisplayState {
    var isActiveNextWeek = false

    for (weekNumber in courseWeeks) {
        when (weekNumber) {
            selectedWeekNumber -> return CourseWeekDisplayState.CURRENT_WEEK
            selectedWeekNumber + 1 -> isActiveNextWeek = true
        }
    }

    return if (isActiveNextWeek) {
        CourseWeekDisplayState.NEXT_WEEK_PREVIEW
    } else {
        CourseWeekDisplayState.HIDDEN
    }
}
