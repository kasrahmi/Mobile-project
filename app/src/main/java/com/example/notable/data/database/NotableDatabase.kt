package com.example.notable.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NotableDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
