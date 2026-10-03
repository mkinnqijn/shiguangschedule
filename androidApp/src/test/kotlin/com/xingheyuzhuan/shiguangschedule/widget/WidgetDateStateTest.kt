package com.xingheyuzhuan.shiguangschedule.widget

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeRenderer
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-mdpi")
class WidgetDateStateTest {
    private val today = LocalDate.now()

    @Test
    fun todayCourseThatHasNotStartedIsStillToday() {
        assertListShowsToday(listOf(course("未来课程", today, "23:58", "23:59")))
    }

    @Test
    fun ongoingTodayCourseIsStillToday() {
        assertListShowsToday(listOf(course("正在上课", today, "00:00", "23:59")))
    }

    @Test
    fun allEndedTodayCoursesDoNotSwitchToTomorrowPreview() {
        val root = listRoot(listOf(
            course("已结束一", today, "08:00", "09:00"),
            course("已结束二", today, "10:00", "11:00"),
            course("明日课程", today.plusDays(1), "08:00", "09:00")
        ))
        assertEquals(listOf("已结束一", "已结束二"), courseNames(root, R.id.container_courses))
        assertFalse(root.findViewById<TextView>(R.id.tv_header_title).text.toString().contains("明日预告"))
    }

    @Test
    fun trulyEmptyTodayShowsNoCourse() {
        val root = listRoot(emptyList())
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.container_status).visibility)
        assertEquals(root.context.getString(R.string.text_no_courses_today),
            root.findViewById<TextView>(R.id.tv_status_title).text.toString())
    }

    @Test
    fun nextWeekPreviewCourseIsNotCountedAsToday() {
        val root = listRoot(listOf(course("下周预览", today.plusDays(7), "08:00", "09:00")))
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.container_status).visibility)
        assertFalse(courseNames(root, R.id.container_courses).contains("下周预览"))
    }

    @Test
    fun tomorrowCourseIsPreviewedOnlyWhenTodayIsTrulyEmpty() {
        val root = listRoot(listOf(course("明日课程", today.plusDays(1), "08:00", "09:00")))
        assertEquals(root.context.getString(R.string.widget_tomorrow_course_preview),
            root.findViewById<TextView>(R.id.tv_header_title).text.toString())
        assertEquals(listOf("明日课程"), courseNames(root, R.id.container_courses))
    }

    @Test
    fun skippedTodayCourseDoesNotBlockTomorrowPreview() {
        val root = listRoot(listOf(
            course("跳过课程", today, "08:00", "09:00", skipped = true),
            course("明日课程", today.plusDays(1), "08:00", "09:00")
        ))
        assertEquals(listOf("明日课程"), courseNames(root, R.id.container_courses))
    }

    @Test
    fun doubleDaysUsesTheCompleteTodayCourseSet() {
        val context = RuntimeEnvironment.getApplication()
        val courses = listOf(
            course("上午一", today, "08:00", "09:00"),
            course("上午二", today, "10:00", "11:00"),
            course("数学分析", today, "15:25", "17:00")
        )
        val root = DoubleDaysNativeRenderer.render(context, WidgetSnapshot(current_week = 4, courses = courses))
            .apply(context, FrameLayout(context)) as ViewGroup

        assertEquals(listOf("上午一", "上午二", "数学分析"), courseNames(root, R.id.container_today))
        assertEquals(context.getString(R.string.widget_course_total_count, 3),
            root.findViewById<TextView>(R.id.tv_today_footer).text.toString())
    }

    private fun assertListShowsToday(courses: List<WidgetCourseProto>) {
        val root = listRoot(courses)
        assertEquals(courses.map { it.name }, courseNames(root, R.id.container_courses))
        assertEquals(root.context.getString(R.string.widget_courses_format_today, courses.size),
            root.findViewById<TextView>(R.id.tv_header_count_summary).text.toString())
    }

    private fun listRoot(courses: List<WidgetCourseProto>): ViewGroup {
        val context = RuntimeEnvironment.getApplication()
        return ListVerticalNativeRenderer.render(context, WidgetSnapshot(current_week = 4, courses = courses))
            .apply(context, FrameLayout(context)) as ViewGroup
    }

    private fun course(
        name: String,
        date: LocalDate,
        start: String,
        end: String,
        skipped: Boolean = false
    ) = WidgetCourseProto(
        id = "$name-$date",
        name = name,
        teacher = "教师",
        position = "教室",
        start_time = start,
        end_time = end,
        is_skipped = skipped,
        date = date.toString()
    )

    private fun courseNames(root: ViewGroup, containerId: Int): List<String> {
        val container = root.findViewById<ViewGroup>(containerId)
        return (0 until container.childCount)
            .map { container.getChildAt(it) }
            .filterIsInstance<ViewGroup>()
            .mapNotNull { it.findViewById<TextView?>(R.id.tv_course_name)?.text?.toString() }
    }
}
