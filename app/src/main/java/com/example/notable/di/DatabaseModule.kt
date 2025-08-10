package com.example.notable.di

import android.content.Context
import androidx.room.Room
import com.example.notable.data.database.NoteDao
import com.example.notable.data.database.NotableDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNotableDatabase(
        @ApplicationContext context: Context
    ): NotableDatabase {
        return Room.databaseBuilder(
            context,
            NotableDatabase::class.java,
            "notable_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: NotableDatabase): NoteDao {
        return database.noteDao()
    }
}
