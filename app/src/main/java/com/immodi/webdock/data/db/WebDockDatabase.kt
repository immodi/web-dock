package com.immodi.webdock.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.immodi.webdock.domain.WebApp

@Database(
    entities = [WebApp::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class WebDockDatabase : RoomDatabase() {

    abstract fun webAppDao(): WebAppDao

    companion object {
        fun build(context: Context): WebDockDatabase =
            Room.databaseBuilder(context, WebDockDatabase::class.java, "webdock.db").build()
    }
}
