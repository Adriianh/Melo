package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.offline.*

data class OfflineInteractors(
    val getOfflineTracks: GetOfflineTracksUseCase,
    val syncOfflineTracks: SyncOfflineTracksUseCase,
    val downloadTrack: DownloadTrackUseCase,
    val deleteDownloadedTrack: DeleteDownloadedTrackUseCase,
    val markTrackAccessed: MarkTrackAccessedUseCase,
    val autoCleanup: AutoCleanupUseCase,
)
