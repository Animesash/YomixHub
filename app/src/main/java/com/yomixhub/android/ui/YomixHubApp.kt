package com.yomixhub.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yomixhub.android.R
import com.yomixhub.android.data.AppState
import com.yomixhub.android.data.DownloadStatus
import com.yomixhub.android.data.Title
import com.yomixhub.android.data.source.SourceChapter
import com.yomixhub.android.ui.components.ActiveSource
import com.yomixhub.android.ui.components.SourcePickerSheet
import com.yomixhub.android.ui.screens.DownloadsScreen
import com.yomixhub.android.ui.screens.ExploreScreen
import com.yomixhub.android.ui.screens.LibraryScreen
import com.yomixhub.android.ui.screens.NotificationsScreen
import com.yomixhub.android.ui.screens.NovelDetailsScreen
import com.yomixhub.android.ui.screens.ReaderScreen
import com.yomixhub.android.ui.screens.SettingsScreen

/** Bottom navigation destinations. */
enum class HomeTab { LIBRARY, EXPLORE, NOTIFICATIONS, DOWNLOADS }

/** An open chapter in the reader: what to show and where it came from. */
data class ReaderTarget(
    val title: Title,
    val chapter: SourceChapter,
)

/**
 * Root screen: compact top app bar + tabbed content + MD3 navigation bar.
 *
 * The search field state ([searchActive] / [searchQuery]) is hoisted here and
 * shared by every tab, so search works across «Закладки», «Все тайтлы»,
 * «Уведомления» and «Загрузки» and the query follows the user between tabs.
 *
 * The settings, details and reader screens are hoisted here as full-screen
 * overlays so they can be opened from any tab and stay alive while the user
 * moves between them.
 *
 * The [Scaffold] exposes `innerPadding` to the screens, and each lazy list /
 * grid consumes it as `contentPadding` so content scrolls behind the bars
 * (edge-to-edge) instead of being squeezed by an outer `padding` modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YomixHubApp() {
    var currentTab by remember { mutableStateOf(HomeTab.EXPLORE) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSource by remember { mutableStateOf(ActiveSource.ALL) }
    var sourcePickerVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var openTitle by remember { mutableStateOf<Title?>(null) }
    var openChapter by remember { mutableStateOf<ReaderTarget?>(null) }

    fun openDetails(title: Title) {
        if (title.detailsSlug != null) openTitle = title
    }

    fun readChapter(title: Title, chapter: SourceChapter) {
        openChapter = ReaderTarget(title = title, chapter = chapter)
    }

    // Closing the search (icon or X) clears the query on every tab.
    LaunchedEffect(searchActive) {
        if (!searchActive && searchQuery.isNotEmpty()) searchQuery = ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                YomixTopAppBar(
                    searchActive = searchActive,
                    onToggleSearch = { searchActive = !searchActive },
                    onOpenSettings = { settingsVisible = true },
                )
            },
            bottomBar = {
                YomixBottomBar(
                    selectedTab = currentTab,
                    onSelect = { currentTab = it },
                )
            },
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val slide = if (forward) {
                        slideInVertically(tween(220)) { it / 16 } + fadeIn(tween(220))
                    } else {
                        slideInVertically(tween(220)) { -it / 16 } + fadeIn(tween(220))
                    }
                    slide togetherWith fadeOut(tween(160))
                },
                label = "home-tabs",
            ) { tab ->
                when (tab) {
                    HomeTab.LIBRARY -> LibraryScreen(
                        innerPadding = innerPadding,
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSearchActiveChange = { searchActive = it },
                        onOpenTitle = ::openDetails,
                    )
                    HomeTab.EXPLORE -> ExploreScreen(
                        innerPadding = innerPadding,
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSearchActiveChange = { searchActive = it },
                        activeSource = activeSource,
                        onOpenTitle = ::openDetails,
                    )
                    HomeTab.NOTIFICATIONS -> NotificationsScreen(
                        innerPadding = innerPadding,
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSearchActiveChange = { searchActive = it },
                    )
                    HomeTab.DOWNLOADS -> DownloadsScreen(
                        innerPadding = innerPadding,
                        onOpenDownload = { entry ->
                            readChapter(entry.title, entry.chapterRef)
                        },
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSearchActiveChange = { searchActive = it },
                    )
                }
            }
        }

        // Full-screen overlays, stacked: settings on top of the tabs, the
        // details/reader pair above it (back returns to the details with its
        // scroll position intact).
        if (settingsVisible) {
            SettingsScreen(
                activeSource = activeSource,
                onBack = { settingsVisible = false },
                onOpenSourcePicker = { sourcePickerVisible = true },
            )
        }

        openTitle?.let { title ->
            Box(modifier = Modifier.fillMaxSize()) {
                NovelDetailsScreen(
                    title = title,
                    onBack = { openTitle = null },
                    onReadChapter = { chapter -> readChapter(title, chapter) },
                )
            }
        }

        openChapter?.let { target ->
            Box(modifier = Modifier.fillMaxSize()) {
                ReaderScreen(
                    chapter = target.chapter,
                    novelTitle = target.title.name,
                    onBack = { openChapter = null },
                )
            }
        }

        // The source picker is a modal bottom sheet: opened from Settings and
        // rendered above everything.
        if (sourcePickerVisible) {
            SourcePickerSheet(
                selected = activeSource,
                onSelect = { activeSource = it },
                onDismiss = { sourcePickerVisible = false },
            )
        }
    }
}

/**
 * Compact top app bar (no title): the search action toggles the shared
 * search field on the current tab, the gear opens the settings overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YomixTopAppBar(
    searchActive: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        title = {},
        actions = {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (searchActive) YomixIcons.Close else YomixIcons.Search,
                    contentDescription = stringResource(R.string.cd_search),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = YomixIcons.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * MD3 navigation bar, exactly per the Material 3 spec:
 * 80dp tall (the component's own intrinsic height), `surfaceContainer`
 * container color and an active indicator pill in `secondaryContainer`.
 *
 * System bars:
 *  * [Modifier.navigationBarsPadding] keeps the bar above the gesture /
 *    button navigation area on Android 10+ (and gesture nav on 14+),
 *  * the component's own `windowInsets` are zeroed so the inset is applied
 *    exactly once – with a forced 80dp height plus default insets the labels
 *    and the active pill used to get clipped.
 *
 * Badges are derived from [AppState]: unread notifications on the bell and a
 * progress ring on the downloads icon while a download is running.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YomixBottomBar(
    selectedTab: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        val unreadCount = AppState.notifications.count { it.isUnread }
        val activeDownloadProgress = AppState.downloads
            .firstOrNull { it.status == DownloadStatus.ACTIVE }
            ?.progress

        HomeTab.entries.forEach { tab ->
            val selected = tab == selectedTab

            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    val icon = when (tab) {
                        HomeTab.LIBRARY ->
                            if (selected) YomixIcons.BookmarkFilled else YomixIcons.BookmarkOutlined
                        HomeTab.EXPLORE ->
                            if (selected) YomixIcons.GridFilled else YomixIcons.GridOutlined
                        HomeTab.NOTIFICATIONS ->
                            if (selected) YomixIcons.NotificationsFilled else YomixIcons.NotificationsOutlined
                        HomeTab.DOWNLOADS ->
                            if (selected) YomixIcons.DownloadFilled else YomixIcons.DownloadOutlined
                    }

                    Box {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        when (tab) {
                            HomeTab.NOTIFICATIONS -> if (unreadCount > 0) {
                                UnreadCountBadge(
                                    count = unreadCount,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp),
                                )
                            }

                            HomeTab.DOWNLOADS -> if (activeDownloadProgress != null) {
                                DownloadProgressBadge(
                                    progress = activeDownloadProgress,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-6).dp),
                                )
                            }

                            else -> Unit
                        }
                    }
                },
                label = { Text(text = stringResource(tab.labelRes())) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private fun HomeTab.labelRes(): Int = when (this) {
    HomeTab.LIBRARY -> R.string.tab_library
    HomeTab.EXPLORE -> R.string.tab_explore
    HomeTab.NOTIFICATIONS -> R.string.tab_notifications
    HomeTab.DOWNLOADS -> R.string.tab_downloads
}

/**
 * Subtle red MD3 badge with the number of unread chapters,
 * floating over the top-end corner of the bell icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnreadCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    ) {
        Text(text = count.toString())
    }
}

/**
 * Mini circular progress indicator for active background downloads,
 * shown instead of a numeric badge.
 */
@Composable
private fun DownloadProgressBadge(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(16.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = 2.dp,
            strokeCap = StrokeCap.Round,
        )
    }
}
