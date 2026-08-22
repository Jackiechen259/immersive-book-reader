package com.immersive.reader.core.model

enum class TimerMode {
    OPEN_ENDED,
    COUNTDOWN,
}

enum class ExitPolicy {
    CONFIRM,
    HOLD_TO_EXIT,
    TIME_LOCKED,
}

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    USER_ENDED,
    EMERGENCY_EXIT,
    INTERRUPTED,
}

enum class ThemeMode {
    LIGHT,
    SEPIA,
    DARK,
}

enum class ReadingMode {
    PAGINATED,
    SCROLLING,
}

data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val filePath: String,
    val coverPath: String?,
    val lastLocatorJson: String?,
    val progression: Double?,
    val addedAt: Long,
    val lastOpenedAt: Long?,
)

data class ReadingSession(
    val id: String,
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
