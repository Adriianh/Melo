package com.github.adriianh.data.player

import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages playback volume, mute state, audio output device selection, and
 * bidirectional synchronization with persisted user settings.
 */
internal class AudioSettingsCoordinator(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val meloPlayer: MeloPlayer,
    private val getSettingsUseCase: GetSettingsUseCase?,
    private val updateSettingsUseCase: UpdateSettingsUseCase?,
    private val onOfflineModeToggledOnline: () -> Unit,
) {
    private val _volume = MutableStateFlow(DEFAULT_VOLUME)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    var isOfflineSettingsEnabled: Boolean = false
        private set

    private var lastUnmutedVolume: Float = DEFAULT_VOLUME
    private var saveVolumeJob: Job? = null
    private var lastAppliedPreferredAudioOutputId: String? = null
    private var hasSyncedInitialAudioOutput = false

    fun start() {
        meloPlayer.setVolume(_volume.value)
        scope.launch(dispatcher) {
            getSettingsUseCase?.invoke()?.collectLatest { settings ->
                val wasOfflineSettings = isOfflineSettingsEnabled
                isOfflineSettingsEnabled = settings.offlineMode
                if (wasOfflineSettings && !settings.offlineMode) {
                    onOfflineModeToggledOnline()
                }
                syncVolumeFromSettings(settings.volume)
                syncAudioOutputFromSettings(settings.preferredAudioOutputId)
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _volume.value = clamped
        if (clamped > UNMUTE_THRESHOLD) {
            lastUnmutedVolume = clamped
        }
        meloPlayer.setVolume(clamped)
        persistVolumeDebounced(clamped)
    }

    fun toggleMute() {
        if (_volume.value > MUTE_THRESHOLD) {
            lastUnmutedVolume = _volume.value
            setVolume(0f)
        } else {
            setVolume(lastUnmutedVolume)
        }
    }

    fun selectAudioOutput(deviceId: String?) {
        lastAppliedPreferredAudioOutputId = deviceId
        hasSyncedInitialAudioOutput = true
        meloPlayer.selectAudioOutput(deviceId)
        updateSettingsUseCase?.let { update ->
            scope.launch(dispatcher) {
                update { current -> current.copy(preferredAudioOutputId = deviceId) }
            }
        }
    }

    fun cancel() {
        saveVolumeJob?.cancel()
        saveVolumeJob = null
    }

    private fun syncVolumeFromSettings(volumePercent: Int) {
        val newVol = (volumePercent.toFloat() / 100f).coerceIn(0f, 1f)
        if (newVol > UNMUTE_THRESHOLD) {
            lastUnmutedVolume = newVol
        }
        if (saveVolumeJob?.isActive != true && abs(_volume.value - newVol) > MUTE_THRESHOLD) {
            _volume.value = newVol
            meloPlayer.setVolume(newVol)
        }
    }

    private fun syncAudioOutputFromSettings(preferredDeviceId: String?) {
        if (!hasSyncedInitialAudioOutput || preferredDeviceId != lastAppliedPreferredAudioOutputId) {
            hasSyncedInitialAudioOutput = true
            lastAppliedPreferredAudioOutputId = preferredDeviceId
            if (preferredDeviceId != null || meloPlayer.selectedAudioOutputId.value != null) {
                meloPlayer.selectAudioOutput(preferredDeviceId)
            }
        }
    }

    private fun persistVolumeDebounced(clamped: Float) {
        val getSettings = getSettingsUseCase ?: return
        val updateSettings = updateSettingsUseCase ?: return
        saveVolumeJob?.cancel()
        saveVolumeJob =
            scope.launch(dispatcher) {
                delay(VOLUME_SAVE_DEBOUNCE_MS.milliseconds)
                val currentSettings = getSettings.getSnapshot()
                val volInt = (clamped * 100).toInt()
                if (currentSettings.volume != volInt) {
                    updateSettings(currentSettings.copy(volume = volInt))
                }
            }
    }

    private companion object {
        private const val DEFAULT_VOLUME = 0.75f
        private const val UNMUTE_THRESHOLD = 0.05f
        private const val MUTE_THRESHOLD = 0.01f
        private const val VOLUME_SAVE_DEBOUNCE_MS = 300L
    }
}
