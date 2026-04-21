package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /**
     * Emits the current settings and any future updates.
     */
    fun getSettingsFlow(): Flow<Settings>

    /**
     * Returns the current settings snapshot.
     */
    suspend fun getSettings(): Settings

    /**
     * Updates the settings and persists them.
     */
    suspend fun updateSettings(settings: Settings)
}