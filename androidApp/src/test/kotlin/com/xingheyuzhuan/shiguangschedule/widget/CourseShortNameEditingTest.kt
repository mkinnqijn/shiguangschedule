package com.xingheyuzhuan.shiguangschedule.widget

import android.app.Application
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.xingheyuzhuan.shiguangschedule.data.db.main.*
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.data.repository.*
import com.xingheyuzhuan.shiguangschedule.ui.settings.course.AddEditCourseViewModel
import com.xingheyuzhuan.shiguangschedule.ui.settings.course.UiEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CourseShortNameEditingTest {
    @Test
    fun importEditSaveClearAndExportKeepFullNameAndBothSessions() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder<MainAppDatabase>(RuntimeEnvironment.getApplication()) {
            MainAppDatabaseConstructor.initialize()
        }.setDriver(AndroidSQLiteDriver()).build()
        val viewModels = ViewModelStore()
        try {
            db.courseTableDao().insert(CourseTable("table", "课表", 0))
            val settings = AppSettingsRepository(MemoryStore(emptyPreferences()), db.courseTableDao(), db.courseTableConfigDao())
            val style = StyleSettingsRepository(MemoryStore(ScheduleGridStyleProto()))
            val times = TimeScheduleRepository(db, db.timeTableDao(), db.timeSlotDao(), db.courseTimeBindingDao(), db.timeTableComboDao())
            val courses = CourseTableRepository(db.courseTableDao(), db.courseDao(), db.courseWeekDao(), times, settings)
            val converter = CourseConversionRepository(db.courseDao(), db.courseWeekDao(), db.timeSlotDao(), settings, style, times)
            val json = """{"courses":[
                {"id":"a","name":"正式名称","teacher":"教师甲","position":"教室甲","day":1,"weeks":[4,6],"startSection":1,"endSection":2,"widgetShortName":"旧简称"},
                {"id":"b","name":"正式名称","teacher":"教师乙","position":"教室乙","day":5,"weeks":[4,6],"startSection":1,"endSection":2,"widgetShortName":"旧简称"}
            ],"config":{"semesterStartDate":"2026-09-07"}}"""
            converter.importCourseTableFromJson("table", CourseImportExport.json.decodeFromString(json))
            val vm = AddEditCourseViewModel(courses, times, settings, style)
            viewModels.put("edit", vm)
            vm.initWithId("b")
            pumpUntil { vm.uiState.value.isDataLoaded }
            assertEquals("旧简称", vm.uiState.value.widgetShortName)
            assertFalse(vm.hasUnsavedChanges())
            vm.onWidgetShortNameChange("  新简称  ")
            assertTrue(vm.hasUnsavedChanges())
            val saved = async { vm.uiEvent.first() }
            vm.onSave()
            pumpUntil { saved.isCompleted }
            assertEquals(UiEvent.SaveSuccess, saved.await())
            val stored = db.courseDao().getCoursesWithWeeksByTableId("table").first()
            assertEquals(2, stored.size)
            stored.forEach {
                assertEquals("新简称", it.course.widgetShortName)
                assertEquals("正式名称", it.course.name)
                assertEquals(setOf(4, 6), it.weeks.map { week -> week.weekNumber }.toSet())
            }
            val exported = requireNotNull(converter.exportCourseTableToJson("table"))
            val reimported = CourseImportExport.json.decodeFromString<CourseImportExport.CourseTableImportModel>(
                CourseImportExport.json.encodeToString(exported)
            )
            assertEquals(listOf("新简称", "新简称"), reimported.courses.map { it.widgetShortName })
            assertEquals(listOf("正式名称", "正式名称"), reimported.courses.map { it.name })
            // Reopen through the actual editor loading path, then clear the optional field.
            val reopened = AddEditCourseViewModel(courses, times, settings, style)
            viewModels.put("reopened", reopened)
            reopened.initWithId("a")
            pumpUntil { reopened.uiState.value.isDataLoaded }
            assertEquals("新简称", reopened.uiState.value.widgetShortName)
            reopened.onWidgetShortNameChange(" ")
            val cleared = async { reopened.uiEvent.first() }
            reopened.onSave()
            pumpUntil { cleared.isCompleted }
            assertEquals(UiEvent.SaveSuccess, cleared.await())
            db.courseDao().getCoursesWithWeeksByTableId("table").first().forEach {
                assertNull(it.course.widgetShortName)
                assertEquals("正式名称", it.course.name)
            }
        } finally {
            viewModels.clear()
            db.close()
        }
    }

    private suspend fun pumpUntil(done: () -> Boolean) = withTimeout(10_000) {
        while (!done()) {
            shadowOf(Looper.getMainLooper()).idle()
            delay(10)
        }
    }

    private class MemoryStore<T>(value: T) : DataStore<T> {
        override val data = MutableStateFlow(value)
        override suspend fun updateData(transform: suspend (t: T) -> T): T =
            transform(data.value).also { data.value = it }
    }
}
