package com.example.notable.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notable.data.database.NotableDatabase
import com.example.notable.data.database.NoteDao
import com.example.notable.data.database.SyncStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new columns to existing table
            database.execSQL("ALTER TABLE notes ADD COLUMN serverId INTEGER")
            database.execSQL("ALTER TABLE notes ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE notes ADD COLUMN syncStatus TEXT NOT NULL DEFAULT '${SyncStatus.SYNCED.name}'")
        }
    }

    @Provides
    @Singleton
    fun provideNotableDatabase(@ApplicationContext context: Context): NotableDatabase {
        return Room.databaseBuilder(
            context,
            NotableDatabase::class.java,
            "notable_database"
        )
            .fallbackToDestructiveMigration(false) // This will clear all data on schema changes
            .build()
    }


    @Provides
    fun provideNoteDao(database: NotableDatabase): NoteDao = database.noteDao()
}
