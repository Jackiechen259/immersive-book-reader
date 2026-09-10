package com.immersive.reader.achievement

import com.immersive.reader.R
import java.util.concurrent.TimeUnit

object AchievementCatalog {
    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = AchievementIds.FIRST_SESSION,
            category = AchievementCategory.HABIT,
            titleRes = R.string.achievement_first_session_title,
            descriptionRes = R.string.achievement_first_session_body,
            target = 1L,
        ),
        AchievementDefinition(
            id = AchievementIds.HOURS_1,
            category = AchievementCategory.HABIT,
            titleRes = R.string.achievement_hours_1_title,
            descriptionRes = R.string.achievement_hours_1_body,
            target = TimeUnit.HOURS.toMillis(1),
            unit = AchievementProgressUnit.MILLIS,
        ),
        AchievementDefinition(
            id = AchievementIds.HOURS_10,
            category = AchievementCategory.HABIT,
            titleRes = R.string.achievement_hours_10_title,
            descriptionRes = R.string.achievement_hours_10_body,
            target = TimeUnit.HOURS.toMillis(10),
            unit = AchievementProgressUnit.MILLIS,
        ),
        AchievementDefinition(
            id = AchievementIds.DAYS_7,
            category = AchievementCategory.HABIT,
            titleRes = R.string.achievement_days_7_title,
            descriptionRes = R.string.achievement_days_7_body,
            target = 7L,
        ),
        AchievementDefinition(
            id = AchievementIds.FIRST_IMPORT,
            category = AchievementCategory.BOOK,
            titleRes = R.string.achievement_first_import_title,
            descriptionRes = R.string.achievement_first_import_body,
            target = 1L,
        ),
        AchievementDefinition(
            id = AchievementIds.FIRST_FINISH,
            category = AchievementCategory.BOOK,
            titleRes = R.string.achievement_first_finish_title,
            descriptionRes = R.string.achievement_first_finish_body,
            target = 1L,
        ),
        AchievementDefinition(
            id = AchievementIds.BOOKS_5,
            category = AchievementCategory.BOOK,
            titleRes = R.string.achievement_books_5_title,
            descriptionRes = R.string.achievement_books_5_body,
            target = 5L,
        ),
        AchievementDefinition(
            id = AchievementIds.COUNTDOWN_COMPLETE,
            category = AchievementCategory.FOCUS,
            titleRes = R.string.achievement_countdown_complete_title,
            descriptionRes = R.string.achievement_countdown_complete_body,
            target = 1L,
        ),
        AchievementDefinition(
            id = AchievementIds.HOLD_TO_EXIT,
            category = AchievementCategory.FOCUS,
            titleRes = R.string.achievement_hold_to_exit_title,
            descriptionRes = R.string.achievement_hold_to_exit_body,
            target = 1L,
        ),
        AchievementDefinition(
            id = AchievementIds.DEEP_FOCUS,
            category = AchievementCategory.FOCUS,
            titleRes = R.string.achievement_deep_focus_title,
            descriptionRes = R.string.achievement_deep_focus_body,
            target = 1L,
        ),
        AchievementDefinition(
            id = AchievementIds.FIVE_COMPLETED,
            category = AchievementCategory.FOCUS,
            titleRes = R.string.achievement_five_completed_title,
            descriptionRes = R.string.achievement_five_completed_body,
            target = 5L,
        ),
    )
}
