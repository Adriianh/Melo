@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import com.github.adriianh.core.domain.model.AppLanguage
import java.util.Locale

private val initialSystemLocale: Locale = Locale.getDefault()

@Composable
internal actual fun ProvideAppLocale(
    appLanguage: AppLanguage,
    content: @Composable () -> Unit,
) {
    val targetLocale =
        when (appLanguage) {
            AppLanguage.SYSTEM -> initialSystemLocale
            AppLanguage.SPANISH -> Locale.forLanguageTag("es")
            AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
        }
    if (Locale.getDefault() != targetLocale) {
        Locale.setDefault(targetLocale)
    }

    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
        key(appLanguage) {
            content()
        }
    }
}
