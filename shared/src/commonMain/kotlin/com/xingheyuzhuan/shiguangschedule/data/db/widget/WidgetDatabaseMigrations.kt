package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Preserve the existing cache when adding optional names.
val WIDGET_MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE widget_courses ADD COLUMN widgetShortName TEXT")
    }
}
