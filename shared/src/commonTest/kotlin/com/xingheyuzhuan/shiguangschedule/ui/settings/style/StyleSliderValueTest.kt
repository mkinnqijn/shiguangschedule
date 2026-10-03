package com.xingheyuzhuan.shiguangschedule.ui.settings.style

import kotlin.test.Test
import kotlin.test.assertEquals

class StyleSliderValueTest {

    @Test
    fun integerSliderSnapsEveryValueIncludingTwo() {
        val range = 0f..12f

        assertEquals(11, styleSliderSteps(range, 1f))
        for (expected in 0..12) {
            val valueWithFloatNoise = expected.toFloat() - 0.000001f
            assertEquals(
                expected.toFloat(),
                snapStyleSliderValue(valueWithFloatNoise, range, 1f)
            )
        }
    }

    @Test
    fun courseTextTopPaddingRangeHasOneDpSteps() {
        assertEquals(19, styleSliderSteps(0f..20f, 1f))
        assertEquals(2f, snapStyleSliderValue(1.999999f, 0f..20f, 1f))
    }
}
