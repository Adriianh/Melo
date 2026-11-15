@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "LongParameterList",
    "TooManyFunctions",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

private val EXPANDED_SETTINGS_BREAKPOINT = 540.dp

@Composable
internal fun SettingsContent(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    initialCategory: SettingsCategory? = null,
    selectedCategoryName: String? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    updateState: UpdateState? = null,
) {
    val platform = remember { getPlatform() }
    val categories = remember(platform.type) { SettingsCategory.visibleFor(platform.type) }
    var internalSelectedCategoryName by rememberSaveable { mutableStateOf(initialCategory?.name) }
    val activeCategoryName =
        if (onSelectedCategoryChange != null) {
            selectedCategoryName ?: initialCategory?.name
        } else {
            internalSelectedCategoryName
        }
    val setSelectedCategoryName: (String?) -> Unit = { nextName ->
        if (onSelectedCategoryChange != null) {
            onSelectedCategoryChange(nextName)
        } else {
            internalSelectedCategoryName = nextName
        }
    }
    val selectedCategory = categories.find { it.name == activeCategoryName }

    val updateViewModel: UpdateViewModel? = if (updateState != null) null else koinViewModel()
    val vmUpdateState = updateViewModel?.uiState?.collectAsState()?.value
    val resolvedUpdateState = updateState ?: vmUpdateState ?: UpdateState.Idle

    val entries =
        rememberSettingEntries(
            settings = settings,
            isLoggedIn = isLoggedIn,
            profiles = profiles,
            activePageId = activePageId,
            isLoadingProfiles = isLoadingProfiles,
            isSwitchingProfile = isSwitchingProfile,
            profilesError = profilesError,
            actions = actions,
            platformType = platform.type,
            updateState = resolvedUpdateState,
            onCheckForUpdates = { channel -> updateViewModel?.checkForUpdates(channel) },
            onUpdateChannelSelected = { channel ->
                actions.onUpdateChannelSelected(channel)
                updateViewModel?.checkForUpdates(channel)
            },
            onStartUpdateDownload = { release, asset ->
                updateViewModel?.startDownload(release, asset)
            },
            onCancelUpdateDownload = { updateViewModel?.cancelDownload() },
            onInstallUpdate = { path -> updateViewModel?.installUpdate(path) },
        )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= EXPANDED_SETTINGS_BREAKPOINT

        if (isExpanded) {
            val activeCategory = selectedCategory ?: categories.first()
            Row(modifier = Modifier.fillMaxSize()) {
                SettingsSidebar(
                    categories = categories,
                    selectedCategory = activeCategory,
                    updateState = resolvedUpdateState,
                    onSelectCategory = { setSelectedCategoryName(it.name) },
                )

                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MeloColors.border),
                )

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    SettingsExpandedDetailHeader(
                        category = activeCategory,
                        onClose = onClose,
                    )

                    val detailScrollState = rememberScrollState()
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(
                                    state = detailScrollState,
                                    enabled = detailScrollState.maxValue > 0,
                                ).padding(horizontal = 24.dp),
                    ) {
                        SettingsCategoryEntriesPane(
                            entries = entries,
                            category = activeCategory,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        } else {
            SettingsBackHandler(enabled = selectedCategory != null) {
                setSelectedCategoryName(null)
            }

            AnimatedContent(
                targetState = selectedCategory,
                label = "SettingsCategoryTransition",
                modifier = Modifier.fillMaxSize(),
            ) { currentCategory ->
                if (currentCategory == null) {
                    SettingsCategoryList(
                        categories = categories,
                        settings = settings,
                        isLoggedIn = isLoggedIn,
                        updateState = resolvedUpdateState,
                        onSelectCategory = { setSelectedCategoryName(it.name) },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SettingsCompactDetailHeader(
                            category = currentCategory,
                            onBack = { setSelectedCategoryName(null) },
                        )

                        val detailScrollState = rememberScrollState()
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(
                                        state = detailScrollState,
                                        enabled = detailScrollState.maxValue > 0,
                                    ),
                        ) {
                            SettingsCategoryEntriesPane(
                                entries = entries,
                                category = currentCategory,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsExpandedDetailHeader(
    category: SettingsCategory,
    onClose: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = category.title,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MeloColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsCompactDetailHeader(
    category: SettingsCategory,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver a categorías",
                tint = MeloColors.textPrimary,
            )
        }
        Text(
            text = category.title,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
