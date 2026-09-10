package com.immersive.reader.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.immersive.reader.R
import com.immersive.reader.core.model.AppLanguage
import com.immersive.reader.core.model.ReadingMode
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.core.model.ThemeMode
import java.util.concurrent.TimeUnit

@Composable
fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.SEPIA -> R.string.theme_sepia
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
fun ReadingMode.label(): String = stringResource(
    when (this) {
        ReadingMode.PAGINATED -> R.string.reading_mode_pages
        ReadingMode.SCROLLING -> R.string.reading_mode_scroll
    },
)

@Composable
fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.ENGLISH -> R.string.language_english
        AppLanguage.SIMPLIFIED_CHINESE -> R.string.language_simplified_chinese
    },
)

@Composable
fun SessionStatus.title(): String = stringResource(
    when (this) {
        SessionStatus.COMPLETED -> R.string.session_complete
        SessionStatus.USER_ENDED -> R.string.session_saved
        SessionStatus.EMERGENCY_EXIT -> R.string.session_interrupted
        SessionStatus.INTERRUPTED -> R.string.session_interrupted
        SessionStatus.ACTIVE -> R.string.reading_in_progress
    },
)

@Composable
fun localizedReadingDuration(milliseconds: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds).coerceAtLeast(0L)
    if (totalSeconds == 0L) return stringResource(R.string.duration_zero_minutes)
    if (totalSeconds < 60L) return stringResource(R.string.duration_seconds, totalSeconds)
    val minutes = totalSeconds / 60L
    if (minutes < 60L) return stringResource(R.string.duration_minutes, minutes)
    val hours = minutes / 60L
    val remain = minutes % 60L
    return if (remain == 0L) {
        stringResource(R.string.duration_hours, hours)
    } else {
        stringResource(R.string.duration_hours_minutes, hours, remain)
    }
}
