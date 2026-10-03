package com.xingheyuzhuan.shiguangschedule.data.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

internal data class SkippedDateSources(
    val effectiveDates: Set<String>,
    val officialDates: Set<String>,
    val manualDates: Set<String>
)

internal fun mergeSkippedDates(
    officialDates: Set<String>,
    manualDates: Set<String>
): Set<String> = officialDates + manualDates

internal fun resolveSkippedDateSources(
    legacyDates: Set<String>,
    storedOfficialDates: Set<String>?,
    storedManualDates: Set<String>?
): SkippedDateSources {
    val officialDates = storedOfficialDates ?: legacyDates
    val manualDates = storedManualDates.orEmpty()
    return SkippedDateSources(
        effectiveDates = mergeSkippedDates(officialDates, manualDates),
        officialDates = officialDates,
        manualDates = manualDates
    )
}

internal fun expandSkippedDateRange(
    startDate: LocalDate,
    endDate: LocalDate
): Set<String> {
    require(endDate >= startDate) { "End date must not be before start date" }

    val dates = linkedSetOf<String>()
    var date = startDate
    while (date <= endDate) {
        dates += date.toString()
        date = date.plus(1, DateTimeUnit.DAY)
    }
    return dates
}
