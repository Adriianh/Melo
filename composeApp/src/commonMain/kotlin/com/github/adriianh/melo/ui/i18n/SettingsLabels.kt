@file:Suppress("TooManyFunctions")

package com.github.adriianh.melo.ui.i18n

import androidx.compose.runtime.Composable
import com.github.adriianh.core.domain.model.AppLanguage
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.CacheSizeLimit
import com.github.adriianh.core.domain.model.FullscreenIdleHideDelay
import com.github.adriianh.core.domain.model.LyricsActiveLineColor
import com.github.adriianh.core.domain.model.LyricsAlignment
import com.github.adriianh.core.domain.model.LyricsFontScale
import com.github.adriianh.core.domain.model.NowPlayingBackdropScrim
import com.github.adriianh.core.domain.model.NowPlayingBlurIntensity
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.settings_blur_intense
import com.github.adriianh.melo.resources.settings_blur_medium
import com.github.adriianh.melo.resources.settings_blur_off
import com.github.adriianh.melo.resources.settings_blur_soft
import com.github.adriianh.melo.resources.settings_cache_hint_10gb
import com.github.adriianh.melo.resources.settings_cache_hint_1gb
import com.github.adriianh.melo.resources.settings_cache_hint_2gb
import com.github.adriianh.melo.resources.settings_cache_hint_500mb
import com.github.adriianh.melo.resources.settings_cache_hint_5gb
import com.github.adriianh.melo.resources.settings_cache_hint_unlimited
import com.github.adriianh.melo.resources.settings_cache_unlimited
import com.github.adriianh.melo.resources.settings_idle_hide_10s
import com.github.adriianh.melo.resources.settings_idle_hide_3s
import com.github.adriianh.melo.resources.settings_idle_hide_5s
import com.github.adriianh.melo.resources.settings_idle_hide_off
import com.github.adriianh.melo.resources.settings_language_english
import com.github.adriianh.melo.resources.settings_language_spanish
import com.github.adriianh.melo.resources.settings_language_system
import com.github.adriianh.melo.resources.settings_lyrics_align_center
import com.github.adriianh.melo.resources.settings_lyrics_align_start
import com.github.adriianh.melo.resources.settings_lyrics_color_accent
import com.github.adriianh.melo.resources.settings_lyrics_color_white
import com.github.adriianh.melo.resources.settings_lyrics_scale_compact
import com.github.adriianh.melo.resources.settings_lyrics_scale_large
import com.github.adriianh.melo.resources.settings_lyrics_scale_normal
import com.github.adriianh.melo.resources.settings_quality_auto
import com.github.adriianh.melo.resources.settings_quality_high
import com.github.adriianh.melo.resources.settings_quality_hint_auto
import com.github.adriianh.melo.resources.settings_quality_low
import com.github.adriianh.melo.resources.settings_quality_medium
import com.github.adriianh.melo.resources.settings_scrim_dark
import com.github.adriianh.melo.resources.settings_scrim_light
import com.github.adriianh.melo.resources.settings_scrim_normal
import com.github.adriianh.melo.resources.settings_theme_dark
import com.github.adriianh.melo.resources.settings_theme_light
import com.github.adriianh.melo.resources.settings_theme_system
import com.github.adriianh.melo.resources.settings_update_channel_nightly
import com.github.adriianh.melo.resources.settings_update_channel_stable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun ThemeMode.labelRes(): StringResource =
    when (this) {
        ThemeMode.SYSTEM -> Res.string.settings_theme_system
        ThemeMode.LIGHT -> Res.string.settings_theme_light
        ThemeMode.DARK -> Res.string.settings_theme_dark
    }

@Composable
internal fun ThemeMode.uiLabel(): String = stringResource(labelRes())

internal fun AudioQuality.titleRes(): StringResource =
    when (this) {
        AudioQuality.AUTO -> Res.string.settings_quality_auto
        AudioQuality.HIGH -> Res.string.settings_quality_high
        AudioQuality.MEDIUM -> Res.string.settings_quality_medium
        AudioQuality.LOW -> Res.string.settings_quality_low
    }

@Composable
internal fun AudioQuality.uiTitle(): String = stringResource(titleRes())

@Composable
internal fun AudioQuality.uiHint(): String =
    when (this) {
        AudioQuality.AUTO -> stringResource(Res.string.settings_quality_hint_auto)
        AudioQuality.HIGH -> "160–256 kbps"
        AudioQuality.MEDIUM -> "128 kbps"
        AudioQuality.LOW -> "48–64 kbps"
    }

@Composable
internal fun CacheSizeLimit.uiTitle(): String =
    when (this) {
        CacheSizeLimit.UNLIMITED -> stringResource(Res.string.settings_cache_unlimited)
        else -> displayName
    }

internal fun CacheSizeLimit.hintRes(): StringResource =
    when (this) {
        CacheSizeLimit.MB_500 -> Res.string.settings_cache_hint_500mb
        CacheSizeLimit.GB_1 -> Res.string.settings_cache_hint_1gb
        CacheSizeLimit.GB_2 -> Res.string.settings_cache_hint_2gb
        CacheSizeLimit.GB_5 -> Res.string.settings_cache_hint_5gb
        CacheSizeLimit.GB_10 -> Res.string.settings_cache_hint_10gb
        CacheSizeLimit.UNLIMITED -> Res.string.settings_cache_hint_unlimited
    }

@Composable
internal fun CacheSizeLimit.uiHint(): String = stringResource(hintRes())

internal fun NowPlayingBlurIntensity.labelRes(): StringResource =
    when (this) {
        NowPlayingBlurIntensity.OFF -> Res.string.settings_blur_off
        NowPlayingBlurIntensity.SOFT -> Res.string.settings_blur_soft
        NowPlayingBlurIntensity.MEDIUM -> Res.string.settings_blur_medium
        NowPlayingBlurIntensity.INTENSE -> Res.string.settings_blur_intense
    }

@Composable
internal fun NowPlayingBlurIntensity.uiLabel(): String = stringResource(labelRes())

internal fun NowPlayingBackdropScrim.labelRes(): StringResource =
    when (this) {
        NowPlayingBackdropScrim.LIGHT -> Res.string.settings_scrim_light
        NowPlayingBackdropScrim.NORMAL -> Res.string.settings_scrim_normal
        NowPlayingBackdropScrim.DARK -> Res.string.settings_scrim_dark
    }

@Composable
internal fun NowPlayingBackdropScrim.uiLabel(): String = stringResource(labelRes())

internal fun UpdateChannel.labelRes(): StringResource =
    when (this) {
        UpdateChannel.STABLE -> Res.string.settings_update_channel_stable
        UpdateChannel.NIGHTLY -> Res.string.settings_update_channel_nightly
    }

@Composable
internal fun UpdateChannel.uiLabel(): String = stringResource(labelRes())

internal fun AppLanguage.labelRes(): StringResource =
    when (this) {
        AppLanguage.SYSTEM -> Res.string.settings_language_system
        AppLanguage.SPANISH -> Res.string.settings_language_spanish
        AppLanguage.ENGLISH -> Res.string.settings_language_english
    }

@Composable
internal fun AppLanguage.uiLabel(): String = stringResource(labelRes())

internal fun LyricsFontScale.labelRes(): StringResource =
    when (this) {
        LyricsFontScale.COMPACT -> Res.string.settings_lyrics_scale_compact
        LyricsFontScale.NORMAL -> Res.string.settings_lyrics_scale_normal
        LyricsFontScale.LARGE -> Res.string.settings_lyrics_scale_large
    }

@Composable
internal fun LyricsFontScale.uiLabel(): String = stringResource(labelRes())

internal fun LyricsAlignment.labelRes(): StringResource =
    when (this) {
        LyricsAlignment.START -> Res.string.settings_lyrics_align_start
        LyricsAlignment.CENTER -> Res.string.settings_lyrics_align_center
    }

@Composable
internal fun LyricsAlignment.uiLabel(): String = stringResource(labelRes())

internal fun LyricsActiveLineColor.labelRes(): StringResource =
    when (this) {
        LyricsActiveLineColor.WHITE -> Res.string.settings_lyrics_color_white
        LyricsActiveLineColor.ACCENT -> Res.string.settings_lyrics_color_accent
    }

@Composable
internal fun LyricsActiveLineColor.uiLabel(): String = stringResource(labelRes())

internal fun FullscreenIdleHideDelay.labelRes(): StringResource =
    when (this) {
        FullscreenIdleHideDelay.OFF -> Res.string.settings_idle_hide_off
        FullscreenIdleHideDelay.SECONDS_3 -> Res.string.settings_idle_hide_3s
        FullscreenIdleHideDelay.SECONDS_5 -> Res.string.settings_idle_hide_5s
        FullscreenIdleHideDelay.SECONDS_10 -> Res.string.settings_idle_hide_10s
    }

@Composable
internal fun FullscreenIdleHideDelay.uiLabel(): String = stringResource(labelRes())
