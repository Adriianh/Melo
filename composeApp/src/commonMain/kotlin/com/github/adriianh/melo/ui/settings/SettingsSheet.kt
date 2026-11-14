@file:Suppress("FunctionNaming", "LongParameterList", "MagicNumber", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.getPlatform

private val MODAL_DIALOG_MIN_WIDTH = 600.dp
private val MODAL_DIALOG_MIN_HEIGHT = 480.dp
private val MODAL_DIALOG_MAX_HEIGHT = 680.dp

@Composable
internal fun SettingsModal(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    onDismiss: () -> Unit,
    initialCategory: SettingsCategory? = null,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val windowHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val isExpandedDialog = windowWidth >= MODAL_DIALOG_MIN_WIDTH && windowHeight >= MODAL_DIALOG_MIN_HEIGHT
    val platform = remember { getPlatform() }
    val firstCategory = remember(platform.type) { SettingsCategory.visibleFor(platform.type).firstOrNull() }

    var selectedCategoryName by rememberSaveable {
        mutableStateOf(
            initialCategory?.name ?: if (isExpandedDialog) firstCategory?.name else null,
        )
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isCompactSearchOpen by rememberSaveable { mutableStateOf(false) }

    if (isExpandedDialog) {
        val dialogHeight = minOf(windowHeight * 0.85f, MODAL_DIALOG_MAX_HEIGHT)
        SettingsDialog(
            settings = settings,
            isLoggedIn = isLoggedIn,
            profiles = profiles,
            activePageId = activePageId,
            isLoadingProfiles = isLoadingProfiles,
            isSwitchingProfile = isSwitchingProfile,
            profilesError = profilesError,
            actions = actions,
            onDismiss = onDismiss,
            dialogHeight = dialogHeight,
            initialCategory = initialCategory,
            selectedCategoryName = selectedCategoryName,
            onSelectedCategoryChange = { selectedCategoryName = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            isCompactSearchOpen = isCompactSearchOpen,
            onCompactSearchOpenChange = { isCompactSearchOpen = it },
        )
    } else {
        SettingsFullScreenSurface(
            settings = settings,
            isLoggedIn = isLoggedIn,
            profiles = profiles,
            activePageId = activePageId,
            isLoadingProfiles = isLoadingProfiles,
            isSwitchingProfile = isSwitchingProfile,
            profilesError = profilesError,
            actions = actions,
            onDismiss = onDismiss,
            initialCategory = initialCategory,
            selectedCategoryName = selectedCategoryName,
            onSelectedCategoryChange = { selectedCategoryName = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            isCompactSearchOpen = isCompactSearchOpen,
            onCompactSearchOpenChange = { isCompactSearchOpen = it },
        )
    }
}

@Composable
internal fun SettingsFullScreenSurface(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    onDismiss: () -> Unit,
    initialCategory: SettingsCategory? = null,
    selectedCategoryName: String? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    isCompactSearchOpen: Boolean? = null,
    onCompactSearchOpenChange: ((Boolean) -> Unit)? = null,
    updateState: UpdateState? = null,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MeloColors.surface1)
                .pointerInput(Unit) {}
                .semantics { paneTitle = "Ajustes" },
        color = MeloColors.surface1,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            SettingsContent(
                settings = settings,
                isLoggedIn = isLoggedIn,
                profiles = profiles,
                activePageId = activePageId,
                isLoadingProfiles = isLoadingProfiles,
                isSwitchingProfile = isSwitchingProfile,
                profilesError = profilesError,
                actions = actions,
                modifier = Modifier.fillMaxSize(),
                initialCategory = initialCategory,
                selectedCategoryName = selectedCategoryName,
                onSelectedCategoryChange = onSelectedCategoryChange,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                isCompactSearchOpen = isCompactSearchOpen,
                onCompactSearchOpenChange = onCompactSearchOpenChange,
                onClose = onDismiss,
                updateState = updateState,
            )
        }
    }
}

@Composable
internal fun SettingsDialog(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    onDismiss: () -> Unit,
    dialogHeight: Dp = MODAL_DIALOG_MAX_HEIGHT,
    initialCategory: SettingsCategory? = null,
    selectedCategoryName: String? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    isCompactSearchOpen: Boolean? = null,
    onCompactSearchOpenChange: ((Boolean) -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = {
            if (!searchQuery.isNullOrEmpty() && onSearchQueryChange != null) {
                onSearchQueryChange("")
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SettingsDialogSurface(
            settings = settings,
            isLoggedIn = isLoggedIn,
            profiles = profiles,
            activePageId = activePageId,
            isLoadingProfiles = isLoadingProfiles,
            isSwitchingProfile = isSwitchingProfile,
            profilesError = profilesError,
            actions = actions,
            onDismiss = onDismiss,
            dialogHeight = dialogHeight,
            initialCategory = initialCategory,
            selectedCategoryName = selectedCategoryName,
            onSelectedCategoryChange = onSelectedCategoryChange,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            isCompactSearchOpen = isCompactSearchOpen,
            onCompactSearchOpenChange = onCompactSearchOpenChange,
        )
    }
}

@Composable
internal fun SettingsDialogSurface(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    onDismiss: () -> Unit,
    dialogHeight: Dp = MODAL_DIALOG_MAX_HEIGHT,
    initialCategory: SettingsCategory? = null,
    selectedCategoryName: String? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    isCompactSearchOpen: Boolean? = null,
    onCompactSearchOpenChange: ((Boolean) -> Unit)? = null,
    updateState: UpdateState? = null,
) {
    val dialogShape = RoundedCornerShape(24.dp)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 860.dp)
                .height(dialogHeight)
                .clip(dialogShape)
                .semantics { paneTitle = "Ajustes" }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        if (!searchQuery.isNullOrEmpty() && onSearchQueryChange != null) {
                            onSearchQueryChange("")
                            true
                        } else {
                            onDismiss()
                            true
                        }
                    } else {
                        false
                    }
                },
        shape = dialogShape,
        color = MeloColors.surface1,
        border = BorderStroke(1.dp, MeloColors.border),
    ) {
        SettingsContent(
            settings = settings,
            isLoggedIn = isLoggedIn,
            profiles = profiles,
            activePageId = activePageId,
            isLoadingProfiles = isLoadingProfiles,
            isSwitchingProfile = isSwitchingProfile,
            profilesError = profilesError,
            actions = actions,
            modifier = Modifier.fillMaxSize(),
            initialCategory = initialCategory,
            selectedCategoryName = selectedCategoryName,
            onSelectedCategoryChange = onSelectedCategoryChange,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            isCompactSearchOpen = isCompactSearchOpen,
            onCompactSearchOpenChange = onCompactSearchOpenChange,
            onClose = onDismiss,
            updateState = updateState,
        )
    }
}
