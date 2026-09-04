
package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.innertube.YouTube
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
    configDirPath: String,
    private val dispatcher: CoroutineDispatcher
) : SettingsRepository {

    private val settingsFilePath = "$configDirPath/settings.json"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _settingsFlow = MutableStateFlow(loadSettingsSync())

    init {
        syncYouTubeSession(_settingsFlow.value.sessionCookies)
    }

    override fun getSettingsFlow(): Flow<Settings> = _settingsFlow.asStateFlow()

    override suspend fun getSettings(): Settings = _settingsFlow.value

    override suspend fun updateSettings(settings: Settings) {
        _settingsFlow.value = settings
        syncYouTubeSession(settings.sessionCookies)
        saveSettingsToDisk(settings)
    }

    private fun syncYouTubeSession(cookies: String?) {
        if (!cookies.isNullOrBlank()) {
            YouTube.cookie = cookies
            YouTube.useLoginForBrowse = true
        } else {
            YouTube.cookie = null
            YouTube.useLoginForBrowse = false
        }
    }

    private fun loadSettingsSync(): Settings {
        if (!PlatformFileSystem.fileExists(settingsFilePath)) return Settings()
        return try {
            val content = PlatformFileSystem.readText(settingsFilePath) ?: return Settings()
            json.decodeFromString(Settings.serializer(), content)
        } catch (e: Exception) {
            e.printStackTrace()
            Settings()
        }
    }

    private suspend fun saveSettingsToDisk(settings: Settings) {
        withContext(dispatcher) {
            try {
                val jsonString = json.encodeToString(settings)
                PlatformFileSystem.writeText(settingsFilePath, jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}