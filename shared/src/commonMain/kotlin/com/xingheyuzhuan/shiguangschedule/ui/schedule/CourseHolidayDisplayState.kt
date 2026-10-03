package com.xingheyuzhuan.shiguangschedule.ui.schedule

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

internal fun resolveCourseDateForWeek(
    semesterStartDate: LocalDate?,
    selectedWeekNumber: Int,
    courseDay: Int,
    firstDayOfWeek: Int
): LocalDate? {
    if (semesterStartDate == null || selectedWeekNumber < 1 || courseDay !in 1..7 || firstDayOfWeek !in 1..7) {
        return null
    }

    val daysBeforeFirstWeek =
        (semesterStartDate.dayOfWeek.isoDayNumber - firstDayOfWeek + 7) % 7
    val alignedSemesterStart = semesterStartDate.minus(
        daysBeforeFirstWeek.toLong(),
        DateTimeUnit.DAY
    )
    val courseDayOffset = (courseDay - firstDayOfWeek + 7) % 7

    return alignedSemesterStart
        .plus((selectedWeekNumber - 1).toLong(), DateTimeUnit.WEEK)
        .plus(courseDayOffset.toLong(), DateTimeUnit.DAY)
}

internal fun isCourseDateSkipped(
    skippedDates: Set<String>,
    semesterStartDate: LocalDate?,
    selectedWeekNumber: Int,
    courseDay: Int,
    firstDayOfWeek: Int
): Boolean {
    val courseDate = resolveCourseDateForWeek(
        semesterStartDate = semesterStartDate,
        selectedWeekNumber = selectedWeekNumber,
        courseDay = courseDay,
        firstDayOfWeek = firstDayOfWeek
    ) ?: return false

    return courseDate.toString() in skippedDates
}
