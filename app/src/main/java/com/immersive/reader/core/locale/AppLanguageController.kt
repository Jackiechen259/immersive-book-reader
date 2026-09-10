package com.immersive.reader.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.immersive.reader.core.model.AppLanguage

fun currentAppLanguage(): AppLanguage =
    appLanguageFromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

fun appLanguageFromTags(tags: String): AppLanguage {
    if (tags.isBlank()) return AppLanguage.SYSTEM
    return if (tags.startsWith("zh", ignoreCase = true)) {
        AppLanguage.SIMPLIFIED_CHINESE
    } else {
        AppLanguage.ENGLISH
    }
}

fun applyAppLanguage(language: AppLanguage) {
    val locales = when (language) {
        AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        AppLanguage.SIMPLIFIED_CHINESE -> LocaleListCompat.forLanguageTags("zh-CN")
    }
    AppCompatDelegate.setApplicationLocales(locales)
}
