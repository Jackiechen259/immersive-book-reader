package com.immersive.reader.di

import android.content.Context
import androidx.room.Room
import com.immersive.reader.core.database.AchievementUnlockDao
import com.immersive.reader.core.database.AppDatabase
import com.immersive.reader.core.database.BookDao
import com.immersive.reader.core.database.ReadingSessionDao
import com.immersive.reader.focus.AndroidFocusController
import com.immersive.reader.focus.FocusController
import com.immersive.reader.focus.ImmersiveController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "immersive_reader.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideBookDao(database: AppDatabase): BookDao = database.bookDao()
    @Provides fun provideReadingSessionDao(database: AppDatabase): ReadingSessionDao = database.readingSessionDao()
    @Provides fun provideAchievementUnlockDao(database: AppDatabase): AchievementUnlockDao = database.achievementUnlockDao()
    @Provides fun provideFilesDir(@ApplicationContext context: Context): File = context.filesDir

    @Provides
    @Singleton
    fun provideImmersiveController(): ImmersiveController = ImmersiveController()

    @Provides
    @Singleton
    fun provideFocusController(controller: AndroidFocusController): FocusController = controller
}
