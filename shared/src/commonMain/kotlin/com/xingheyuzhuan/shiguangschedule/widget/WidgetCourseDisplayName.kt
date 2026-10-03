package com.xingheyuzhuan.shiguangschedule.widget

import com.xingheyuzhuan.shiguangschedule.ui.formatCourseNameForDisplay

/** Use only in narrow widgets; full-width views continue to read [WidgetCourseProto.name]. */
fun WidgetCourseProto.narrowDisplayName(): String =
    widget_short_name.trim().takeIf { it.isNotEmpty() } ?: formatCourseNameForDisplay(name)
