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
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeRenderer
import java.time.LocalDate
import java.io.File
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
class CompactLayoutTest {
    @Test
    fun oneAndTwoCoursesFitInHost() = verifyHost()

    @Test
    @Config(qualifiers = "zh-rCN-night-mdpi")
    fun oneAndTwoCoursesFitInDarkHost() = verifyHost()

    @Test
    @Config(qualifiers = "zh-rCN-420dpi")
    fun oneAndTwoCoursesFitAtFractionalDensity() = verifyHost()

    @Test
    fun secondTeacherFitsEvenWithVisibleSpaceBelowTheCourseItem() {
        val host = createHost()
        host.updateAppWidget(CompactNativeRenderer.render(host.context, snapshot(2, "")))
        measure(host, 180, 168)
        val root = host.findViewById<ViewGroup>(R.id.widget_root)
        val courses = root.findViewById<ViewGroup>(R.id.container_courses)
        val teacher = courses.getChildAt(2).findViewById<TextView>(R.id.tv_course_teacher)
        val bounds = Rect(0, 0, teacher.width, teacher.height)
        root.offsetDescendantRectToMyCoords(teacher, bounds)
        assertTrue("The reported symptom includes visible blank space below the clipped line",
            root.height - bounds.bottom >= 16)
        assertTrue("Teacher height=${teacher.height}, required=${teacher.layout.height}, bottom blank=${root.height - bounds.bottom}",
            teacher.height >= teacher.layout.height)
    }

    @Test
    fun oldItemPaddingReproducesClippingWhileFirstCourseAndHeaderStayPut() {
        for (count in 1..2) {
            val host = createHost()
            host.updateAppWidget(CompactNativeRenderer.render(host.context, snapshot(count, "")))
            measure(host, 180, 168)
            val root = host.findViewById<ViewGroup>(R.id.widget_root)
            val courses = root.findViewById<ViewGroup>(R.id.container_courses)
            val firstCourse = courses.getChildAt(0)
            val ids = listOf(R.id.tv_header_title, R.id.tv_course_name, R.id.tv_course_position,
                R.id.tv_course_time, R.id.tv_course_teacher)
            val originalBounds = ids.associateWith { boundsInRoot(root, root.findViewById(it)) }
            if (count == 2) savePreview(root, "compact-after")
            for (i in 0 until count) {
                val item = courses.getChildAt(i * 2)
                assertEquals(4, item.paddingTop)
                assertEquals(2, item.paddingBottom)
                item.setPadding(4, 4, 4, 4) // Restore only the pre-fix item padding.
            }
            measure(host, 180, 168)
            for (id in ids) assertEquals("First course/header must not move", originalBounds[id],
                boundsInRoot(root, root.findViewById(id)))
            assertEquals(14, root.findViewById<View>(R.id.inner_content_card).paddingBottom)
            assertEquals(12f, firstCourse.findViewById<TextView>(R.id.tv_course_name).textSize)
            assertEquals(10f, firstCourse.findViewById<TextView>(R.id.tv_course_position).textSize)
            assertEquals(10f, firstCourse.findViewById<TextView>(R.id.tv_course_time).textSize)
            assertEquals(9f, firstCourse.findViewById<TextView>(R.id.tv_course_teacher).textSize)
            if (count == 2) {
                val teacher = courses.getChildAt(2).findViewById<TextView>(R.id.tv_course_teacher)
                assertEquals(9, teacher.height)
                assertEquals(13, teacher.layout.height)
                assertEquals(18, root.height - boundsInRoot(root, teacher).bottom)
                val ink = Rect()
                teacher.paint.getTextBounds(teacher.text.toString(), 0, teacher.text.length, ink)
                assertTrue("Old bounds clip the actual teacher glyphs, not just line spacing",
                    teacher.baseline + ink.bottom > teacher.height)
                savePreview(root, "compact-before")
            }
        }
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

    private fun verifyHost() {
        for (width in listOf(150, 180, 220)) for (shortName in listOf("", "自定义简称")) {
            val host = createHost()
            // Exercise RemoteViews reapply as well as a newly inflated widget.
            for (count in listOf(1, 2, 1, 2)) {
                host.updateAppWidget(CompactNativeRenderer.render(host.context, snapshot(count, shortName)))
                for (height in listOf(260, 170, 180, 220)) {
                    measure(host, width, height)
                    val root = host.findViewById<ViewGroup>(R.id.widget_root)
                    val container = root.findViewById<ViewGroup>(R.id.container_courses)
                    assertEquals(count * 2 - 1, container.childCount)
                    for (index in 0 until count) {
                        val item = container.getChildAt(index * 2)
                        val course = snapshot(count, shortName).courses[index]
                        val fields = listOf(
                            R.id.tv_course_name to shortName.ifBlank { course.name },
                            R.id.tv_course_position to course.position,
                            R.id.tv_course_time to "${course.start_time}-${course.end_time}",
                            R.id.tv_course_teacher to course.teacher
                        )
                        for ((id, expected) in fields) {
                            val text = item.findViewById<TextView>(id)
                            assertEquals(expected, text.text.toString())
                            assertEquals(View.VISIBLE, text.visibility)
                            assertTrue("$count courses at $width x $height: ${text.text} clipped inside TextView",
                                text.height - text.totalPaddingTop - text.totalPaddingBottom >= text.layout.height)
                            if (id == R.id.tv_course_teacher) {
                                val ink = Rect()
                                text.paint.getTextBounds(expected, 0, expected.length, ink)
                                assertTrue("Teacher glyphs must fit inside their own TextView: $ink",
                                    text.baseline + ink.top >= 0 && text.baseline + ink.bottom <= text.height)
                            }
                            var ancestor = text.parent as ViewGroup
                            while (true) {
                                val bounds = Rect(0, 0, text.width, text.height)
                                ancestor.offsetDescendantRectToMyCoords(text, bounds)
                                assertTrue("${text.text} outside ${ancestor.javaClass.simpleName}: $bounds / ${ancestor.height}",
                                    bounds.top >= ancestor.paddingTop && bounds.bottom <= ancestor.height - ancestor.paddingBottom)
                                if (ancestor === host) break
                                ancestor = ancestor.parent as ViewGroup
                            }
                        }
                    }
                    assertEquals(View.GONE, root.findViewById<View>(R.id.container_status).visibility)
                    assertEquals(View.GONE, root.findViewById<View>(R.id.container_full_status).visibility)
                }
            }
        }
    }

    private fun createHost(): AppWidgetHostView {
        val context = RuntimeEnvironment.getApplication()
        return AppWidgetHostView(context).apply {
            setAppWidget(1, AppWidgetProviderInfo().apply {
                provider = ComponentName(context, CompactNativeProvider::class.java)
            })
        }
    }

    private fun snapshot(count: Int, shortName: String): WidgetSnapshot {
        val date = LocalDate.now().plusDays(1).toString() // Do not depend on test execution time.
        val courses = listOf(
            WidgetCourseProto(id = "1", name = "数学分析（新工科）I", teacher = "郭飞", position = "46楼A108", start_time = "08:30", end_time = "10:05", date = date, widget_short_name = shortName),
            WidgetCourseProto(id = "2", name = "习近平新时代中国特色社会主义思想概论", teacher = "柳兰芳", position = "46楼A405", start_time = "10:25", end_time = "12:00", date = date, widget_short_name = shortName)
        )
        return WidgetSnapshot(current_week = 4, courses = courses.take(count))
    }

    private fun measure(root: View, widthDp: Int, heightDp: Int) {
        val density = root.resources.displayMetrics.density
        val width = (widthDp * density).roundToInt()
        val height = (heightDp * density).roundToInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
    }
}
