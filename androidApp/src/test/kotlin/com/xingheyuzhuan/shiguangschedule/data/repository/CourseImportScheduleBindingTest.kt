package com.xingheyuzhuan.shiguangschedule.data.repository

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBinding
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabaseConstructor
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTable
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.CourseTableImportModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.ImportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.TimeSlotJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CourseImportScheduleBindingTest {

    @Test
    fun courseOnlyImportKeepsExistingScheduleBinding() = runBlocking {
        withFixture { fixture ->
            fixture.times.savePublicTimeTable(
                TimeTable(id = "public", name = "公共作息", createdAt = 1),
                listOf(TimeSlot("public", 1, "09:00", "09:45"))
            )
            fixture.times.bindCourseTableToTimeSchedule(
                courseTableId = TABLE_ID,
                targetType = CourseTimeBinding.TargetType.SINGLE,
                targetId = "public"
            )

            fixture.converter.importCoursesFromList(
                TABLE_ID,
                listOf(
                    ImportCourseJsonModel(
                        name = "新课程",
                        teacher = "教师",
                        position = "教室",
                        day = 1,
                        startSection = 1,
                        endSection = 1,
                        weeks = listOf(1)
                    )
                )
            )

            assertEquals("public", fixture.times.getBinding(TABLE_ID)?.targetId)
            assertEquals("09:00", fixture.times.getEffectiveTimeSlotsOnce(TABLE_ID, kotlinx.datetime.LocalDate(2026, 10, 5)).single().startTime)
            assertEquals(1, fixture.db.courseDao().getCoursesWithWeeksByTableId(TABLE_ID).first().size)
        }
    }

    @Test
    fun emptyPresetImportDoesNotEraseExistingSlots() = runBlocking {
        withFixture { fixture ->
            fixture.converter.importTimeSlots(TABLE_ID, emptyList())

            val slots = fixture.db.timeSlotDao().getTimeSlotsOnceByTimeTableId(TABLE_ID)
            assertEquals(listOf(1, 2), slots.map { it.number })
        }
    }

    @Test
    fun fullImportWithoutPresetSlotsKeepsExistingScheduleBinding() = runBlocking {
        withFixture { fixture ->
            fixture.times.savePublicTimeTable(
                TimeTable(id = "public", name = "公共作息", createdAt = 1),
                listOf(TimeSlot("public", 1, "09:00", "09:45"))
            )
            fixture.times.bindCourseTableToTimeSchedule(
                courseTableId = TABLE_ID,
                targetType = CourseTimeBinding.TargetType.SINGLE,
                targetId = "public"
            )

            fixture.converter.importCourseTableFromJson(
                TABLE_ID,
                CourseTableImportModel(
                    courses = listOf(
                        ImportCourseJsonModel(
                            name = "新课程",
                            teacher = "教师",
                            position = "教室",
                            day = 1,
                            startSection = 1,
                            endSection = 1,
                            weeks = listOf(1)
                        )
                    ),
                    timeSlots = emptyList()
                )
            )

            assertEquals("public", fixture.times.getBinding(TABLE_ID)?.targetId)
        }
    }

    @Test
    fun nonEmptyPresetImportReplacesExclusiveSlotsAndSelectsIt() = runBlocking {
        withFixture { fixture ->
            fixture.times.savePublicTimeTable(
                TimeTable(id = "public", name = "公共作息", createdAt = 1),
                listOf(TimeSlot("public", 1, "09:00", "09:45"))
            )
            fixture.times.bindCourseTableToTimeSchedule(
                courseTableId = TABLE_ID,
                targetType = CourseTimeBinding.TargetType.SINGLE,
                targetId = "public"
            )

            fixture.converter.importTimeSlots(
                TABLE_ID,
                listOf(TimeSlotJsonModel(number = 1, startTime = "07:30", endTime = "08:15"))
            )

            assertEquals(TABLE_ID, fixture.times.getBinding(TABLE_ID)?.targetId)
            assertEquals(
                "07:30",
                fixture.times.getEffectiveTimeSlotsOnce(TABLE_ID, kotlinx.datetime.LocalDate(2026, 10, 5)).single().startTime
            )
        }
    }

    private suspend fun withFixture(block: suspend (Fixture) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder<MainAppDatabase>(RuntimeEnvironment.getApplication()) {
            MainAppDatabaseConstructor.initialize()
        }.setDriver(AndroidSQLiteDriver()).build()

        try {
            db.courseTableDao().insert(CourseTable(TABLE_ID, "课表", 0))
            val settings = AppSettingsRepository(
                MemoryStore(emptyPreferences()),
                db.courseTableDao(),
                db.courseTableConfigDao()
            )
            val style = StyleSettingsRepository(MemoryStore(ScheduleGridStyleProto()))
            val times = TimeScheduleRepository(
                db,
                db.timeTableDao(),
                db.timeSlotDao(),
                db.courseTimeBindingDao(),
                db.timeTableComboDao()
            )
            times.saveExclusiveTimeTable(
                TimeTable(id = TABLE_ID, name = null, createdAt = 0),
                listOf(
                    TimeSlot(TABLE_ID, 1, "08:00", "08:45"),
                    TimeSlot(TABLE_ID, 2, "08:50", "09:35")
                )
            )
            val converter = CourseConversionRepository(
                db.courseDao(),
                db.courseWeekDao(),
                db.timeSlotDao(),
                settings,
                style,
                times
            )

            block(Fixture(db, times, converter))
        } finally {
            db.close()
        }
    }

    private data class Fixture(
        val db: MainAppDatabase,
        val times: TimeScheduleRepository,
        val converter: CourseConversionRepository
    )

    private class MemoryStore<T>(value: T) : DataStore<T> {
        override val data = MutableStateFlow(value)
        override suspend fun updateData(transform: suspend (t: T) -> T): T =
            transform(data.value).also { data.value = it }
    }

    private companion object {
        const val TABLE_ID = "table"
    }
}
