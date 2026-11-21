package com.github.adriianh.melo.ui.i18n

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

sealed interface UiText {
    data class Dynamic(
        val value: String,
    ) : UiText

    data class Resource(
        val res: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class Plural(
        val res: PluralStringResource,
        val quantity: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}

@Suppress("SpreadOperator")
@Composable
fun UiText.resolve(): String =
    when (this) {
        is UiText.Dynamic -> value
        is UiText.Resource -> stringResource(res, *args.toTypedArray())
        is UiText.Plural -> pluralStringResource(res, quantity, *args.toTypedArray())
    }
