package com.immodi.webdock.ui.home

import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.immodi.webdock.AppContainer
import com.immodi.webdock.R
import com.immodi.webdock.data.prefs.HomeView
import com.immodi.webdock.domain.WebApp
import com.immodi.webdock.ui.components.WebAppIcon
import com.immodi.webdock.ui.components.hostLabel
import com.immodi.webdock.ui.theme.WebDockTheme
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun HomeRoute(
    container: AppContainer,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onOpen: (WebApp) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            repository = container.webAppRepository,
            settingsRepository = container.settingsRepository,
            metadataRepository = container.metadataRepository,
            profileManager = container.profileManager,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onQueryChange = viewModel::setQuery,
        onToggleTag = viewModel::toggleTag,
        onToggleView = viewModel::toggleView,
        onAdd = onAdd,
        onEdit = onEdit,
        onOpen = { app ->
            viewModel.markOpened(app)
            onOpen(app)
        },
        onTogglePin = { app -> viewModel.setPinned(app, !app.pinned) },
        onRefreshMetadata = viewModel::refreshMetadata,
        onClearData = viewModel::clearData,
        onDelete = viewModel::delete,
        onMove = viewModel::moveUnpinned,
        onOpenSettings = onOpenSettings,
        profileSupported = container.profileManager.isMultiProfileSupported(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleView: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onOpen: (WebApp) -> Unit,
    onTogglePin: (WebApp) -> Unit,
    onRefreshMetadata: (WebApp) -> Unit,
    onClearData: (WebApp) -> Unit,
    onDelete: (WebApp) -> Unit,
    onMove: (Int, Int) -> Unit,
    onOpenSettings: () -> Unit,
    profileSupported: Boolean,
) {
    var menuAppId by remember { mutableStateOf<String?>(null) }
    var deleteApp by remember { mutableStateOf<WebApp?>(null) }
    var clearDataApp by remember { mutableStateOf<WebApp?>(null) }
    val menuApp = state.apps.firstOrNull { it.id == menuAppId }
    val reorderState = remember { ReorderState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                    TextButton(onClick = onToggleView) {
                        Text(
                            stringResource(
                                if (state.view == HomeView.GRID) R.string.view_list else R.string.view_grid,
                            ),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_app))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )

            if (state.allTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.allTags, key = { it }) { tag ->
                        FilterChip(
                            selected = state.selectedTags.any { it.equals(tag, ignoreCase = true) },
                            onClick = { onToggleTag(tag) },
                            label = { Text(tag) },
                        )
                    }
                }
            }

            when {
                state.totalCount == 0 -> EmptyState(
                    title = stringResource(R.string.empty_state_title),
                    subtitle = stringResource(R.string.empty_state_subtitle),
                    actionLabel = stringResource(R.string.add_app),
                    onAction = onAdd,
                )

                state.apps.isEmpty() -> EmptyState(
                    title = stringResource(R.string.no_matches_title),
                    subtitle = stringResource(R.string.no_matches_subtitle),
                )

                state.view == HomeView.GRID -> AppGrid(
                    state = state,
                    reorderState = reorderState,
                    onClick = onOpen,
                    onLongClick = { app -> menuAppId = app.id },
                    onMove = onMove,
                )

                else -> AppList(
                    state = state,
                    reorderState = reorderState,
                    onClick = onOpen,
                    onLongClick = { app -> menuAppId = app.id },
                    onMove = onMove,
                )
            }
        }
    }

    menuApp?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { menuAppId = null },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WebAppIcon(
                        name = app.name,
                        host = hostLabel(app.url),
                        iconUrl = app.iconUrl,
                        iconFile = app.iconFile,
                        size = 48.dp,
                    )
                    Column {
                        Text(app.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            hostLabel(app.url),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SheetAction(
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    label = stringResource(R.string.action_open),
                ) {
                    menuAppId = null
                    onOpen(app)
                }
                SheetAction(
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    label = stringResource(R.string.action_edit),
                ) {
                    menuAppId = null
                    onEdit(app.id)
                }
                SheetAction(
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    label = stringResource(R.string.action_refresh_metadata),
                ) {
                    menuAppId = null
                    onRefreshMetadata(app)
                }
                SheetAction(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = stringResource(
                        if (app.pinned) R.string.action_unpin else R.string.action_pin,
                    ),
                ) {
                    menuAppId = null
                    onTogglePin(app)
                }
                SheetAction(
                    icon = { Icon(Icons.Default.Clear, contentDescription = null) },
                    label = stringResource(R.string.clear_data),
                ) {
                    menuAppId = null
                    clearDataApp = app
                }
                SheetAction(
                    icon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    label = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                ) {
                    menuAppId = null
                    deleteApp = app
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    clearDataApp?.let { app ->
        val shared = !app.settings.isolateStorage || !profileSupported
        AlertDialog(
            onDismissRequest = { clearDataApp = null },
            title = { Text(stringResource(R.string.clear_data_title)) },
            text = {
                Text(
                    stringResource(
                        if (shared) R.string.clear_data_message_shared else R.string.clear_data_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearDataApp = null
                        onClearData(app)
                    },
                ) {
                    Text(
                        stringResource(R.string.clear_data),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { clearDataApp = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    deleteApp?.let { app ->
        AlertDialog(
            onDismissRequest = { deleteApp = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, app.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteApp = null
                        onDelete(app)
                    },
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteApp = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: @Composable () -> Unit,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AppGrid(
    state: HomeUiState,
    reorderState: ReorderState,
    onClick: (WebApp) -> Unit,
    onLongClick: (WebApp) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val unpinned = state.apps.filterNot { it.pinned }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.apps, key = { it.id }) { app ->
            WebAppGridCard(
                app = app,
                draggable = state.canReorder && !app.pinned,
                reorderState = reorderState,
                onClick = { onClick(app) },
                onLongClick = { onLongClick(app) },
                onDragEnd = { offset ->
                    val from = unpinned.indexOfFirst { it.id == app.id }
                    val to = findDropIndex(app.id, unpinned, reorderState.bounds, offset)
                    if (from >= 0 && to != null && from != to) onMove(from, to)
                },
            )
        }
    }
}

@Composable
private fun AppList(
    state: HomeUiState,
    reorderState: ReorderState,
    onClick: (WebApp) -> Unit,
    onLongClick: (WebApp) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val unpinned = state.apps.filterNot { it.pinned }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.apps, key = { it.id }) { app ->
            WebAppListRow(
                app = app,
                draggable = state.canReorder && !app.pinned,
                reorderState = reorderState,
                onClick = { onClick(app) },
                onLongClick = { onLongClick(app) },
                onDragEnd = { offset ->
                    val from = unpinned.indexOfFirst { it.id == app.id }
                    val to = findDropIndex(app.id, unpinned, reorderState.bounds, offset)
                    if (from >= 0 && to != null && from != to) onMove(from, to)
                },
            )
        }
    }
}

@Composable
private fun WebAppGridCard(
    app: WebApp,
    draggable: Boolean,
    reorderState: ReorderState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragEnd: (Offset) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .onGloballyPositioned { coordinates ->
                reorderState.bounds[app.id] = Rect(
                    offset = coordinates.localToRoot(Offset.Zero),
                    size = Size(
                        width = coordinates.size.width.toFloat(),
                        height = coordinates.size.height.toFloat(),
                    ),
                )
            }
            .then(
                Modifier.reorderModifier(
                    appId = app.id,
                    draggable = draggable,
                    state = reorderState,
                    haptics = haptics,
                    onClick = { currentOnClick() },
                    onLongClick = { currentOnLongClick() },
                    onDragEnd = { currentOnDragEnd(it) },
                ),
            )
            .zIndex(if (reorderState.draggingId == app.id) 1f else 0f)
            .graphicsLayer {
                if (reorderState.draggingId == app.id) {
                    translationX = reorderState.offset.x
                    translationY = reorderState.offset.y
                    scaleX = 1.03f
                    scaleY = 1.03f
                    shadowElevation = 12.dp.toPx()
                }
            },
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box {
                WebAppIcon(
                    name = app.name,
                    host = hostLabel(app.url),
                    iconUrl = app.iconUrl,
                    iconFile = app.iconFile,
                )
                if (app.pinned) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = stringResource(R.string.pinned),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WebAppListRow(
    app: WebApp,
    draggable: Boolean,
    reorderState: ReorderState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragEnd: (Offset) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                reorderState.bounds[app.id] = Rect(
                    offset = coordinates.localToRoot(Offset.Zero),
                    size = Size(
                        width = coordinates.size.width.toFloat(),
                        height = coordinates.size.height.toFloat(),
                    ),
                )
            }
            .then(
                Modifier.reorderModifier(
                    appId = app.id,
                    draggable = draggable,
                    state = reorderState,
                    haptics = haptics,
                    onClick = { currentOnClick() },
                    onLongClick = { currentOnLongClick() },
                    onDragEnd = { currentOnDragEnd(it) },
                ),
            )
            .zIndex(if (reorderState.draggingId == app.id) 1f else 0f)
            .graphicsLayer {
                if (reorderState.draggingId == app.id) {
                    translationX = reorderState.offset.x
                    translationY = reorderState.offset.y
                    scaleX = 1.02f
                    scaleY = 1.02f
                    shadowElevation = 10.dp.toPx()
                }
            },
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WebAppIcon(
                name = app.name,
                host = hostLabel(app.url),
                iconUrl = app.iconUrl,
                iconFile = app.iconFile,
                size = 48.dp,
            )
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (app.pinned) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = stringResource(R.string.pinned),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.reorderModifier(
    appId: String,
    draggable: Boolean,
    state: ReorderState,
    haptics: HapticFeedback,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragEnd: (Offset) -> Unit,
): Modifier {
    if (!draggable) {
        return this.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }
    return this.pointerInput(appId) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
            val touchSlop = viewConfiguration.touchSlop
            val downTime = SystemClock.uptimeMillis()
            var movement = Offset.Zero
            var dragTotal = Offset.Zero
            var longPressed = false

            while (true) {
                val event = if (longPressed) {
                    awaitPointerEvent()
                } else {
                    val remaining = (longPressTimeout - (SystemClock.uptimeMillis() - downTime))
                        .coerceAtLeast(1L)
                    withTimeoutOrNull(remaining) { awaitPointerEvent() }
                }

                if (event == null) {
                    longPressed = true
                    dragTotal = Offset.Zero
                    state.draggingId = appId
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    continue
                }

                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                if (!change.pressed) {
                    if (longPressed) {
                        if (dragTotal.getDistance() < touchSlop) onLongClick()
                    } else if (movement.getDistance() < touchSlop) {
                        onClick()
                    }
                    break
                }

                val delta = change.position - change.previousPosition
                if (!longPressed) {
                    movement += delta
                    if (movement.getDistance() > touchSlop) break
                } else {
                    change.consume()
                    dragTotal += delta
                    state.offset += delta
                }
            }

            if (longPressed) {
                if (dragTotal.getDistance() >= touchSlop) onDragEnd(dragTotal)
                state.reset()
            }
        }
    }
}

@Stable
private class ReorderState {
    var draggingId by mutableStateOf<String?>(null)
    var offset by mutableStateOf(Offset.Zero)
    val bounds = mutableStateMapOf<String, Rect>()

    fun reset() {
        draggingId = null
        offset = Offset.Zero
    }
}

private fun findDropIndex(
    draggedId: String,
    unpinned: List<WebApp>,
    bounds: Map<String, Rect>,
    dragOffset: Offset,
): Int? {
    val draggedBounds = bounds[draggedId] ?: return null
    val center = draggedBounds.center + dragOffset
    var closestIndex: Int? = null
    var closestDistance = Float.MAX_VALUE

    unpinned.forEachIndexed { index, app ->
        val rect = bounds[app.id] ?: return@forEachIndexed
        if (rect.contains(center)) return index
        val distance = (rect.center - center).getDistanceSquared()
        if (distance < closestDistance) {
            closestDistance = distance
            closestIndex = index
        }
    }
    return closestIndex
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    WebDockTheme {
        HomeScreen(
            state = HomeUiState(
                apps = listOf(
                    WebApp(url = "https://trade-journel.pages.dev/", name = "Trade Journal", pinned = true),
                    WebApp(url = "https://futures-playbook.pages.dev/", name = "Futures Playbook"),
                ),
                totalCount = 2,
                allTags = listOf("trading", "notes"),
            ),
            onQueryChange = {},
            onToggleTag = {},
            onToggleView = {},
            onAdd = {},
            onEdit = {},
            onOpen = {},
            onTogglePin = {},
            onRefreshMetadata = {},
            onClearData = {},
            onDelete = {},
            onMove = { _, _ -> },
            onOpenSettings = {},
            profileSupported = true,
        )
    }
}
