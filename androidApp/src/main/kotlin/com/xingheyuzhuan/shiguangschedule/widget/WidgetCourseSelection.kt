package com.xingheyuzhuan.shiguangschedule.widget

import java.time.LocalDate
import java.time.LocalTime

internal data class TodayCourseSelection(
    val allTodayCourses: List<WidgetCourseProto>,
    val remainingTodayCourses: List<WidgetCourseProto>
)

/**
 * Separates the question "did today have classes?" from the list that should
 * still be displayed at the current time.
 */
internal fun WidgetSnapshot.selectTodayCourses(
    today: LocalDate = LocalDate.now(),
    now: LocalTime = LocalTime.now()
): TodayCourseSelection {
    val allTodayCourses = validCoursesOn(today)
    val remainingTodayCourses = allTodayCourses.filter { course ->
        runCatching { LocalTime.parse(course.end_time) > now }.getOrDefault(true)
    }
    return TodayCourseSelection(allTodayCourses, remainingTodayCourses)
}

internal fun WidgetSnapshot.validCoursesOn(date: LocalDate): List<WidgetCourseProto> =
    courses.filter { course ->
        course.date == date.toString() && !course.is_skipped
    }.sortedBy { it.start_time }
