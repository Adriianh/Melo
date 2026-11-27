package com.github.adriianh.melo.ui.settings

import com.github.adriianh.core.domain.model.AppLanguage
import com.github.adriianh.core.domain.model.ArtworkCornerStyle
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.CacheSizeLimit
import com.github.adriianh.core.domain.model.DockedCollapsedArtworkMode
import com.github.adriianh.core.domain.model.DockedInitialTab
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.FullscreenIdleHideDelay
import com.github.adriianh.core.domain.model.LyricsActiveLineColor
import com.github.adriianh.core.domain.model.LyricsActiveLineWeight
import com.github.adriianh.core.domain.model.LyricsAlignment
import com.github.adriianh.core.domain.model.LyricsAnchorPosition
import com.github.adriianh.core.domain.model.LyricsAutoResumeDelay
import com.github.adriianh.core.domain.model.LyricsFontScale
import com.github.adriianh.core.domain.model.LyricsInactiveOpacity
import com.github.adriianh.core.domain.model.LyricsLineSpacing
import com.github.adriianh.core.domain.model.NowPlayingBackdropScrim
import com.github.adriianh.core.domain.model.NowPlayingBlurIntensity
import com.github.adriianh.core.domain.model.NowPlayingInitialPanel
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset
import com.github.adriianh.core.domain.model.update.UpdateChannel

data class SettingsActions(
    val onAppLanguageSelected: (AppLanguage) -> Unit = {},
    val onThemeModeSelected: (ThemeMode) -> Unit,
    val onThemePresetSelected: (ThemePreset) -> Unit,
    val onDynamicColorToggle: (Boolean) -> Unit,
    val onAmbientMotionToggle: (Boolean) -> Unit = {},
    val onNowPlayingBlurToggle: (Boolean) -> Unit = {},
    val onNowPlayingBlurIntensitySelected: (NowPlayingBlurIntensity) -> Unit = {},
    val onNowPlayingBackdropScrimSelected: (NowPlayingBackdropScrim) -> Unit = {},
    val onArtworkCornerStyleSelected: (ArtworkCornerStyle) -> Unit = {},
    val onArtworkCustomCornerRadiusChanged: (Int) -> Unit = {},
    val onReduceTransparencyToggle: (Boolean) -> Unit = {},
    val onReduceMotionToggle: (Boolean) -> Unit = {},
    val onLyricsBlurToggle: (Boolean) -> Unit = {},
    val onLyricsActiveLineZoomToggle: (Boolean) -> Unit = {},
    val onLyricsFontScaleSelected: (LyricsFontScale) -> Unit = {},
    val onLyricsCustomFontScaleChanged: (Float) -> Unit = {},
    val onLyricsLineSpacingSelected: (LyricsLineSpacing) -> Unit = {},
    val onLyricsCustomLineSpacingChanged: (Int) -> Unit = {},
    val onLyricsActiveLineWeightSelected: (LyricsActiveLineWeight) -> Unit = {},
    val onLyricsInactiveOpacitySelected: (LyricsInactiveOpacity) -> Unit = {},
    val onLyricsCustomInactiveOpacityChanged: (Float) -> Unit = {},
    val onLyricsAlignmentSelected: (LyricsAlignment) -> Unit = {},
    val onLyricsActiveLineColorSelected: (LyricsActiveLineColor) -> Unit = {},
    val onLyricsAnchorPositionSelected: (LyricsAnchorPosition) -> Unit = {},
    val onLyricsSyncOffsetChanged: (Int) -> Unit = {},
    val onLyricsShowSourceBadgeToggle: (Boolean) -> Unit = {},
    val onLyricsShowInstrumentalBreaksToggle: (Boolean) -> Unit = {},
    val onLyricsShowCompactPreviewLineToggle: (Boolean) -> Unit = {},
    val onLyricsAutoResumeDelaySelected: (LyricsAutoResumeDelay) -> Unit = {},
    val onLyricsCustomAutoResumeSecondsChanged: (Int) -> Unit = {},
    val onLyricsKeepSyncButtonOnIdleToggle: (Boolean) -> Unit = {},
    val onLyricsAutoTranslateToggle: (Boolean) -> Unit = {},
    val onLyricsDefaultTargetLanguageSelected: (String) -> Unit = {},
    val onNowPlayingInitialPanelSelected: (NowPlayingInitialPanel) -> Unit = {},
    val onPlayerShowRemainingTimeToggle: (Boolean) -> Unit = {},
    val onFullscreenIdleHideDelaySelected: (FullscreenIdleHideDelay) -> Unit = {},
    val onFullscreenCustomIdleHideSecondsChanged: (Int) -> Unit = {},
    val onNowPlayingKeepScreenOnToggle: (Boolean) -> Unit = {},
    val onDockShowOnStartupToggle: (Boolean) -> Unit = {},
    val onDockInitialTabSelected: (DockedInitialTab) -> Unit = {},
    val onDockExpandQueueByDefaultToggle: (Boolean) -> Unit = {},
    val onDockShowSuggestionsToggle: (Boolean) -> Unit = {},
    val onDockCollapsedArtworkModeSelected: (DockedCollapsedArtworkMode) -> Unit = {},
    val onResetNowPlayingDefaults: () -> Unit = {},
    val onDataSaverToggle: (Boolean) -> Unit,
    val onAudioQualitySelected: (AudioQuality) -> Unit,
    val onDownloadFormatSelected: (DownloadFormat) -> Unit = {},
    val onDownloadQualitySelected: (DownloadQuality) -> Unit = {},
    val onCacheSizeLimitSelected: (CacheSizeLimit) -> Unit = {},
    val onOfflineModeToggle: (Boolean) -> Unit = {},
    val onClearCache: () -> Unit = {},
    val onAutoplayToggle: (Boolean) -> Unit = {},
    val onResumeLastActiveSessionToggle: (Boolean) -> Unit = {},
    val onStopPlaybackOnTaskRemovalToggle: (Boolean) -> Unit = {},
    val onDiscordRpcToggle: (Boolean) -> Unit = {},
    val onDiscordTokenChanged: (String?) -> Unit = {},
    val onAddLocalPath: (String) -> Unit = {},
    val onRemoveLocalPath: (String) -> Unit = {},
    val onSyncHistoryToYouTubeChanged: (Boolean) -> Unit = {},
    val onToggleAutoCheckUpdates: (Boolean) -> Unit = {},
    val onUpdateChannelSelected: (UpdateChannel) -> Unit = {},
    val onSelectAccountProfile: (String?) -> Unit = {},
    val onRefreshAccountProfiles: () -> Unit = {},
    val onOpenLogin: () -> Unit,
    val onLogout: () -> Unit,
)
