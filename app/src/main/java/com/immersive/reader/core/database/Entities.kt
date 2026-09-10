package com.immersive.reader.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.core.model.TimerMode

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val filePath: String,
    val coverPath: String?,
    val lastLocatorJson: String?,
    val progression: Double?,
    val addedAt: Long,
    val lastOpenedAt: Long?,
)

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val timerMode: TimerMode,
    val targetDurationMillis: Long?,
    val exitPolicy: ExitPolicy,
    val startEpochMillis: Long,
    val plannedEndEpochMillis: Long?,
    val actualEndEpochMillis: Long?,
    val accumulatedReadingMillis: Long,
    val startLocatorJson: String?,
    val endLocatorJson: String?,
    val status: SessionStatus,
    val emergencyExit: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "achievement_unlocks")
data class AchievementUnlockEntity(
    @PrimaryKey val id: String,
    val unlockedAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
)
