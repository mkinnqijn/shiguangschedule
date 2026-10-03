package com.xingheyuzhuan.shiguangschedule.data.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SkippedDateUtilsTest {

    @Test
    fun sameStartAndEndCreatesSingleDate() {
        assertEquals(
            setOf("2027-01-15"),
            expandSkippedDateRange(
                startDate = LocalDate(2027, 1, 15),
                endDate = LocalDate(2027, 1, 15)
            )
        )
    }

    @Test
    fun rangeExpansionIncludesBothEndsAcrossYearBoundary() {
        assertEquals(
            setOf("2026-12-30", "2026-12-31", "2027-01-01", "2027-01-02"),
            expandSkippedDateRange(
                startDate = LocalDate(2026, 12, 30),
                endDate = LocalDate(2027, 1, 2)
            )
        )
    }

    @Test
    fun endBeforeStartIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            expandSkippedDateRange(
                startDate = LocalDate(2027, 2, 20),
                endDate = LocalDate(2027, 1, 15)
            )
        }
    }

    @Test
    fun officialAndManualDatesMergeWithoutDuplicates() {
        assertEquals(
            setOf("2027-01-01", "2027-01-02", "2027-01-03"),
            mergeSkippedDates(
                officialDates = setOf("2027-01-01", "2027-01-02"),
                manualDates = setOf("2027-01-02", "2027-01-03")
            )
        )
    }

    @Test
    fun officialRefreshKeepsManualDates() {
        val refreshedOfficialDates = setOf("2027-10-01", "2027-10-02")
        val manualDates = setOf("2027-01-15", "2027-10-02")

        assertEquals(
            setOf("2027-01-15", "2027-10-01", "2027-10-02"),
            mergeSkippedDates(refreshedOfficialDates, manualDates)
        )
    }

    @Test
    fun deletingOfficialDateRemovesOnlyThatDate() {
        val sources = removeSkippedDateFromSources(
            officialDates = setOf("2027-10-01", "2027-10-02", "2027-10-03"),
            manualDates = emptySet(),
            excludedOfficialDates = emptySet(),
            date = "2027-10-02"
        )

        assertEquals(setOf("2027-10-01", "2027-10-03"), sources.effectiveDates)
        assertEquals(setOf("2027-10-02"), sources.excludedOfficialDates)
    }

    @Test
    fun deletingDateRemovesBothManualAndOfficialSources() {
        val sources = removeSkippedDateFromSources(
            officialDates = setOf("2027-10-01"),
            manualDates = setOf("2027-10-01", "2027-10-02"),
            excludedOfficialDates = emptySet(),
            date = "2027-10-01"
        )

        assertEquals(setOf("2027-10-02"), sources.effectiveDates)
        assertEquals(setOf("2027-10-02"), sources.manualDates)
        assertEquals(setOf("2027-10-01"), sources.excludedOfficialDates)
    }

    @Test
    fun officialRefreshDoesNotRestoreUserExcludedDate() {
        val refreshedOfficialDates = setOf("2027-10-01", "2027-10-02", "2027-10-03")

        assertEquals(
            setOf("2027-10-01", "2027-10-03"),
            mergeSkippedDates(
                officialDates = refreshedOfficialDates,
                manualDates = emptySet(),
                excludedOfficialDates = setOf("2027-10-02")
            )
        )
    }

    @Test
    fun userExclusionSurvivesSettingsReload() {
        val sources = resolveSkippedDateSources(
            legacyDates = setOf("2027-10-01", "2027-10-02"),
            storedOfficialDates = setOf("2027-10-01", "2027-10-02"),
            storedManualDates = emptySet(),
            storedExcludedOfficialDates = setOf("2027-10-02")
        )

        assertEquals(setOf("2027-10-01"), sources.effectiveDates)
        assertEquals(setOf("2027-10-02"), sources.excludedOfficialDates)
    }

    @Test
    fun legacyDatesArePreservedAsOfficialSource() {
        val sources = resolveSkippedDateSources(
            legacyDates = setOf("2026-10-01", "2026-10-02"),
            storedOfficialDates = null,
            storedManualDates = null
        )

        assertEquals(sources.effectiveDates, sources.officialDates)
        assertTrue(sources.manualDates.isEmpty())
    }
}
