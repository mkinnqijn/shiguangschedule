package com.xingheyuzhuan.shiguangschedule.data.db

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.xingheyuzhuan.shiguangschedule.data.db.main.ALL_MIGRATIONS
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabaseConstructor
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WIDGET_MIGRATION_3_4
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetDatabaseConstructor
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35], application = android.app.Application::class)
class WidgetShortNameMigrationTest {
    @Test
    fun mainDatabaseUpgradesFromRealVersion6SchemaAndRetainsCoursesAndWeeks() = runBlocking {
        val dir = Files.createTempDirectory("course-migration").toFile()
        val path = File(dir, "main.db").absolutePath
        try {
            val schemaFile = File("../shared/schemas/com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase/6.json")
            val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject.getValue("database").jsonObject
            AndroidSQLiteDriver().open(path).use { connection ->
                schema.getValue("entities").jsonArray.forEach { item ->
                    val entity = item.jsonObject
                    val table = entity.getValue("tableName").jsonPrimitive.content
                    fun sql(text: String) = text.replace("$"+"{TABLE_NAME}", table)
                    connection.execSQL(sql(entity.getValue("createSql").jsonPrimitive.content))
                    entity["indices"]?.jsonArray?.forEach { index ->
                        connection.execSQL(sql(index.jsonObject.getValue("createSql").jsonPrimitive.content))
                    }
                }
                schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
                connection.execSQL("PRAGMA user_version = 6")
                connection.execSQL("INSERT INTO course_tables VALUES ('table', '课表', 0)")
                connection.execSQL("""INSERT INTO courses
                    (id, courseTableId, name, teacher, position, day, startSection, endSection,
                     isCustomTime, customStartTime, customEndTime, colorInt, remark)
                    VALUES ('course', 'table', '正式名称', '教师', '教室', 5, 1, 2, 0, NULL, NULL, 3, '备注')""")
                connection.execSQL("INSERT INTO course_weeks VALUES ('course', 4), ('course', 6)")
            }
            val db = Room.databaseBuilder<MainAppDatabase>(org.robolectric.RuntimeEnvironment.getApplication(), path) { MainAppDatabaseConstructor.initialize() }
                .addMigrations(*ALL_MIGRATIONS).setDriver(AndroidSQLiteDriver()).build()
            try {
                // Opening through Room performs migration AND validates the generated v7 schema.
                val original = db.courseDao().getCoursesWithWeeksByTableId("table").first().single()
                assertNull(original.course.widgetShortName)
                assertEquals("正式名称", original.course.name)
                assertEquals("备注", original.course.remark)
                assertEquals(setOf(4, 6), original.weeks.map { it.weekNumber }.toSet())
                val edited = original.course.copy(widgetShortName = "自定简称")
                db.courseDao().update(edited)
                assertEquals(edited, db.courseDao().getCoursesWithWeeksByTableId("table").first().single().course)
                db.courseDao().update(edited.copy(widgetShortName = null))
                assertNull(db.courseDao().getCoursesWithWeeksByTableId("table").first().single().course.widgetShortName)
            } finally { db.close() }
        } finally { dir.deleteRecursively() }
    }

    @Test
    fun widgetVersion3CacheIsMigratedWithoutDroppingRows() = runBlocking {
        val dir = Files.createTempDirectory("widget-migration").toFile()
        val path = File(dir, "widget.db").absolutePath
        try {
            AndroidSQLiteDriver().open(path).use { connection ->
                connection.execSQL("""CREATE TABLE widget_courses (
                    id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, teacher TEXT NOT NULL,
                    position TEXT NOT NULL, startTime TEXT NOT NULL, endTime TEXT NOT NULL,
                    isSkipped INTEGER NOT NULL, date TEXT NOT NULL, colorInt INTEGER NOT NULL)""")
                connection.execSQL("""CREATE TABLE widget_semester_start_date (
                    id INTEGER NOT NULL PRIMARY KEY, semesterStartDate TEXT,
                    semesterTotalWeeks INTEGER NOT NULL, firstDayOfWeek INTEGER NOT NULL)""")
                connection.execSQL("INSERT INTO widget_courses VALUES ('course', '正式名称', '教师', '教室', '08:30', '10:05', 1, '2026-10-02', 3)")
                connection.execSQL("INSERT INTO widget_semester_start_date VALUES (1, '2026-09-07', 20, 1)")
                connection.execSQL("PRAGMA user_version = 3")
            }
            val db = Room.databaseBuilder<WidgetDatabase>(org.robolectric.RuntimeEnvironment.getApplication(), path) { WidgetDatabaseConstructor.initialize() }
                .addMigrations(WIDGET_MIGRATION_3_4).setDriver(AndroidSQLiteDriver()).build()
            try {
                val old = db.widgetCourseDao().getAllWidgetCourses().first().single()
                assertNull(old.widgetShortName)
                assertEquals(true, old.isSkipped)
                assertEquals("正式名称", old.name)
                assertEquals("2026-09-07", db.widgetAppSettingsDao().getAppSettings().first()?.semesterStartDate)
                val edited = old.copy(widgetShortName = "自定简称")
                db.widgetCourseDao().insertAll(listOf(edited))
                assertEquals(edited, db.widgetCourseDao().getAllWidgetCourses().first().single())
            } finally { db.close() }
        } finally { dir.deleteRecursively() }
    }
}
