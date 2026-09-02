package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.offline.AutoCleanupUseCase
import com.github.adriianh.core.domain.usecase.offline.DeleteDownloadedTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.DownloadTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.EnrichLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.MarkTrackAccessedUseCase
import com.github.adriianh.core.domain.usecase.offline.ScanLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.SyncOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.UpdateTrackMetadataUseCase

data class OfflineInteractors(
    val getOfflineTracks: GetOfflineTracksUseCase,
    val syncOfflineTracks: SyncOfflineTracksUseCase,
    val downloadTrack: DownloadTrackUseCase,
    val deleteDownloadedTrack: DeleteDownloadedTrackUseCase,
    val markTrackAccessed: MarkTrackAccessedUseCase,
    val autoCleanup: AutoCleanupUseCase,
    val scanLocalTracks: ScanLocalTracksUseCase,
    val enrichLocalTracks: EnrichLocalTracksUseCase,
    val updateTrackMetadata: UpdateTrackMetadataUseCase
)