package com.immersive.reader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.core.model.TimerMode

class DatabaseConverters {
    @TypeConverter fun timerModeFromValue(value: String): TimerMode = TimerMode.valueOf(value)
    @TypeConverter fun timerModeToValue(value: TimerMode): String = value.name
    @TypeConverter fun exitPolicyFromValue(value: String): ExitPolicy = ExitPolicy.valueOf(value)
    @TypeConverter fun exitPolicyToValue(value: ExitPolicy): String = value.name
    @TypeConverter fun statusFromValue(value: String): SessionStatus = SessionStatus.valueOf(value)
    @TypeConverter fun statusToValue(value: SessionStatus): String = value.name
}

@Database(
    entities = [BookEntity::class, ReadingSessionEntity::class, AchievementUnlockEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun achievementUnlockDao(): AchievementUnlockDao
}
