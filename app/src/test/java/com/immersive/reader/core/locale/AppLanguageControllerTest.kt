package com.immersive.reader.core.locale

import com.immersive.reader.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageControllerTest {
    @Test
    fun blankTagsFollowTheSystemLanguage() {
        assertEquals(AppLanguage.SYSTEM, appLanguageFromTags(""))
        assertEquals(AppLanguage.SYSTEM, appLanguageFromTags("   "))
    }

    @Test
    fun chineseTagsSelectSimplifiedChinese() {
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, appLanguageFromTags("zh-CN"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, appLanguageFromTags("zh"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, appLanguageFromTags("zh-Hans-CN"))
    }

    @Test
    fun otherTagsSelectEnglish() {
        assertEquals(AppLanguage.ENGLISH, appLanguageFromTags("en"))
        assertEquals(AppLanguage.ENGLISH, appLanguageFromTags("en-US"))
    }
}
