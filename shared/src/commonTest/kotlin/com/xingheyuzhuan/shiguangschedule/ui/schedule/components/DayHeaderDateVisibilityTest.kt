package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DayHeaderDateVisibilityTest {

    @Test
    fun defaultFontScaleKeepsDateUntilTwoLinesNoLongerFit() {
        assertTrue(shouldShowDayHeaderDate(42f, false, 1f))
        assertTrue(shouldShowDayHeaderDate(39f, false, 1f))
        assertTrue(shouldShowDayHeaderDate(35f, false, 1f))
        assertTrue(shouldShowDayHeaderDate(32f, false, 1f))
        assertFalse(shouldShowDayHeaderDate(31f, false, 1f))
        assertFalse(shouldShowDayHeaderDate(30f, false, 1f))
    }

    @Test
    fun largerSystemFontScaleRaisesRequiredHeight() {
        assertTrue(shouldShowDayHeaderDate(42f, false, 1.3f))
        assertFalse(shouldShowDayHeaderDate(39f, false, 1.3f))
    }

    @Test
    fun explicitHideSettingAlwaysHidesDate() {
        assertFalse(shouldShowDayHeaderDate(80f, true, 1f))
    }
}
