package com.slygames.facade.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val FacadeShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Shape tokens used only by the non-Compose workspace grid (folder previews). */
object WorkspaceShapes {
    val FolderPreviewCorner = 20.dp
    val IconBadgeCorner = 8.dp
}
