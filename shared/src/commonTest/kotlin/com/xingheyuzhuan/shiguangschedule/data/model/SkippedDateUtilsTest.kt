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
    fun removingManualDateKeepsOverlappingOfficialDateAndOtherManualDates() {
        val officialDates = setOf("2027-10-01")
        val manualDates = setOf("2027-10-01", "2027-10-02", "2027-10-03")
        val updatedManualDates = manualDates - "2027-10-01"

        assertEquals(
            setOf("2027-10-01", "2027-10-02", "2027-10-03"),
            mergeSkippedDates(officialDates, updatedManualDates)
        )
        assertTrue("2027-10-01" !in updatedManualDates)
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
