@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.adriianh.core.domain.model.AppLanguage

internal val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SYSTEM }

@Composable
internal expect fun ProvideAppLocale(
    appLanguage: AppLanguage,
    content: @Composable () -> Unit,
)
