@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "LongParameterList",
    "MagicNumber",
    "TooManyFunctions",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.core.domain.player.AudioOutputDevice
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_close
import com.github.adriianh.melo.resources.settings_back_to_categories
import com.github.adriianh.melo.resources.settings_breadcrumb_prefix
import com.github.adriianh.melo.resources.settings_close
import com.github.adriianh.melo.resources.settings_empty_subtitle
import com.github.adriianh.melo.resources.settings_empty_title
import com.github.adriianh.melo.resources.settings_search_clear
import com.github.adriianh.melo.resources.settings_search_close
import com.github.adriianh.melo.resources.settings_search_placeholder
import com.github.adriianh.melo.resources.settings_search_results_title
import com.github.adriianh.melo.resources.settings_title
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.getPlatform
import org.jetbrains.compose.resources.stringResource
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
    profilesError: UiText?,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    initialCategory: SettingsCategory? = null,
    selectedCategoryName: String? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    isCompactSearchOpen: Boolean? = null,
    onCompactSearchOpenChange: ((Boolean) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    updateState: UpdateState? = null,
) {
    val platform = remember { getPlatform() }
    val categories = remember(platform.type) { SettingsCategory.visibleFor(platform.type) }

    val (activeCategoryName, setSelectedCategoryName) =
        rememberHoistedState(
            initial = initialCategory?.name,
            externalValue = selectedCategoryName,
            onExternalChange = onSelectedCategoryChange,
        )
    val (activeSearchQuery, setSearchQuery) =
        rememberHoistedState(
            initial = "",
            externalValue = searchQuery ?: "",
            onExternalChange = onSearchQueryChange,
        )
    val (activeCompactSearchOpen, setCompactSearchOpen) =
        rememberHoistedState(
            initial = false,
            externalValue = isCompactSearchOpen == true,
            onExternalChange = onCompactSearchOpenChange,
        )

    val selectedCategory = categories.find { it.name == activeCategoryName }
    val isSearching = activeSearchQuery.isNotBlank()
    val selectCategoryAndClearSearch: (SettingsCategory) -> Unit = { category ->
        setSelectedCategoryName(category.name)
        setSearchQuery("")
        setCompactSearchOpen(false)
    }

    val updateViewModel: UpdateViewModel? = if (updateState != null) null else koinViewModel()
    val vmUpdateState = updateViewModel?.uiState?.collectAsState()?.value
    val resolvedUpdateState = updateState ?: vmUpdateState ?: UpdateState.Idle
    val playerViewModel: PlayerViewModel? = if (updateState != null) null else koinViewModel()
    val audioOutputs: List<AudioOutputDevice> =
        playerViewModel
            ?.availableAudioOutputs
            ?.collectAsState()
            ?.value
            .orEmpty()
    val selectedAudioOutputId: String? =
        playerViewModel
            ?.selectedAudioOutputId
            ?.collectAsState()
            ?.value
            ?: settings.preferredAudioOutputId

    LaunchedEffect(playerViewModel) {
        playerViewModel?.refreshAudioOutputs()
    }

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
            audioOutputs = audioOutputs,
            selectedAudioOutputId = selectedAudioOutputId,
            onRefreshAudioOutputs = { playerViewModel?.refreshAudioOutputs() },
            onSelectAudioOutput = { deviceId -> playerViewModel?.selectAudioOutput(deviceId) },
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
            SettingsExpandedLayout(
                categories = categories,
                entries = entries,
                selectedCategory = selectedCategory,
                searchQuery = activeSearchQuery,
                isSearching = isSearching,
                updateState = resolvedUpdateState,
                onSelectCategory = selectCategoryAndClearSearch,
                onSearchQueryChange = setSearchQuery,
                onClose = onClose,
            )
        } else {
            SettingsCompactLayout(
                categories = categories,
                entries = entries,
                settings = settings,
                isLoggedIn = isLoggedIn,
                selectedCategory = selectedCategory,
                searchQuery = activeSearchQuery,
                isSearchOpen = activeCompactSearchOpen,
                isSearching = isSearching,
                updateState = resolvedUpdateState,
                onSelectCategory = selectCategoryAndClearSearch,
                onClearCategory = { setSelectedCategoryName(null) },
                onSearchQueryChange = setSearchQuery,
                onCompactSearchOpenChange = setCompactSearchOpen,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun SettingsExpandedLayout(
    categories: List<SettingsCategory>,
    entries: List<SettingEntry>,
    selectedCategory: SettingsCategory?,
    searchQuery: String,
    isSearching: Boolean,
    updateState: UpdateState?,
    onSelectCategory: (SettingsCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClose: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        if (searchQuery.isNotEmpty()) {
                            onSearchQueryChange("")
                            true
                        } else if (onClose != null) {
                            onClose()
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                },
    ) {
        SettingsSidebar(
            categories = categories,
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            updateState = updateState,
            onSelectCategory = onSelectCategory,
            onEscapeClose = onClose,
        )

        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MeloColors.border),
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val trailingBreadcrumb =
                when {
                    isSearching -> stringResource(Res.string.settings_search_results_title)
                    selectedCategory != null -> stringResource(selectedCategory.titleRes)
                    else -> null
                }

            SettingsExpandedDetailHeader(
                trailingSegment = trailingBreadcrumb,
                onClose = onClose,
            )

            key(isSearching, selectedCategory) {
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
                    when {
                        isSearching -> {
                            SettingSearchResultsPane(
                                entries = entries,
                                query = searchQuery,
                                onOpenCategory = onSelectCategory,
                            )
                        }
                        selectedCategory != null -> {
                            SettingsCategoryEntriesPane(
                                entries = entries,
                                category = selectedCategory,
                            )
                        }
                        else -> {
                            SettingsEmptyCategoryPane()
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsCompactLayout(
    categories: List<SettingsCategory>,
    entries: List<SettingEntry>,
    settings: Settings,
    isLoggedIn: Boolean,
    selectedCategory: SettingsCategory?,
    searchQuery: String,
    isSearchOpen: Boolean,
    isSearching: Boolean,
    updateState: UpdateState?,
    onSelectCategory: (SettingsCategory) -> Unit,
    onClearCategory: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCompactSearchOpenChange: (Boolean) -> Unit,
    onClose: (() -> Unit)?,
) {
    var predictiveBackProgress by remember { mutableStateOf<Float?>(null) }

    val handleBack: () -> Unit = {
        when {
            selectedCategory != null -> onClearCategory()
            isSearchOpen -> {
                onSearchQueryChange("")
                onCompactSearchOpenChange(false)
            }
            else -> onClose?.invoke()
        }
    }

    SettingsPredictiveBackHandler(
        enabled = selectedCategory != null || isSearchOpen || onClose != null,
        onProgress = { progress -> predictiveBackProgress = progress },
        onBack = handleBack,
    )

    val rootBackProgress =
        if (selectedCategory == null && !isSearchOpen) (predictiveBackProgress ?: 0f) else 0f
    val detailBackProgress =
        if (selectedCategory != null) (predictiveBackProgress ?: 0f) else 0f

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (rootBackProgress > 0f) {
                        val scale = 1f - rootBackProgress * 0.04f
                        scaleX = scale
                        scaleY = scale
                        translationX = rootBackProgress * size.width * 0.10f
                        alpha = 1f - rootBackProgress * 0.25f
                    }
                },
    ) {
        SettingsCompactTopBar(
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            isSearchOpen = isSearchOpen,
            onSearchQueryChange = onSearchQueryChange,
            onOpenSearch = { onCompactSearchOpenChange(true) },
            onCloseSearch = {
                onSearchQueryChange("")
                onCompactSearchOpenChange(false)
            },
            onBackFromCategory = onClearCategory,
            onClose = onClose,
        )

        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                val enteringDetail = targetState != null && initialState == null
                if (enteringDetail) {
                    (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "SettingsCategorySlideTransition",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { currentCategory ->
            SettingsCompactBody(
                currentCategory = currentCategory,
                categories = categories,
                entries = entries,
                settings = settings,
                isLoggedIn = isLoggedIn,
                searchQuery = searchQuery,
                isSearching = isSearching,
                updateState = updateState,
                detailBackProgress = detailBackProgress,
                onSelectCategory = onSelectCategory,
            )
        }
    }
}

@Composable
private fun SettingsCompactBody(
    currentCategory: SettingsCategory?,
    categories: List<SettingsCategory>,
    entries: List<SettingEntry>,
    settings: Settings,
    isLoggedIn: Boolean,
    searchQuery: String,
    isSearching: Boolean,
    updateState: UpdateState?,
    detailBackProgress: Float,
    onSelectCategory: (SettingsCategory) -> Unit,
) {
    if (currentCategory == null) {
        key(isSearching) {
            val listScrollState = rememberScrollState()
            if (isSearching) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                state = listScrollState,
                                enabled = listScrollState.maxValue > 0,
                            ).padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    SettingSearchResultsPane(
                        entries = entries,
                        query = searchQuery,
                        onOpenCategory = onSelectCategory,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                SettingsCategoryList(
                    categories = categories,
                    settings = settings,
                    isLoggedIn = isLoggedIn,
                    updateState = updateState,
                    onSelectCategory = onSelectCategory,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
    } else {
        key(currentCategory) {
            val detailScrollState = rememberScrollState()
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (detailBackProgress > 0f) {
                                translationX = detailBackProgress * size.width * 0.25f
                                alpha = 1f - detailBackProgress * 0.2f
                            }
                        }.verticalScroll(
                            state = detailScrollState,
                            enabled = detailScrollState.maxValue > 0,
                        ).padding(horizontal = 20.dp, vertical = 8.dp),
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

@Composable
private fun <T> rememberHoistedState(
    initial: T,
    externalValue: T,
    onExternalChange: ((T) -> Unit)?,
): Pair<T, (T) -> Unit> {
    var internalState by rememberSaveable { mutableStateOf(initial) }
    val value = if (onExternalChange != null) externalValue else internalState
    val setter: (T) -> Unit = { next ->
        if (onExternalChange != null) {
            onExternalChange(next)
        } else {
            internalState = next
        }
    }
    return value to setter
}

@Composable
private fun SettingsCompactTopBar(
    selectedCategory: SettingsCategory?,
    searchQuery: String,
    isSearchOpen: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onBackFromCategory: () -> Unit,
    onClose: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (selectedCategory == null && isSearchOpen) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            IconButton(
                onClick = onCloseSearch,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.settings_search_close),
                    tint = MeloColors.textPrimary,
                )
            }

            SettingsSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                focusRequester = focusRequester,
                showInternalClearButton = false,
                onClearOrEscape = onCloseSearch,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = {
                    if (searchQuery.isNotEmpty()) {
                        onSearchQueryChange("")
                    } else {
                        onCloseSearch()
                    }
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription =
                        stringResource(
                            if (searchQuery.isNotEmpty()) {
                                Res.string.settings_search_clear
                            } else {
                                Res.string.settings_search_close
                            },
                        ),
                    tint = MeloColors.textSecondary,
                )
            }
        } else if (selectedCategory != null) {
            IconButton(
                onClick = onBackFromCategory,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.settings_back_to_categories),
                    tint = MeloColors.textPrimary,
                )
            }

            Text(
                text = stringResource(selectedCategory.titleRes),
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.settings_close),
                        tint = MeloColors.textPrimary,
                    )
                }
            }

            Text(
                text = stringResource(Res.string.settings_title),
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = if (onClose == null) 12.dp else 0.dp),
            )

            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(Res.string.settings_search_placeholder),
                    tint = MeloColors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun SettingsExpandedDetailHeader(
    trailingSegment: String?,
    onClose: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val settingsTitle = stringResource(Res.string.settings_title)
        val breadcrumbPrefix = stringResource(Res.string.settings_breadcrumb_prefix)
        val headerText =
            if (trailingSegment == null) {
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MeloColors.textPrimary, fontWeight = FontWeight.Bold)) {
                        append(settingsTitle)
                    }
                }
            } else {
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MeloColors.textMuted, fontWeight = FontWeight.Medium)) {
                        append(breadcrumbPrefix)
                    }
                    withStyle(SpanStyle(color = MeloColors.textPrimary, fontWeight = FontWeight.Bold)) {
                        append(trailingSegment)
                    }
                }
            }

        Text(
            text = headerText,
            fontSize = 20.sp,
            lineHeight = 26.sp,
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
                    contentDescription = stringResource(Res.string.common_action_close),
                    tint = MeloColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsEmptyCategoryPane() {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = primary.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Text(
            text = stringResource(Res.string.settings_empty_title),
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = MeloColors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(Res.string.settings_empty_subtitle),
            style = MeloType.labelSmall,
            color = MeloColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp),
        )
    }
}
