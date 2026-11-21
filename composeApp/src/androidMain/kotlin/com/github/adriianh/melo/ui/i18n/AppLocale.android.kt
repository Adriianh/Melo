@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.i18n

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.github.adriianh.core.domain.model.AppLanguage
import java.util.Locale

private val initialSystemLocale: Locale = Locale.getDefault()

@Composable
internal actual fun ProvideAppLocale(
    appLanguage: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseConfiguration = LocalConfiguration.current
    val baseContext = LocalContext.current

    val targetLocale =
        when (appLanguage) {
            AppLanguage.SYSTEM -> initialSystemLocale
            AppLanguage.SPANISH -> Locale.forLanguageTag("es")
            AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
        }
    if (Locale.getDefault() != targetLocale) {
        Locale.setDefault(targetLocale)
    }

    val localizedConfiguration =
        remember(baseConfiguration, targetLocale) {
            Configuration(baseConfiguration).apply {
                setLocale(targetLocale)
            }
        }
    val localizedContext =
        remember(baseContext, localizedConfiguration) {
            baseContext.createConfigurationContext(localizedConfiguration)
        }

    CompositionLocalProvider(
        LocalAppLanguage provides appLanguage,
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
    ) {
        content()
    }
}
