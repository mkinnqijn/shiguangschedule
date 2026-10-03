package com.xingheyuzhuan.shiguangschedule.widget

import android.app.Application
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-mdpi")
class DoubleDaysLayoutTest {
    @Test
    fun twoCoursesKeepAllFourFieldsFullyVisibleInBothColumns() = verifyTwoCourses()

    @Test
    @Config(qualifiers = "zh-rCN-night-mdpi")
    fun twoCoursesKeepAllFieldsInNightMode() = verifyTwoCourses()

    @Test
    fun oldSpacingReproducesClippedSecondTeacherAt180dp() {
        val root = createWidget()
        val card = root.findViewById<View>(R.id.inner_content_card)
        card.setPadding(14, 14, 14, 14)
        for (id in listOf(R.id.container_today, R.id.container_tomorrow)) {
            val column = root.findViewById<View>(id)
            column.layoutParams = (column.layoutParams as ViewGroup.MarginLayoutParams).apply { topMargin = 8 }
        }
        measure(root, 180)
        val column = root.findViewById<ViewGroup>(R.id.container_tomorrow)
        val teacher = column.getChildAt(2).findViewById<TextView>(R.id.tv_course_teacher)
        assertTrue("Original spacing should reproduce a clipped text line",
            teacher.height < teacher.layout.height)
    }

    private fun verifyTwoCourses() {
        for (height in listOf(180, 220, 260)) {
            val root = createWidget()
            measure(root, height)
            val card = root.findViewById<View>(R.id.inner_content_card)
            for (id in listOf(R.id.container_today, R.id.container_tomorrow)) {
                val column = root.findViewById<ViewGroup>(id)
                assertEquals(3, column.childCount) // Two full items and their divider.
                for (index in listOf(0, 2)) {
                    val item = column.getChildAt(index)
                    val fields = listOf(
                        R.id.tv_course_name to "正式课程名称",
                        R.id.tv_course_position to "教学楼101",
                        R.id.tv_course_time to "08:30-23:59",
                        R.id.tv_course_teacher to "任课教师"
                    )
                    for ((fieldId, expected) in fields) {
                        val text = item.findViewById<TextView>(fieldId)
                        assertEquals(expected, text.text.toString())
                        assertEquals(View.VISIBLE, text.visibility)
                        assertTrue("Text line must fit at height=" + height,
                            text.height - text.totalPaddingTop - text.totalPaddingBottom >= text.layout.height)
                        val bounds = Rect(0, 0, text.width, text.height)
                        root.offsetDescendantRectToMyCoords(text, bounds)
                        assertTrue("Text must stay above the bottom padding", bounds.bottom <= height - card.paddingBottom)
                    }
                    assertEquals(12f, item.findViewById<TextView>(R.id.tv_course_name).textSize)
                    assertEquals(10f, item.findViewById<TextView>(R.id.tv_course_position).textSize)
                    assertEquals(10f, item.findViewById<TextView>(R.id.tv_course_time).textSize)
                    assertEquals(9f, item.findViewById<TextView>(R.id.tv_course_teacher).textSize)
                }
            }
        }
    }

    private fun createWidget(): ViewGroup {
        val context = RuntimeEnvironment.getApplication()
        val courses = (0..1).flatMap { offset ->
            (1..2).map { index ->
                WidgetCourseProto(
                    id = offset.toString() + "-" + index, name = "正式课程名称", teacher = "任课教师",
                    position = "教学楼101", start_time = "08:30", end_time = "23:59:59",
                    date = LocalDate.now().plusDays(offset.toLong()).toString()
                )
            }
        }
        return DoubleDaysNativeRenderer.renderAt(
            context,
            WidgetSnapshot(current_week = 4, courses = courses),
            LocalDate.now(),
            LocalTime.NOON
        )
            .apply(context, FrameLayout(context)) as ViewGroup
    }

    private fun measure(root: View, height: Int) {
        root.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 320, height)
    }
}
