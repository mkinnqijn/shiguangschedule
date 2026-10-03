package com.xingheyuzhuan.shiguangschedule.widget

import android.app.Application
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.ui.formatCourseNameForDisplay
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeRenderer
import java.io.File
import java.time.LocalDate
import kotlin.math.roundToInt
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
class ListVerticalLayoutTest {
    @Test
    fun oneAndTwoCoursesFitInWidgetHost() = verifyOneAndTwoCourses()

    @Test
    @Config(qualifiers = "zh-rCN-night-mdpi")
    fun oneAndTwoCoursesFitInDarkWidgetHost() = verifyOneAndTwoCourses()

    @Test
    @Config(qualifiers = "zh-rCN-420dpi")
    fun oneAndTwoCoursesFitAtFractionalDensity() = verifyOneAndTwoCourses()

    @Test
    fun oldItemSpacingReproducesClippedSecondTeacherWithBlankSpaceBelow() {
        val host = createHost()
        host.updateAppWidget(ListVerticalNativeRenderer.render(host.context, snapshot(2)))
        measure(host, 360, 176)
        val root = host.findViewById<ViewGroup>(R.id.widget_root)
        val courses = root.findViewById<ViewGroup>(R.id.container_courses)
        val fixedTeacher = courses.getChildAt(2).findViewById<TextView>(R.id.tv_course_teacher)
        assertTrue(fixedTeacher.includeFontPadding)
        assertTrue(fixedTeacher.height - fixedTeacher.totalPaddingTop - fixedTeacher.totalPaddingBottom >= fixedTeacher.layout.height)
        assertTrue(root.height - boundsInRoot(root, fixedTeacher).bottom >= dp(root, 12))
        savePreview(root, "list-vertical-after")

        for (index in listOf(0, 2)) {
            courses.getChildAt(index).setPadding(4, 8, 4, 8)
        }
        val divider = courses.getChildAt(1)
        divider.layoutParams = (divider.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin = 2
            bottomMargin = 2
        }
        measure(host, 360, 176)
        val oldTeacher = courses.getChildAt(2).findViewById<TextView>(R.id.tv_course_teacher)
        assertTrue("The old spacing must reproduce the clipped teacher line",
            oldTeacher.height < oldTeacher.layout.height)
        assertTrue("The crop occurs while outer blank space remains visible",
            root.height - boundsInRoot(root, oldTeacher).bottom >= 16)
        savePreview(root, "list-vertical-before")
    }

    private fun verifyOneAndTwoCourses() {
        for (count in 1..2) {
            val host = createHost()
            host.updateAppWidget(ListVerticalNativeRenderer.render(host.context, snapshot(count)))
            measure(host, 360, 176)
            val root = host.findViewById<ViewGroup>(R.id.widget_root)
            val card = root.findViewById<View>(R.id.inner_content_card)
            val header = root.findViewById<TextView>(R.id.tv_header_title)
            val courses = root.findViewById<ViewGroup>(R.id.container_courses)

            assertEquals(count * 2 - 1, courses.childCount)
            assertEquals(dp(root, 12), card.paddingTop)
            assertEquals(dp(root, 12), card.paddingBottom)

            for (index in 0 until count) {
                val item = courses.getChildAt(index * 2)
                val course = snapshot(count).courses[index]
                assertEquals(dp(root, 4), item.paddingTop)
                assertEquals(dp(root, 4), item.paddingBottom)
                assertEquals(formatCourseNameForDisplay(course.name), item.findViewById<TextView>(R.id.tv_course_name).text.toString())
                assertNotEquals("A wide list widget must keep the formal course name", course.widget_short_name,
                    item.findViewById<TextView>(R.id.tv_course_name).text.toString())

                val fields = mutableListOf(
                    R.id.tv_course_name to formatCourseNameForDisplay(course.name),
                    R.id.tv_course_start_time to course.start_time,
                    R.id.tv_course_end_time to course.end_time,
                    R.id.tv_course_position to course.position
                )
                if (course.teacher.isNotBlank()) fields += R.id.tv_course_teacher to course.teacher
                for ((id, expected) in fields) {
                    val text = item.findViewById<TextView>(id)
                    assertEquals(expected, text.text.toString())
                    assertEquals(View.VISIBLE, text.visibility)
                    assertTrue("$count courses: \${text.text} is clipped inside its TextView",
                        text.height - text.totalPaddingTop - text.totalPaddingBottom >= text.layout.height)
                    assertInsideEveryParent(host, text)
                }
                if (course.teacher.isBlank()) {
                    assertEquals(View.GONE, item.findViewById<View>(R.id.tv_course_teacher).visibility)
                }
                assertEquals(dp(root, 13).toFloat(), item.findViewById<TextView>(R.id.tv_course_name).textSize)
                assertEquals(dp(root, 10).toFloat(), item.findViewById<TextView>(R.id.tv_course_position).textSize)
                assertEquals(dp(root, 9).toFloat(), item.findViewById<TextView>(R.id.tv_course_teacher).textSize)
            }

            if (count == 2) {
                val divider = courses.getChildAt(1)
                val margins = divider.layoutParams as ViewGroup.MarginLayoutParams
                assertEquals(dp(root, 1), divider.height)
                assertEquals(0, margins.topMargin)
                assertEquals(0, margins.bottomMargin)
            }

            val firstName = courses.getChildAt(0).findViewById<TextView>(R.id.tv_course_name)
            assertTrue("A single course must retain a natural gap below the header",
                boundsInRoot(root, firstName).top - boundsInRoot(root, header).bottom >= dp(root, 8))
        }
    }

    private fun assertInsideEveryParent(host: ViewGroup, text: TextView) {
        var ancestor = text.parent as ViewGroup
        while (true) {
            val bounds = Rect(0, 0, text.width, text.height)
            ancestor.offsetDescendantRectToMyCoords(text, bounds)
            assertTrue("\${text.text} exceeds \${ancestor.javaClass.simpleName}: $bounds / \${ancestor.height}",
                bounds.top >= ancestor.paddingTop && bounds.bottom <= ancestor.height - ancestor.paddingBottom)
            if (ancestor === host) return
            ancestor = ancestor.parent as ViewGroup
        }
    }

    private fun createHost(): AppWidgetHostView {
        val context = RuntimeEnvironment.getApplication()
        return AppWidgetHostView(context).apply {
            setAppWidget(1, AppWidgetProviderInfo().apply {
                provider = ComponentName(context, ListVerticalNativeProvider::class.java)
            })
        }
    }

    private fun snapshot(count: Int): WidgetSnapshot {
        val date = LocalDate.now().plusDays(1).toString()
        return WidgetSnapshot(current_week = 4, courses = listOf(
            WidgetCourseProto(id = "1", name = "习近平新时代中国特色社会主义思想概论", widget_short_name = "习概",
                teacher = "教师名字", position = "45教B108", start_time = "13:30", end_time = "15:05", date = date),
            WidgetCourseProto(id = "2", name = "数学分析（新工科）I", widget_short_name = "数分",
                teacher = "教师名字", position = "46楼A308", start_time = "15:25", end_time = "17:00", date = date)
        ).take(count))
    }

    private fun boundsInRoot(root: ViewGroup, view: View): Rect =
        Rect(0, 0, view.width, view.height).also { root.offsetDescendantRectToMyCoords(view, it) }

    private fun savePreview(root: View, name: String) {
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val file = File("build/reports/widget-layout/$name.png")
        checkNotNull(file.parentFile).mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun measure(root: View, widthDp: Int, heightDp: Int) {
        val width = dp(root, widthDp)
        val height = dp(root, heightDp)
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
    }

    private fun dp(view: View, value: Int) = (value * view.resources.displayMetrics.density).roundToInt()
}
