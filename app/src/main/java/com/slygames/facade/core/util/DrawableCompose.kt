package com.slygames.facade.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Rasterizes a platform [Drawable] (an app icon, an icon-pack drawable, a
 * widget preview) into an [ImageBitmap] for use in `Image(bitmap = ...)`
 * inside the Compose app drawer / dock. Facade avoids an image-loading
 * library here since every source is already an in-process [Drawable] with
 * no network/decoding cost.
 */
fun Drawable.toImageBitmap(targetSizePx: Int = 128): ImageBitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: targetSizePx
    val height = intrinsicHeight.takeIf { it > 0 } ?: targetSizePx
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}
