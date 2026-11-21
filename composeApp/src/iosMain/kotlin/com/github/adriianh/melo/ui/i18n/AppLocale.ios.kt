@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import com.github.adriianh.core.domain.model.AppLanguage
import platform.Foundation.NSUserDefaults

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

@Composable
internal actual fun ProvideAppLocale(
    appLanguage: AppLanguage,
    content: @Composable () -> Unit,
) {
    val defaults = NSUserDefaults.standardUserDefaults
    val tag = appLanguage.languageTag
    if (tag == null) {
        defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
    } else {
        defaults.setObject(listOf(tag), forKey = APPLE_LANGUAGES_KEY)
    }
    defaults.synchronize()

    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
        key(appLanguage) {
            content()
        }
    }
}
