package com.xingheyuzhuan.shiguangschedule.ui

/** Formats a stored course name for display without changing the source value. */
fun formatCourseNameForDisplay(name: String): String =
    name.replace('（', '(').replace('）', ')')
