package com.xingheyuzhuan.shiguangschedule.data.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

internal data class SkippedDateSources(
    val effectiveDates: Set<String>,
    val officialDates: Set<String>,
    val manualDates: Set<String>,
    val excludedOfficialDates: Set<String>
)

internal fun mergeSkippedDates(
    officialDates: Set<String>,
    manualDates: Set<String>,
    excludedOfficialDates: Set<String> = emptySet()
): Set<String> = (officialDates - excludedOfficialDates) + manualDates

internal fun resolveSkippedDateSources(
    legacyDates: Set<String>,
    storedOfficialDates: Set<String>?,
    storedManualDates: Set<String>?,
    storedExcludedOfficialDates: Set<String>? = null
): SkippedDateSources {
    val officialDates = storedOfficialDates ?: legacyDates
    val manualDates = storedManualDates.orEmpty()
    val excludedOfficialDates = storedExcludedOfficialDates.orEmpty()
    return SkippedDateSources(
        effectiveDates = mergeSkippedDates(officialDates, manualDates, excludedOfficialDates),
        officialDates = officialDates,
        manualDates = manualDates,
        excludedOfficialDates = excludedOfficialDates
    )
}

internal fun removeSkippedDateFromSources(
    officialDates: Set<String>,
    manualDates: Set<String>,
    excludedOfficialDates: Set<String>,
    date: String
): SkippedDateSources {
    val updatedManualDates = manualDates - date
    val updatedExcludedOfficialDates = if (date in officialDates) {
        excludedOfficialDates + date
    } else {
        excludedOfficialDates
    }
    return SkippedDateSources(
        effectiveDates = mergeSkippedDates(
            officialDates = officialDates,
            manualDates = updatedManualDates,
            excludedOfficialDates = updatedExcludedOfficialDates
        ),
        officialDates = officialDates,
        manualDates = updatedManualDates,
        excludedOfficialDates = updatedExcludedOfficialDates
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
