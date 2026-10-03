package com.xingheyuzhuan.shiguangschedule.widget

import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.CourseTableImportModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.ExportCourseJsonModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetCourseDisplayNameTest {
    @Test
    fun narrowNameIsUserSuppliedAndFullNameIsPreserved() {
        val course = WidgetCourseProto(name = "数学分析（新工科）I", widget_short_name = "  自定简称（原样）  ")
        assertEquals("自定简称（原样）", course.narrowDisplayName())
        assertEquals("数学分析（新工科）I", course.name)
        assertEquals("数学分析(新工科)I", course.copy(widget_short_name = "").narrowDisplayName())
        assertEquals("数学分析(新工科)I", course.copy(widget_short_name = " \t\n").narrowDisplayName())
    }

    @Test
    fun oldJsonWithoutShortNameStillImports() {
        val oldJson = """{"courses":[{"name":"正式名称","teacher":"教师","position":"教室","day":5,"weeks":[4]}]}"""
        val model = CourseImportExport.json.decodeFromString<CourseTableImportModel>(oldJson)
        assertNull(model.courses.single().widgetShortName)
        assertEquals("正式名称", model.courses.single().name)
    }

    @Test
    fun jsonExportCanBeImportedWithoutLosingShortNameOrWeeks() {
        val exported = CourseImportExport.CourseTableExportModel(
            courses = listOf(exportCourse("用户简称")),
            timeSlots = emptyList(),
            config = CourseImportExport.CourseConfigJsonModel()
        )
        val json = CourseImportExport.json.encodeToString(exported)
        val imported = CourseImportExport.json.decodeFromString<CourseTableImportModel>(json)
        assertEquals("用户简称", imported.courses.single().widgetShortName)
        assertEquals("正式名称", imported.courses.single().name)
        assertEquals(listOf(4, 6), imported.courses.single().weeks)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun cborBackupRoundTripIncludesOptionalShortName() {
        val original = exportCourse("用户简称")
        val encoded = CourseImportExport.cbor.encodeToByteArray(original)
        assertEquals(original, CourseImportExport.cbor.decodeFromByteArray<ExportCourseJsonModel>(encoded))
        // encodeDefaults=false omits the null optional field, as in older backups.
        val legacy = CourseImportExport.cbor.encodeToByteArray(exportCourse(null))
        assertNull(CourseImportExport.cbor.decodeFromByteArray<ExportCourseJsonModel>(legacy).widgetShortName)
    }

    @Test
    fun protobufRoundTripAndLegacySnapshotUseSafeFallback() {
        val oldCourse = WidgetCourseProto(name = "正式名称")
        val decodedOld = WidgetCourseProto.ADAPTER.decode(WidgetCourseProto.ADAPTER.encode(oldCourse))
        assertEquals("正式名称", decodedOld.narrowDisplayName())
        val course = oldCourse.copy(widget_short_name = "自定简称")
        assertEquals(course, WidgetCourseProto.ADAPTER.decode(WidgetCourseProto.ADAPTER.encode(course)))
    }

    private fun exportCourse(shortName: String?) = ExportCourseJsonModel(
        id = "course", name = "正式名称", teacher = "教师", position = "教室",
        day = 5, startSection = 1, endSection = 2, color = 0,
        weeks = listOf(4, 6), remark = null, widgetShortName = shortName
    )
}
