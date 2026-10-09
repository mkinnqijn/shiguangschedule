package com.xingheyuzhuan.shiguangschedule.widget

import android.app.Application
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
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
    fun oneCourseKeepsOneFixedHalfHeightSlotInEachColumn() {
        val today = LocalDate.now()
        val courses = listOf(
            WidgetCourseProto(id = "today", name = "今日课程", teacher = "教师", position = "地点", start_time = "08:30", end_time = "23:59", date = today.toString()),
            WidgetCourseProto(id = "tomorrow", name = "明日课程", teacher = "教师", position = "地点", start_time = "08:30", end_time = "23:59", date = today.plusDays(1).toString())
        )
        val root = render(courses, today)
        measure(root, 180)

        for ((firstId, secondId, dividerId) in listOf(
            Triple(R.id.container_today_course_1, R.id.container_today_course_2, R.id.divider_today_courses),
            Triple(R.id.container_tomorrow_course_1, R.id.container_tomorrow_course_2, R.id.divider_tomorrow_courses)
        )) {
            val first = root.findViewById<ViewGroup>(firstId)
            val second = root.findViewById<ViewGroup>(secondId)
            assertTrue(kotlin.math.abs(first.height - second.height) <= 1)
            assertEquals(View.VISIBLE, first.visibility)
            assertEquals(View.INVISIBLE, second.visibility)
            assertEquals(1, first.childCount)
            assertEquals(0, second.childCount)
            assertEquals(View.GONE, root.findViewById<View>(dividerId).visibility)
        }
    }

    @Test
    fun eachColumnShowsAtMostTwoCoursesAndKeepsTheRealCount() {
        val today = LocalDate.now()
        val courses = (0..1).flatMap { offset ->
            (1..3).map { index ->
                WidgetCourseProto(
                    id = "$offset-$index", name = "课程$index", teacher = "", position = "",
                    start_time = "18:0$index", end_time = "23:59", date = today.plusDays(offset.toLong()).toString()
                )
            }
        }
        val root = render(courses, today)
        measure(root, 180)

        for ((containerId, firstId, secondId, footerId) in listOf(
            listOf(R.id.container_today, R.id.container_today_course_1, R.id.container_today_course_2, R.id.tv_today_footer),
            listOf(R.id.container_tomorrow, R.id.container_tomorrow_course_1, R.id.container_tomorrow_course_2, R.id.tv_tomorrow_footer)
        )) {
            val container = root.findViewById<ViewGroup>(containerId)
            assertEquals(3, container.childCount)
            assertEquals(1, root.findViewById<ViewGroup>(firstId).childCount)
            assertEquals(1, root.findViewById<ViewGroup>(secondId).childCount)
            assertFalse(root.findViewById<TextView>(footerId).text.toString().contains("2"))
            assertTrue(root.findViewById<TextView>(footerId).text.toString().contains("3"))
            assertFalse(containsText(container, "课程3"))
        }
    }

    @Test
    fun tomorrowAlwaysShowsTheEarliestTwoCoursesEvenWhenSnapshotOrderIsScrambled() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1).toString()
        val courses = listOf(
            WidgetCourseProto(id = "bio", name = "生物技术与海洋探秘", teacher = "教师", position = "55楼A区308", start_time = "15:25", end_time = "17:00", date = tomorrow),
            WidgetCourseProto(id = "late", name = "晚间课程", teacher = "教师", position = "地点", start_time = "18:30", end_time = "20:05", date = tomorrow),
            WidgetCourseProto(id = "design", name = "设计与建造", teacher = "", position = "待定", start_time = "10:25", end_time = "12:00", date = tomorrow),
            WidgetCourseProto(id = "math", name = "数学分析", teacher = "郭飞", position = "46楼A108", start_time = "08:30", end_time = "10:05", date = tomorrow)
        )

        val root = render(courses, today)
        val first = slotItem(root, R.id.container_tomorrow_course_1)
        val second = slotItem(root, R.id.container_tomorrow_course_2)
        val column = root.findViewById<ViewGroup>(R.id.container_tomorrow)

        assertEquals("数学分析", first.findViewById<TextView>(R.id.tv_course_name).text.toString())
        assertEquals("设计与建造", second.findViewById<TextView>(R.id.tv_course_name).text.toString())
        assertFalse(containsText(column, "生物技术与海洋探秘"))
        assertFalse(containsText(column, "晚间课程"))
        assertTrue(root.findViewById<TextView>(R.id.tv_tomorrow_footer).text.toString().contains("4"))
    }

    @Test
    fun missingTeacherAndPositionInEitherSlotKeepTheSameFixedSlotHeights() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1).toString()
        val variants = listOf(
            Triple("first teacher", listOf("", "教师2"), listOf("地点1", "地点2")),
            Triple("second teacher", listOf("教师1", ""), listOf("地点1", "地点2")),
            Triple("first position", listOf("教师1", "教师2"), listOf("", "地点2")),
            Triple("second position", listOf("教师1", "教师2"), listOf("地点1", ""))
        )

        for ((label, teachers, positions) in variants) {
            val courses = (0..1).map { index ->
                WidgetCourseProto(
                    id = "$label-$index",
                    name = "课程${index + 1}",
                    teacher = teachers[index],
                    position = positions[index],
                    start_time = if (index == 0) "08:30" else "10:25",
                    end_time = if (index == 0) "10:05" else "12:00",
                    date = tomorrow
                )
            }
            val root = render(courses, today)
            measure(root, 180)
            val firstSlot = root.findViewById<View>(R.id.container_tomorrow_course_1)
            val secondSlot = root.findViewById<View>(R.id.container_tomorrow_course_2)
            assertTrue("$label must not change slot heights", kotlin.math.abs(firstSlot.height - secondSlot.height) <= 1)
        }
    }

    @Test
    fun twoDifferentCourseColorsAreBoundInLightLayout() =
        verifyTwoDifferentCourseColors(R.id.course_indicator, useDarkColors = false)

    @Test
    @Config(qualifiers = "zh-rCN-night-mdpi")
    fun twoDifferentCourseColorsAreBoundInNightLayout() =
        verifyTwoDifferentCourseColors(R.id.course_indicator_dark, useDarkColors = true)

    @Test
    fun missingFieldsUseTheExpectedInternalRowsOnTodayAndTomorrow() {
        val today = LocalDate.now()
        val courses = listOf(
            WidgetCourseProto(id = "a", name = "完整", teacher = "教师A", position = "地点A", start_time = "18:30", end_time = "23:59", date = today.toString()),
            WidgetCourseProto(id = "b", name = "缺教师", teacher = "", position = "地点B", start_time = "20:10", end_time = "23:59", date = today.toString()),
            WidgetCourseProto(id = "c", name = "缺地点", teacher = "教师C", position = "", start_time = "08:30", end_time = "10:05", date = today.plusDays(1).toString()),
            WidgetCourseProto(id = "d", name = "仅名称", teacher = "", position = "", start_time = "10:25", end_time = "12:00", date = today.plusDays(1).toString())
        )
        val root = render(courses, today)
        measure(root, 180)

        val complete = slotItem(root, R.id.container_today_course_1)
        val missingTeacher = slotItem(root, R.id.container_today_course_2)
        val missingPosition = slotItem(root, R.id.container_tomorrow_course_1)
        val nameOnly = slotItem(root, R.id.container_tomorrow_course_2)
        assertEquals(View.VISIBLE, complete.findViewById<View>(R.id.tv_course_teacher).visibility)
        assertEquals(View.GONE, missingTeacher.findViewById<View>(R.id.tv_course_teacher).visibility)
        assertEquals("教师C", missingPosition.findViewById<TextView>(R.id.tv_course_position).text.toString())
        assertEquals(View.GONE, missingPosition.findViewById<View>(R.id.tv_course_teacher).visibility)
        assertEquals(View.GONE, nameOnly.findViewById<View>(R.id.container_course_info_row).visibility)
        assertEquals(View.VISIBLE, nameOnly.findViewById<View>(R.id.tv_course_time_inline).visibility)
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
                    val slot = column.getChildAt(index) as ViewGroup
                    assertEquals(1, slot.childCount)
                    val item = slot.getChildAt(0)
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
        return render(courses, LocalDate.now())
    }

    private fun verifyTwoDifferentCourseColors(indicatorId: Int, useDarkColors: Boolean) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1).toString()
        val lightColors = listOf(0xff42a5f5.toInt(), 0xffffc107.toInt())
        val darkColors = listOf(0xff1565c0.toInt(), 0xffff8f00.toInt())
        val style = ScheduleGridStyleProto(
            course_color_maps = lightColors.indices.map { index ->
                DualColorProto(
                    light_color = lightColors[index].toLong(),
                    dark_color = darkColors[index].toLong()
                )
            }
        )
        val courses = listOf(
            WidgetCourseProto(id = "blue", name = "蓝色课程", position = "地点", start_time = "08:30", end_time = "10:05", color_int = 0, date = tomorrow),
            WidgetCourseProto(id = "yellow", name = "黄色课程", position = "地点", start_time = "10:25", end_time = "12:00", color_int = 1, date = tomorrow)
        )
        val root = render(courses, today, style)
        val expected = if (useDarkColors) darkColors else lightColors

        listOf(R.id.container_tomorrow_course_1, R.id.container_tomorrow_course_2)
            .forEachIndexed { index, slotId ->
                val indicator = slotItem(root, slotId).findViewById<ImageView>(indicatorId)
                val filter = indicator.colorFilter as PorterDuffColorFilter
                assertEquals(expected[index], shadowOf(filter).color)
            }
    }

    private fun render(
        courses: List<WidgetCourseProto>,
        today: LocalDate,
        style: ScheduleGridStyleProto? = null
    ): ViewGroup {
        val context = RuntimeEnvironment.getApplication()
        return DoubleDaysNativeRenderer.renderAt(
            context,
            WidgetSnapshot(current_week = 4, courses = courses, style = style),
            today,
            LocalTime.NOON
        )
            .apply(context, FrameLayout(context)) as ViewGroup
    }

    private fun slotItem(root: ViewGroup, slotId: Int): ViewGroup {
        val slot = root.findViewById<ViewGroup>(slotId)
        assertEquals(1, slot.childCount)
        return slot.getChildAt(0) as ViewGroup
    }

    private fun containsText(view: View, expected: String): Boolean {
        if (view is TextView && view.text.toString() == expected) return true
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                if (containsText(view.getChildAt(index), expected)) return true
            }
        }
        return false
    }

    private fun measure(root: View, height: Int) {
        root.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 320, height)
    }
}
