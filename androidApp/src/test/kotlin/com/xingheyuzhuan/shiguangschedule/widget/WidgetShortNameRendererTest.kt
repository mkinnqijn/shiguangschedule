package com.xingheyuzhuan.shiguangschedule.widget

import android.app.Application
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.ui.formatCourseNameForDisplay
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeRenderer
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WidgetShortNameRendererTest {
    @Test
    fun allRenderersUseTheRightNameAndKeepCourseDetails() {
        val context = RuntimeEnvironment.getApplication()
        val course = WidgetCourseProto(
            id = "course", name = "数学分析（新工科）I", widget_short_name = "自定简称（原样）",
            teacher = "教师", position = "教室", start_time = "00:00", end_time = "23:59:59",
            date = LocalDate.now().toString()
        )
        val renderers = listOf(
            TinyNativeRenderer::render to true,
            CompactNativeRenderer::render to true,
            DoubleDaysNativeRenderer::render to true,
            ListVerticalNativeRenderer::render to false
        )
        for (shortName in listOf("自定简称（原样）", "", "  ")) {
            val snapshot = WidgetSnapshot(current_week = 4, courses = listOf(course.copy(widget_short_name = shortName)))
            for ((render, usesShortName) in renderers) {
                val view = render(context, snapshot).apply(context, FrameLayout(context))
                val expected = if (usesShortName && shortName.isNotBlank()) {
                    "自定简称（原样）"
                } else {
                    formatCourseNameForDisplay(course.name)
                }
                assertEquals(expected, view.findViewById<TextView>(R.id.tv_course_name).text.toString())
                assertEquals("数学分析（新工科）I", course.name)
                assertEquals(course.position, view.findViewById<TextView>(R.id.tv_course_position).text.toString())
                // Widgets that already display a teacher must keep it.
                view.findViewById<TextView>(R.id.tv_course_teacher)?.let {
                    assertEquals(View.VISIBLE, it.visibility)
                    assertEquals(course.teacher, it.text.toString())
                }
            }
        }
    }
}
