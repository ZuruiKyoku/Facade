package com.slygames.facade.features.wallpaper

import android.app.WallpaperManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

/** Minutes between automatic switches; index 0 ("Off") means the pool never rotates on its own. */
private val SHUFFLE_INTERVAL_PRESETS_MINUTES = listOf(0, 1, 5, 15, 30, 60)
private const val MAX_SELECTABLE_VIDEOS = 20

private fun shuffleIntervalLabel(minutes: Int): String = when (minutes) {
    0 -> "Off"
    60 -> "1 hour"
    else -> "$minutes min"
}

@Composable
fun WallpaperPickerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WallpaperPickerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The system's own crop-and-set flow does the actual WallpaperManager.setBitmap/setStream
    // call on success (cropping UI, permission handling, static-wallpaper persistence all live
    // there) - Facade only needs to hand it a URI and get out of the way. Picking a photo this
    // way also implicitly clears any active live (video) wallpaper, same as any other launcher.
    val cropAndSetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val cropIntent = WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
            cropAndSetLauncher.launch(cropIntent)
        }
    }

    // The system Photo Picker needs no runtime permission at all (it grants access only to what
    // the user taps), shows real thumbnails, and supports multi-select natively - all three
    // things the old in-app MediaStore-query grid couldn't do without a lot more code.
    val pickVideosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTABLE_VIDEOS)
    ) { uris -> viewModel.addSelectedVideos(uris) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = { Text("Choose photo") },
                supportingContent = { Text("Pick a picture from your gallery") },
                leadingContent = { Icon(Icons.Filled.Photo, contentDescription = null) },
                modifier = Modifier.clickable {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = {
                    Text(
                        "Live video wallpaper",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Choose videos") },
                supportingContent = { Text("Pick one or more from your gallery - selecting several lets you shuffle between them") },
                leadingContent = { Icon(Icons.Filled.VideoLibrary, contentDescription = null) },
                modifier = Modifier.clickable {
                    pickVideosLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )

            if (state.preferences.selectedMediaUris.isNotEmpty()) {
                SelectedVideosRow(
                    uris = state.preferences.selectedMediaUris,
                    thumbnails = state.thumbnails,
                    onRemove = viewModel::removeSelectedVideo
                )

                ListItem(
                    headlineContent = { Text("Mute audio") },
                    trailingContent = {
                        Switch(checked = state.preferences.muted, onCheckedChange = viewModel::setMuted)
                    }
                )
                ListItem(
                    headlineContent = { Text("Loop playback") },
                    trailingContent = {
                        Switch(checked = state.preferences.loop, onCheckedChange = viewModel::setLoop)
                    }
                )

                if (state.preferences.selectedMediaUris.size > 1) {
                    val currentIndex = SHUFFLE_INTERVAL_PRESETS_MINUTES
                        .indexOf(state.preferences.shuffleIntervalMinutes)
                        .coerceAtLeast(0)
                    ListItem(
                        headlineContent = {
                            Text("Shuffle every (${shuffleIntervalLabel(state.preferences.shuffleIntervalMinutes)})")
                        },
                        supportingContent = {
                            Slider(
                                value = currentIndex.toFloat(),
                                valueRange = 0f..(SHUFFLE_INTERVAL_PRESETS_MINUTES.size - 1).toFloat(),
                                steps = SHUFFLE_INTERVAL_PRESETS_MINUTES.size - 2,
                                onValueChange = {
                                    viewModel.setShuffleIntervalMinutes(SHUFFLE_INTERVAL_PRESETS_MINUTES[it.roundToInt()])
                                }
                            )
                        }
                    )
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = { context.startActivity(viewModel.buildActivateWallpaperIntent()) }
                ) {
                    Text("Set as home screen wallpaper")
                }
            }
        }
    }
}

@Composable
private fun SelectedVideosRow(
    uris: Set<String>,
    thumbnails: Map<String, android.graphics.Bitmap?>,
    onRemove: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(uris.toList(), key = { it }) { uriString ->
            VideoThumbnailCell(
                thumbnail = thumbnails[uriString],
                isThumbnailLoading = !thumbnails.containsKey(uriString),
                onRemove = { onRemove(uriString) }
            )
        }
    }
}

@Composable
private fun VideoThumbnailCell(
    thumbnail: android.graphics.Bitmap?,
    isThumbnailLoading: Boolean,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.padding(4.dp).size(88.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                thumbnail != null -> Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                isThumbnailLoading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else -> Icon(Icons.Filled.VideoLibrary, contentDescription = null)
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(22.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
