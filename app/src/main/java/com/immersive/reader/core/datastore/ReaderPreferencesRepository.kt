package com.immersive.reader.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.immersive.reader.core.model.ReadingMode
import com.immersive.reader.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerDataStore by preferencesDataStore(name = "reader_preferences")

data class ReaderPreferences(
    val theme: ThemeMode = ThemeMode.LIGHT,
    val fontSize: Float = 1f,
    val lineHeight: Float = 1.5f,
    val readingMode: ReadingMode = ReadingMode.PAGINATED,
    val keepScreenAwake: Boolean = false,
    val useDnd: Boolean = false,
    val exitHoldDurationSeconds: Int = 3,
)

@Singleton
class ReaderPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val fontSize = floatPreferencesKey("font_size")
        val lineHeight = floatPreferencesKey("line_height")
        val readingMode = stringPreferencesKey("reading_mode")
        val keepScreenAwake = booleanPreferencesKey("keep_screen_awake")
        val useDnd = booleanPreferencesKey("use_dnd")
        val exitHoldDurationSeconds = intPreferencesKey("exit_hold_duration_seconds")
    }

    val preferences: Flow<ReaderPreferences> = context.readerDataStore.data.map { values ->
        ReaderPreferences(
            theme = values[Keys.theme]?.let(ThemeMode::valueOf) ?: ThemeMode.LIGHT,
            fontSize = values[Keys.fontSize] ?: 1f,
            lineHeight = values[Keys.lineHeight] ?: 1.5f,
            readingMode = values[Keys.readingMode]?.let(ReadingMode::valueOf) ?: ReadingMode.PAGINATED,
            keepScreenAwake = values[Keys.keepScreenAwake] ?: false,
            useDnd = values[Keys.useDnd] ?: false,
            exitHoldDurationSeconds = values[Keys.exitHoldDurationSeconds] ?: 3,
        )
    }

    suspend fun setTheme(value: ThemeMode) = context.readerDataStore.edit { it[Keys.theme] = value.name }
    suspend fun setFontSize(value: Float) = context.readerDataStore.edit { it[Keys.fontSize] = value }
    suspend fun setLineHeight(value: Float) = context.readerDataStore.edit { it[Keys.lineHeight] = value }
    suspend fun setReadingMode(value: ReadingMode) = context.readerDataStore.edit { it[Keys.readingMode] = value.name }
    suspend fun setKeepScreenAwake(value: Boolean) = context.readerDataStore.edit { it[Keys.keepScreenAwake] = value }
    suspend fun setUseDnd(value: Boolean) = context.readerDataStore.edit { it[Keys.useDnd] = value }
    suspend fun setExitHoldDurationSeconds(value: Int) = context.readerDataStore.edit { it[Keys.exitHoldDurationSeconds] = value }
}
