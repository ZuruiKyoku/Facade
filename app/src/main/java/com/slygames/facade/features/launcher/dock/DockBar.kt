package com.slygames.facade.features.launcher.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slygames.facade.core.util.toImageBitmap
import com.slygames.facade.data.model.AppItem
import com.slygames.facade.data.model.WorkspaceItem

/**
 * The Compose-driven bottom dock: a fixed row of [DockUiState.slotCount]
 * pinned app slots, rendered above the workspace's coordinate-based grid
 * (see `features.launcher.workspace.WorkspaceGridView`) but sharing the same
 * [WorkspaceItem] vocabulary conceptually - the dock intentionally stays in
 * Compose since it never needs 60fps nested-widget drag, only tap and
 * reorder-by-long-press.
 */
@Composable
fun DockBar(
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (WorkspaceItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DockViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.slots().forEach { item ->
                DockSlot(
                    item = item,
                    onClick = { (item as? WorkspaceItem.App)?.appItem?.let(onAppClick) },
                    onLongClick = { item?.let(onAppLongClick) }
                )
            }
        }
    }
}

@Composable
private fun DockSlot(
    item: WorkspaceItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = Color.Transparent
    ) {
        val appItem = (item as? WorkspaceItem.App)?.appItem
        val icon = appItem?.icon
        if (icon != null) {
            Image(
                bitmap = icon.toImageBitmap(),
                contentDescription = appItem.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
    }
}
