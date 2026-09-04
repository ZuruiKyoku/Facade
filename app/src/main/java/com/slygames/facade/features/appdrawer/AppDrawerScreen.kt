package com.slygames.facade.features.appdrawer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slygames.facade.R
import com.slygames.facade.core.util.IntentDispatcher
import com.slygames.facade.core.util.toImageBitmap
import com.slygames.facade.data.model.AppItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Searchable, alphabetically fast-scrollable app drawer. Built entirely in
 * Compose (`LazyVerticalGrid`) per the hybrid rendering split - unlike the
 * workspace, the drawer's contents are a flat, non-reorderable list, which
 * is exactly what `LazyVerticalGrid` recycling is good at.
 */
@Composable
fun AppDrawerScreen(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppDrawerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()

    // Pull-down-to-dismiss: only engages once the grid can't scroll up any further, so it never
    // fights normal scrolling through the app list. Rubber-bands while dragging and either snaps
    // back or dismisses on release, depending on how far past the top the user pulled.
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pullOffset = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 96.dp.toPx() }

    fun onPullEnded() {
        if (pullOffset.value >= dismissThresholdPx) {
            onDismiss()
        } else {
            coroutineScope.launch { pullOffset.animateTo(0f) }
        }
    }

    val gridNestedScrollConnection = remember(gridState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && !gridState.canScrollBackward) {
                    coroutineScope.launch { pullOffset.snapTo((pullOffset.value + available.y).coerceAtLeast(0f)) }
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                onPullEnded()
                return Velocity.Zero
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .offset { IntOffset(0, pullOffset.value.roundToInt()) }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.app_drawer_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }

            if (uiState.filteredApps.isEmpty() && !uiState.isLoading) {
                val emptyStateDragState = rememberDraggableState { delta ->
                    if (delta > 0) {
                        coroutineScope.launch { pullOffset.snapTo((pullOffset.value + delta).coerceAtLeast(0f)) }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            state = emptyStateDragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { onPullEnded() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No apps found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(gridNestedScrollConnection),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(uiState.filteredApps, key = { it.componentKey }) { app ->
                        AppDrawerCell(
                            app = app,
                            onClick = { IntentDispatcher.launchApp(context, app.packageName, app.activityName) },
                            onLongClick = { IntentDispatcher.openAppInfo(context, app.packageName) }
                        )
                    }
                }
            }
        }

        FastScrollIndex(
            letters = uiState.sectionIndex,
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
        )
    }
}

@Composable
private fun AppDrawerCell(
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        app.icon?.let { icon ->
            Image(
                bitmap = icon.toImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Alphabet rail on the right edge of the drawer for jump-scrolling by section letter. */
@Composable
private fun FastScrollIndex(letters: List<Char>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Unspecified
            )
        }
    }
}
