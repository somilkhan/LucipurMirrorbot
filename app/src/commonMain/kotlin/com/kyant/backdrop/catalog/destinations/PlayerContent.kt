package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import kotlin.math.sin

/**
 * v11 Liquid Glass player, Compose version.
 * Glass is real refraction from the backdrop library (lens + blur + vibrancy).
 * Only the controls are glass. The artwork and text stay opaque.
 * Not compiled here: build the app module to verify.
 */
@Composable
fun PlayerContent() {
    val backdrop = rememberLayerBackdrop()
    var playing by remember { mutableStateOf(true) }
    var position by remember { mutableFloatStateOf(376f) }
    val total = 3459f

    Box(Modifier.fillMaxSize()) {
        // 1. Content layer: the artwork is captured as the backdrop. No glass here.
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(Color(0xFF050403))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFF59E4B), Color.Transparent),
                            center = Offset(size.width * 0.14f, size.height * 0.22f),
                            radius = size.width * 0.38f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF5B8DFF), Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.3f),
                            radius = size.width * 0.36f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFE11D48), Color.Transparent),
                            center = Offset(size.width * 0.52f, size.height * 0.92f),
                            radius = size.width * 0.42f
                        )
                    )
                }
        )

        // 2. Floating glass controls, each sampling the artwork above.
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 40.dp, top = 18.dp)
        ) {
            GlassCircle(backdrop, size = 54.dp, onClick = {}) {
                ChevronLeft(Modifier.size(22.dp))
            }
        }

        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 40.dp, top = 18.dp)
                .height(54.dp)
                .drawGlass(backdrop, Capsule())
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("PiP", "Rotate", "Together", "Lock").forEach { label ->
                Box(
                    Modifier
                        .size(44.dp)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(label, style = TextStyle(Color.White, 11.sp))
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.Center)
                .padding(bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircle(backdrop, size = 64.dp, onClick = { position = (position - 10f).coerceAtLeast(0f) }) {
                BasicText("-10", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
            }
            GlassCircle(backdrop, size = 96.dp, onClick = { playing = !playing }) {
                if (playing) PauseGlyph(Modifier.size(34.dp)) else PlayGlyph(Modifier.size(34.dp))
            }
            GlassCircle(backdrop, size = 64.dp, onClick = { position = (position + 10f).coerceAtMost(total) }) {
                BasicText("+10", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 40.dp, bottom = 140.dp)
        ) {
            BasicText(
                "Sample Series",
                style = TextStyle(Color.White, 26.sp, FontWeight.Bold)
            )
            BasicText(
                "S1 E1 · Episode title",
                style = TextStyle(Color.White.copy(alpha = 0.8f), 16.sp)
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 40.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.End
        ) {
            BasicText(
                "${formatTime(position)} / ${formatTime(total)}",
                style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
            )
            BasicText(
                "Ends in ${formatTime(total - position)}",
                style = TextStyle(Color.White.copy(alpha = 0.66f), 14.sp)
            )
        }

        ThreadScrubber(
            position = position / total,
            onSeek = { position = it * total },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 40.dp, end = 40.dp, bottom = 90.dp)
                .fillMaxWidth()
                .height(44.dp)
        )

        // Glass rail: one capsule, items inside.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 40.dp, end = 40.dp, bottom = 14.dp)
                .fillMaxWidth()
                .height(64.dp)
                .drawGlass(backdrop, Capsule())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Zoom", "1x", "Subs", "Audio", "1080p", "Sources", "Episodes", "Details").forEach { label ->
                BasicText(
                    label,
                    Modifier
                        .clickable { }
                        .padding(horizontal = 8.dp),
                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold, textAlign = TextAlign.Center)
                )
            }
        }
    }
}

// MARK: Glass helpers

/** Real refraction from the backdrop library: blur, then lens (Android 13+). */
private fun Modifier.drawGlass(
    backdrop: com.kyant.backdrop.Backdrop,
    shape: androidx.compose.ui.graphics.Shape
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(2.dp.toPx())
        lens(12.dp.toPx(), 24.dp.toPx())
    },
    onDrawSurface = {
        drawRect(Color.White.copy(alpha = 0.10f))
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
            )
        )
    }
)

@Composable
private fun GlassCircle(
    backdrop: com.kyant.backdrop.Backdrop,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .size(size)
            .drawGlass(backdrop, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// MARK: Glyphs (drawn, no icon library)

@Composable
private fun PlayGlyph(modifier: Modifier) {
    Canvas(modifier) {
        val p = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.12f)
            lineTo(size.width * 0.9f, size.height * 0.5f)
            lineTo(size.width * 0.25f, size.height * 0.88f)
            close()
        }
        drawPath(p, Color.White)
    }
}

@Composable
private fun PauseGlyph(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width * 0.2f
        drawRect(Color.White, Offset(size.width * 0.2f, 0f), androidx.compose.ui.geometry.Size(w, size.height))
        drawRect(Color.White, Offset(size.width * 0.6f, 0f), androidx.compose.ui.geometry.Size(w, size.height))
    }
}

@Composable
private fun ChevronLeft(modifier: Modifier) {
    Canvas(modifier) {
        val p = Path().apply {
            moveTo(size.width * 0.65f, size.height * 0.2f)
            lineTo(size.width * 0.35f, size.height * 0.5f)
            lineTo(size.width * 0.65f, size.height * 0.8f)
        }
        drawPath(p, Color.White, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

// MARK: Thread scrubber

@Composable
private fun ThreadScrubber(
    position: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier
) {
    Canvas(
        modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onSeek(it.x / size.width) })
        }
    ) {
        val px = size.width * position
        val mid = size.height / 2f
        val palette = listOf(Color(0xFFF59E4B), Color(0xFF5B8DFF), Color(0xFFE11D48))
        palette.forEachIndexed { i, c ->
            val path = Path()
            var x = 0f
            while (x <= size.width) {
                val d = kotlin.math.abs(x - px) / (size.width * 0.22f)
                val s = d.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
                val y = mid + (i - 1) * 4.dp.toPx() * s +
                    sin(x * 0.028f + i * 1.37f) * (7f - i) * s
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += 2f
            }
            drawPath(path, c.copy(alpha = 0.5f), style = Stroke(width = 1.6.dp.toPx()))
        }
        drawCircle(Color.White, radius = 5.5.dp.toPx(), center = Offset(px, mid))
    }
}

private fun formatTime(seconds: Float): String {
    val s = seconds.toInt()
    val m = s / 60
    val x = s % 60
    return "${m.toString().padStart(2, '0')}:${x.toString().padStart(2, '0')}"
}
