package com.xingheyuzhuan.shiguangschedule.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CourseNameDisplayTest {
    @Test
    fun fullWidthParenthesesAreConvertedForDisplay() {
        val storedName = "数学分析（新工科）I"

        assertEquals("数学分析(新工科)I", formatCourseNameForDisplay(storedName))
        assertEquals("数学分析（新工科）I", storedName)
    }

    @Test
    fun otherFullWidthCharactersAndPunctuationAreUnchanged() {
        val storedName = "Ａ１：数学，分析、 （一）"

        assertEquals("Ａ１：数学，分析、 (一)", formatCourseNameForDisplay(storedName))
    }

    @Test
    fun halfWidthParenthesesAreUnchanged() {
        assertEquals("数学分析(新工科)I", formatCourseNameForDisplay("数学分析(新工科)I"))
    }
}
