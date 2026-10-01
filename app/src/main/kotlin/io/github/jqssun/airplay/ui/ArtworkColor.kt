package io.github.jqssun.airplay.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

/** A small, cached artwork tint; never use API-31-only blur for the TV backdrop. */
internal fun artworkAverageColor(bitmap: Bitmap): Color? = runCatching {
    val sample = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
    try {
        Color(sample.getPixel(0, 0))
    } finally {
        // A 1×1 input may be returned unchanged: the metadata still owns that bitmap.
        if (sample !== bitmap) sample.recycle()
    }
}.getOrNull()
