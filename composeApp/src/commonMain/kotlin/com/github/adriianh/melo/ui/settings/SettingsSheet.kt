@file:Suppress("FunctionNaming", "LongParameterList", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.util.MeloColors

private val MODAL_DIALOG_MIN_WIDTH = 600.dp
private val MODAL_DIALOG_MIN_HEIGHT = 480.dp
private val MODAL_DIALOG_MAX_HEIGHT = 680.dp

private val SheetContentNestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset =
            if (source == NestedScrollSource.UserInput && available.y < 0f) {
                Offset(0f, available.y)
            } else {
                Offset.Zero
            }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity,
        ): Velocity =
            if (available.y < 0f) {
                Velocity(0f, available.y)
            } else {
                Velocity.Zero
            }
    }

@Composable
fun SettingsModal(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val windowHeight = with(density) { windowInfo.containerSize.height.toDp() }
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }

    if (windowWidth >= MODAL_DIALOG_MIN_WIDTH && windowHeight >= MODAL_DIALOG_MIN_HEIGHT) {
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
            selectedCategoryName = selectedCategoryName,
            onSelectedCategoryChange = { selectedCategoryName = it },
        )
    } else {
        SettingsSheet(
            settings = settings,
            isLoggedIn = isLoggedIn,
            profiles = profiles,
            activePageId = activePageId,
            isLoadingProfiles = isLoadingProfiles,
            isSwitchingProfile = isSwitchingProfile,
            profilesError = profilesError,
            actions = actions,
            onDismiss = onDismiss,
            selectedCategoryName = selectedCategoryName,
            onSelectedCategoryChange = { selectedCategoryName = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    onDismiss: () -> Unit,
    selectedCategoryName: String? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MeloColors.surface1,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MeloColors.textMuted.copy(alpha = 0.4f)),
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
            selectedCategoryName = selectedCategoryName,
            onSelectedCategoryChange = onSelectedCategoryChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .nestedScroll(SheetContentNestedScrollConnection),
        )
    }
}

@Composable
fun SettingsDialog(
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
    selectedCategoryName: String? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
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
            selectedCategoryName = selectedCategoryName,
            onSelectedCategoryChange = onSelectedCategoryChange,
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
    updateState: UpdateState? = null,
) {
    val dialogShape = RoundedCornerShape(24.dp)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 860.dp)
                .height(dialogHeight)
                .clip(dialogShape),
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
            onClose = onDismiss,
            updateState = updateState,
        )
    }
}
