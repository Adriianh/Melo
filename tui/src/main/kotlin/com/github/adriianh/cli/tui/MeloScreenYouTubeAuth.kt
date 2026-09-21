package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.handler.syncYouTubeLibrary
import kotlinx.coroutines.launch

/**
 * YouTube authentication flows owned by [MeloScreen].
 */

internal fun MeloScreen.checkYouTubeAuth() {
    scope.launch {
        val status = youTubeAuthService.getStatus()
        appRunner()?.runOnRenderThread {
            state = state.copy(youtubeAccountName = status.accountName)
        }
        if (status.accountName != null && state.collections.remotePlaylists.isEmpty()) {
            syncYouTubeLibrary()
        }
    }
}

internal fun MeloScreen.importYouTubeAuth() {
    if (settingsViewState.isImportingAuth) return
    settingsViewState = settingsViewState.copy(
        isImportingAuth = true,
        authStatusMessage = "Importing from browser..."
    )
    scope.launch {
        val result = youTubeAuthService.importFromBrowser()
        appRunner()?.runOnRenderThread {
            result.fold(
                onSuccess = { name ->
                    state = state.copy(youtubeAccountName = name)
                    settingsViewState = settingsViewState.copy(
                        isImportingAuth = false,
                        authStatusMessage = "✓ $name"
                    )
                    loadHomeFeed()
                    syncYouTubeLibrary()
                },
                onFailure = { _ ->
                    settingsViewState = settingsViewState.copy(
                        isImportingAuth = false,
                        authStatusMessage = "No browser session found"
                    )
                }
            )
        }
    }
}

internal fun MeloScreen.logoutYouTubeAuth() {
    scope.launch {
        youTubeAuthService.logout()
        appRunner()?.runOnRenderThread {
            state = state.copy(
                youtubeAccountName = null,
                collections = state.collections.copy(remotePlaylists = emptyList())
            )
            settingsViewState = settingsViewState.copy(
                authStatusMessage = "Logged out"
            )
            loadHomeFeed()
        }
    }
}