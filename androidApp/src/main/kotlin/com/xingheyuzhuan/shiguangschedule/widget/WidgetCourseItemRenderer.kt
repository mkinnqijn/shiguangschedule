package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import com.xingheyuzhuan.shiguangschedule.R

internal object WidgetCourseItemRenderer {
    fun create(
        context: Context,
        course: WidgetCourseProto,
        displayName: CharSequence,
        snapshot: WidgetSnapshot
    ): RemoteViews {
        val item = RemoteViews(context.packageName, R.layout.widget_item_course_common)
        val position = course.position.trim()
        val teacher = course.teacher.trim()
        val timeRange = formatTimeRange(course)
        val hasPosition = position.isNotEmpty()
        val hasTeacher = teacher.isNotEmpty()
        val hasSecondaryInfo = hasPosition || hasTeacher

        item.setTextViewText(R.id.tv_course_name, displayName)
        item.setTextViewText(R.id.tv_course_time, timeRange)
        item.setTextViewText(R.id.tv_course_time_inline, timeRange)

        if (hasSecondaryInfo) {
            // Complete courses keep three rows. With one optional field missing, the
            // remaining value moves into the second row next to the time.
            item.setViewVisibility(R.id.container_course_info_row, View.VISIBLE)
            item.setViewVisibility(R.id.tv_course_position, View.VISIBLE)
            item.setTextViewText(R.id.tv_course_position, if (hasPosition) position else teacher)
            item.setViewVisibility(
                R.id.tv_course_time,
                if (timeRange.isNotEmpty()) View.VISIBLE else View.GONE
            )
            item.setViewVisibility(R.id.tv_course_time_inline, View.GONE)

            val showTeacherRow = hasPosition && hasTeacher
            item.setViewVisibility(
                R.id.tv_course_teacher,
                if (showTeacherRow) View.VISIBLE else View.GONE
            )
            if (showTeacherRow) item.setTextViewText(R.id.tv_course_teacher, teacher)
        } else {
            // Name-only courses use one centered row, with the time kept at the end
            // when it is available. The fixed outer slot still retains its height.
            item.setViewVisibility(R.id.container_course_info_row, View.GONE)
            item.setViewVisibility(R.id.tv_course_teacher, View.GONE)
            item.setViewVisibility(
                R.id.tv_course_time_inline,
                if (timeRange.isNotEmpty()) View.VISIBLE else View.GONE
            )
        }

        val style = snapshot.style
        val colorIndex = course.color_int
        if (style != null && colorIndex in style.course_color_maps.indices) {
            val colorPair = style.course_color_maps[colorIndex]
            val nightMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val indicatorId = if (nightMode) R.id.course_indicator_dark else R.id.course_indicator
            val color = if (nightMode) colorPair.dark_color else colorPair.light_color
            item.setInt(indicatorId, "setColorFilter", color.toInt())
        }

        return item
    }

    private fun formatTimeRange(course: WidgetCourseProto): String {
        val start = course.start_time.take(5).trim()
        val end = course.end_time.take(5).trim()
        return when {
            start.isNotEmpty() && end.isNotEmpty() -> "$start-$end"
            start.isNotEmpty() -> start
            else -> end
        }
    }
}
