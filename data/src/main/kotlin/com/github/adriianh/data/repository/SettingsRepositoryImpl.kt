package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SettingsRepositoryImpl(
    private val configDir: File,
    private val dispatcher: CoroutineDispatcher
) : SettingsRepository {

    private val settingsFile = File(configDir, "settings.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _settingsFlow = MutableStateFlow(loadSettingsSync())

    override fun getSettingsFlow(): Flow<Settings> = _settingsFlow.asStateFlow()

    override suspend fun getSettings(): Settings = _settingsFlow.value

    override suspend fun updateSettings(settings: Settings) {
        _settingsFlow.value = settings
        saveSettingsToDisk(settings)
    }

    private fun loadSettingsSync(): Settings {
        if (!settingsFile.exists()) return Settings()
        return try {
            json.decodeFromString(Settings.serializer(), settingsFile.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            Settings()
        }
    }

    private suspend fun saveSettingsToDisk(settings: Settings) {
        withContext(dispatcher) {
            try {
                if (!configDir.exists()) configDir.mkdirs()
                val jsonString = json.encodeToString(settings)
                settingsFile.writeText(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}