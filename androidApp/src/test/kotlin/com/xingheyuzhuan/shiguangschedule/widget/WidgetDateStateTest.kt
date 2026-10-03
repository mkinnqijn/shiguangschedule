package com.xingheyuzhuan.shiguangschedule.widget

import android.app.Application
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeRenderer
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-mdpi")
class WidgetDateStateTest {
    private val today = LocalDate.of(2026, 10, 3)
    private val now = LocalTime.of(17, 19)

    @Test
    fun allTodayAndRemainingTodayAreCalculatedSeparately() {
        val snapshot = snapshot(
            course("已结束", today, "15:25", "17:00"),
            course("正在上课", today, "17:00", "18:00"),
            course("尚未开始", today, "18:30", "20:05")
        )

        val selection = snapshot.selectTodayCourses(today, now)

        assertEquals(listOf("已结束", "正在上课", "尚未开始"), selection.allTodayCourses.map { it.name })
        assertEquals(listOf("正在上课", "尚未开始"), selection.remainingTodayCourses.map { it.name })
    }

    @Test
    fun endedCourseIsRemovedButFutureCourseRemainsInBothWidgets() {
        val snapshot = snapshot(
            course("程序设计原理", today, "15:25", "17:00"),
            course("数学分析", today, "18:30", "20:05")
        )

        val listRoot = listRoot(snapshot)
        val doubleRoot = doubleRoot(snapshot)

        assertEquals(listOf("数学分析"), courseNames(listRoot, R.id.container_courses))
        assertEquals(listOf("数学分析"), courseNames(doubleRoot, R.id.container_today))
        assertEquals(
            listRoot.context.getString(R.string.widget_remaining_courses_format_today, 1),
            listRoot.findViewById<TextView>(R.id.tv_header_count_summary).text.toString()
        )
    }

    @Test
    fun ongoingCourseIsKept() {
        val snapshot = snapshot(course("正在上课", today, "17:00", "18:00"))
        assertEquals(listOf("正在上课"), courseNames(listRoot(snapshot), R.id.container_courses))
        assertEquals(listOf("正在上课"), courseNames(doubleRoot(snapshot), R.id.container_today))
    }

    @Test
    fun allEndedTodayShowsFinishedInsteadOfNoCourseOrTomorrowPreview() {
        val snapshot = snapshot(
            course("已结束一", today, "08:00", "09:00"),
            course("已结束二", today, "10:00", "11:00"),
            course("明日课程", today.plusDays(1), "08:00", "09:00")
        )

        val listRoot = listRoot(snapshot)
        assertEquals(View.VISIBLE, listRoot.findViewById<View>(R.id.container_status).visibility)
        assertEquals(
            listRoot.context.getString(R.string.widget_today_courses_finished),
            listRoot.findViewById<TextView>(R.id.tv_status_title).text.toString()
        )
        assertFalse(listRoot.findViewById<TextView>(R.id.tv_header_title).text.toString().contains("明日预告"))

        val doubleRoot = doubleRoot(snapshot)
        assertEquals(View.VISIBLE, doubleRoot.findViewById<View>(R.id.empty_today_container).visibility)
        assertEquals(
            doubleRoot.context.getString(R.string.widget_today_courses_finished),
            doubleRoot.findViewById<TextView>(R.id.empty_today).text.toString()
        )
    }

    @Test
    fun trulyEmptyTodayUsesNoCourseState() {
        val snapshot = snapshot()
        val listRoot = listRoot(snapshot)
        assertEquals(View.VISIBLE, listRoot.findViewById<View>(R.id.container_status).visibility)
        assertEquals(
            listRoot.context.getString(R.string.text_no_courses_today),
            listRoot.findViewById<TextView>(R.id.tv_status_title).text.toString()
        )

        val doubleRoot = doubleRoot(snapshot)
        assertEquals(
            doubleRoot.context.getString(R.string.text_no_course),
            doubleRoot.findViewById<TextView>(R.id.empty_today).text.toString()
        )
    }

    @Test
    fun tomorrowIsPreviewedWhenTodayIsTrulyEmpty() {
        val root = listRoot(snapshot(course("明日课程", today.plusDays(1), "08:00", "09:00")))
        assertEquals(
            root.context.getString(R.string.widget_tomorrow_course_preview),
            root.findViewById<TextView>(R.id.tv_header_title).text.toString()
        )
        assertEquals(listOf("明日课程"), courseNames(root, R.id.container_courses))
    }

    @Test
    fun skippedAndNextWeekCoursesAreNotCountedAsToday() {
        val root = listRoot(snapshot(
            course("跳过课程", today, "18:30", "20:05", skipped = true),
            course("下周预览", today.plusDays(7), "18:30", "20:05")
        ))
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.container_status).visibility)
        assertEquals(root.context.getString(R.string.text_no_courses_today),
            root.findViewById<TextView>(R.id.tv_status_title).text.toString())
    }

    @Test
    fun doubleDaysHasOnlyBetweenCourseDividersForOneTwoAndThreeRemainingCourses() {
        for (count in 1..3) {
            val courses = (1..count).map { index ->
                course("剩余课程$index", today, "18:${20 + index}", "20:0$index")
            }
            val root = doubleRoot(snapshot(*courses.toTypedArray()))
            val container = root.findViewById<ViewGroup>(R.id.container_today)

            assertEquals(count * 2 - 1, container.childCount)
            assertEquals(count, courseNames(root, R.id.container_today).size)
            assertEquals("剩余课程$count",
                container.getChildAt(container.childCount - 1)
                    .findViewById<TextView>(R.id.tv_course_name).text.toString())
            assertEquals(
                root.context.getString(R.string.widget_course_remaining_count, count),
                root.findViewById<TextView>(R.id.tv_today_footer).text.toString()
            )
        }
    }

    @Test
    fun doubleDaysDividerStateDoesNotLeakAcrossHostUpdates() {
        val context = RuntimeEnvironment.getApplication()
        val host = AppWidgetHostView(context).apply {
            setAppWidget(1, AppWidgetProviderInfo().apply {
                provider = ComponentName(context, DoubleDaysNativeProvider::class.java)
            })
        }

        for (count in listOf(3, 2, 1, 3)) {
            val courses = (1..count).map { index ->
                course("刷新课程$index", today, "18:${20 + index}", "20:0$index")
            }
            host.updateAppWidget(
                DoubleDaysNativeRenderer.renderAt(
                    context,
                    snapshot(*courses.toTypedArray()),
                    today,
                    now
                )
            )

            val container = host.findViewById<ViewGroup>(R.id.container_today)
            assertEquals(count * 2 - 1, container.childCount)
            assertEquals("刷新课程$count",
                container.getChildAt(container.childCount - 1)
                    .findViewById<TextView>(R.id.tv_course_name).text.toString())
        }
    }

    private fun listRoot(snapshot: WidgetSnapshot): ViewGroup {
        val context = RuntimeEnvironment.getApplication()
        return ListVerticalNativeRenderer.renderAt(context, snapshot, today, now)
            .apply(context, FrameLayout(context)) as ViewGroup
    }

    private fun doubleRoot(snapshot: WidgetSnapshot): ViewGroup {
        val context = RuntimeEnvironment.getApplication()
        return DoubleDaysNativeRenderer.renderAt(context, snapshot, today, now)
            .apply(context, FrameLayout(context)) as ViewGroup
    }

    private fun snapshot(vararg courses: WidgetCourseProto) =
        WidgetSnapshot(current_week = 4, courses = courses.toList())

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
