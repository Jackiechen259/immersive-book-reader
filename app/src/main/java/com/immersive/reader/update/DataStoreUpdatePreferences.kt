package com.immersive.reader.update

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.updateDataStore by preferencesDataStore(name = "update_preferences")

class DataStoreUpdatePreferences(
    private val context: Context,
) : UpdatePreferences {
    private object Keys {
        val lastCheckEpochMillis = longPreferencesKey("last_check_epoch_millis")
        val skippedTag = stringPreferencesKey("skipped_tag")
    }

    val skippedTagFlow: Flow<String?> = context.updateDataStore.data.map { values ->
        values[Keys.skippedTag]
    }

    override suspend fun lastCheckEpochMillis(): Long? =
        context.updateDataStore.data.first()[Keys.lastCheckEpochMillis]

    override suspend fun setLastCheckEpochMillis(value: Long) {
        context.updateDataStore.edit { it[Keys.lastCheckEpochMillis] = value }
    }

    override suspend fun skippedTag(): String? =
        context.updateDataStore.data.first()[Keys.skippedTag]

    override suspend fun setSkippedTag(value: String?) {
        context.updateDataStore.edit { prefs ->
            if (value == null) prefs.remove(Keys.skippedTag) else prefs[Keys.skippedTag] = value
        }
    }
}
