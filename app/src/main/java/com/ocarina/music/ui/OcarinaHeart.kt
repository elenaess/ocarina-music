package com.ocarina.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.Dp
import com.composables.icons.lucide.R as LucideR

/** Fully filled pink heart when saved; Lucide outline otherwise. */
@Composable
fun OcarinaHeart(liked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val c = ocarinaColors
    if (!liked) {
        Icon(painterResource(LucideR.drawable.lucide_ic_heart),
            contentDescription = "Curtir", modifier = modifier.size(size), tint = c.muted)
    } else {
        Canvas(modifier.size(size)) {
            val p = Path().apply {
                moveTo(12f, 21f)
                cubicTo(10f, 19.1f, 2f, 13.5f, 2f, 8.3f)
                cubicTo(2f, 2f, 9.3f, 1.3f, 12f, 5.2f)
                cubicTo(14.7f, 1.3f, 22f, 2f, 22f, 8.3f)
                cubicTo(22f, 13.5f, 14f, 19.1f, 12f, 21f)
                close()
            }
            scale(this.size.width / 24f, this.size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                drawPath(p, color = c.accent)
            }
        }
    }
}
