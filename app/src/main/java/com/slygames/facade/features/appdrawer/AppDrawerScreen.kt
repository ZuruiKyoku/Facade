package com.slygames.facade.features.appdrawer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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

    // Drag-out-to-add: the app drawer and workspace are separate NavHost destinations (the
    // workspace isn't even composed while this screen is up), so there's no shared view to drop
    // onto - a long-press-drag here just picks up a floating ghost of the icon, and releasing it
    // (past a small movement threshold, so a stationary long-press is a no-op rather than an
    // accidental add) hands the app straight to the repository and dismisses back to the
    // workspace, which picks up the new placement reactively once it's on screen again.
    var draggedApp by remember { mutableStateOf<AppItem?>(null) }
    var ghostPosition by remember { mutableStateOf(Offset.Zero) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val ghostSizePx = with(density) { GHOST_ICON_SIZE_DP.dp.toPx() }
    val dragConfirmThresholdPx = with(density) { 24.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
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
                                iconScale = uiState.iconScale,
                                onClick = { IntentDispatcher.launchApp(context, app.packageName, app.activityName) },
                                onDragStart = { originInRoot, pointerOffset ->
                                    draggedApp = app
                                    dragDistance = 0f
                                    ghostPosition = originInRoot + pointerOffset -
                                        Offset(ghostSizePx / 2f, ghostSizePx / 2f)
                                },
                                onDrag = { dragAmount ->
                                    ghostPosition += dragAmount
                                    dragDistance += dragAmount.getDistance()
                                },
                                onDragEnd = {
                                    if (dragDistance >= dragConfirmThresholdPx) {
                                        viewModel.addAppToHomeScreen(app)
                                        onDismiss()
                                    }
                                    draggedApp = null
                                },
                                onDragCancel = { draggedApp = null }
                            )
                        }
                    }
                }
            }

            FastScrollIndex(
                letters = uiState.sectionIndex,
                onLetterSelected = { letter ->
                    uiState.sectionStartIndex[letter]?.let { index ->
                        coroutineScope.launch { gridState.animateScrollToItem(index) }
                    }
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
            )
        }

        draggedApp?.let { app ->
            app.icon?.let { icon ->
                Image(
                    bitmap = icon.toImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .offset { IntOffset(ghostPosition.x.roundToInt(), ghostPosition.y.roundToInt()) }
                        .size(GHOST_ICON_SIZE_DP.dp)
                        .scale(1.15f)
                        .alpha(0.9f)
                )
            }
        }
    }
}

private const val GHOST_ICON_SIZE_DP = 56

/**
 * Tap launches the app as always. Long-press picks it up for a drag (see
 * [AppDrawerScreen]'s ghost overlay) - a plain long-press-then-release
 * without moving is a no-op rather than opening app info, since between
 * this and the fast-scroll rail there's no spare gesture left to carry
 * that separately; app info remains reachable the normal system way.
 */
@Composable
private fun AppDrawerCell(
    app: AppItem,
    iconScale: Float,
    onClick: () -> Unit,
    onDragStart: (originInRoot: Offset, pointerOffsetInCell: Offset) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var originInRoot by remember { mutableStateOf(Offset.Zero) }
    Column(
        modifier = Modifier
            .padding(8.dp)
            .onGloballyPositioned { originInRoot = it.positionInRoot() }
            .pointerInput(app) {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(app) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDragStart(originInRoot, offset) },
                    onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = onDragCancel
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        app.icon?.let { icon ->
            Image(
                bitmap = icon.toImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .scale(iconScale)
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

/**
 * Alphabet rail on the right edge of the drawer for jump-scrolling by
 * section letter: tap a letter, or drag anywhere along the rail, to
 * animate-scroll the grid to that section - same interaction as a
 * contacts-app A-Z index. [onLetterSelected] fires on initial touch-down
 * and again each time the touch crosses into a new letter's band while
 * dragging, not continuously, so it doesn't spam redundant scroll calls.
 */
@Composable
private fun FastScrollIndex(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var activeLetterY by remember { mutableStateOf(0.dp) }
    var railHeightPx by remember { mutableFloatStateOf(0f) }

    fun letterForY(yPx: Float): Char? {
        if (letters.isEmpty() || railHeightPx <= 0f) return null
        val fraction = (yPx / railHeightPx).coerceIn(0f, 0.999f)
        return letters[(fraction * letters.size).toInt()]
    }

    // The outer Box is given the caller's fixed size explicitly (rather than the inner Column)
    // so it never wraps to fit the callout bubble below - that bubble deliberately renders
    // outside these bounds via a negative offset, and without a size pinned here Box would
    // otherwise size itself to include it, growing wider the moment a letter goes active and
    // shoving the rail (and the grid next to it, in the parent Row) sideways to make room.
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { railHeightPx = it.height.toFloat() }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp)
                .pointerInput(letters) {
                    awaitEachGesture {
                        do {
                            // The loop's first iteration picks up the initial press itself, so
                            // there's no separate awaitFirstDown() step - tap and drag are
                            // handled identically, just as one iteration vs. several.
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.pressed } ?: break
                            change.consume()
                            letterForY(change.position.y)?.let { letter ->
                                activeLetterY = change.position.y.toDp()
                                if (letter != activeLetter) {
                                    activeLetter = letter
                                    onLetterSelected(letter)
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        activeLetter = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                val isActive = letter == activeLetter
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Unspecified
                )
            }
        }

        // Large callout bubble tracking the touch, so the picked letter is readable without the
        // finger obscuring the (much smaller) rail text it's currently over.
        activeLetter?.let { letter ->
            Box(
                modifier = Modifier
                    .offset(x = (-64).dp, y = activeLetterY - 28.dp)
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
