package com.immersive.reader.achievement

import androidx.annotation.StringRes

object AchievementIds {
    const val FIRST_SESSION = "first_session"
    const val HOURS_1 = "hours_1"
    const val HOURS_10 = "hours_10"
    const val DAYS_7 = "days_7"
    const val FIRST_IMPORT = "first_import"
    const val FIRST_FINISH = "first_finish"
    const val BOOKS_5 = "books_5"
    const val COUNTDOWN_COMPLETE = "countdown_complete"
    const val HOLD_TO_EXIT = "hold_to_exit"
    const val DEEP_FOCUS = "deep_focus"
    const val FIVE_COMPLETED = "five_completed"
}

enum class AchievementCategory {
    HABIT,
    BOOK,
    FOCUS,
}

enum class AchievementProgressUnit {
    COUNT,
    MILLIS,
}

data class AchievementDefinition(
    val id: String,
    val category: AchievementCategory,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val target: Long,
    val unit: AchievementProgressUnit = AchievementProgressUnit.COUNT,
)

data class AchievementProgress(
    val definition: AchievementDefinition,
    val current: Long,
    val unlockedAtEpochMillis: Long?,
)

data class AchievementUnlock(
    val id: String,
    val unlockedAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
)
