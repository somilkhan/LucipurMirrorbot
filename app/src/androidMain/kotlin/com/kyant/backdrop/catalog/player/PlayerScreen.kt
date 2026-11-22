package com.kyant.backdrop.catalog.player

import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/** Two versions of the same player. Both share playback, auto-hide and seeking. */
enum class PlayerStyle { Glass, Quiet }

private const val AUTO_HIDE_MS = 3_000L

@Composable
fun PlayerScreen(
    rawResId: Int,
    style: PlayerStyle = PlayerStyle.Glass
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(rawResId)))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var texture by remember { mutableStateOf<TextureView?>(null) }

    // Real playback clock: poll the player, so the UI follows actual time.
    LaunchedEffect(player) {
        while (isActive) {
            isPlaying = player.isPlaying
            position = player.currentPosition
            duration = player.duration.takeIf { it > 0 } ?: 1L
            delay(100)
        }
    }

    // Auto-hide: controls stay while paused, otherwise hide after 3s of no touch.
    var controlsVisible by remember { mutableStateOf(true) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastTouch, isPlaying) {
        controlsVisible = true
        if (isPlaying) {
            delay(AUTO_HIDE_MS)
            controlsVisible = false
        }
    }
    fun touch() { lastTouch = System.currentTimeMillis() }

    // Glass samples a low-res snapshot of the video. The TextureView stays sharp on top.
    val backdrop = rememberLayerBackdrop()
    val snapshot by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, texture) {
        while (isActive) {
            val t = texture
            if (t != null && t.isAvailable) {
                t.getBitmap(240, 135)
                    ?.let { value = it.asImageBitmap() }
            }
            delay(if (isPlaying) 133 else 500)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Content layer: the video. Recorded into the backdrop so glass can sample it.
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            snapshot?.let { img ->
                Canvas(Modifier.fillMaxSize()) {
                    drawImage(img, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TextureView(ctx).also {
                        player.setVideoTextureView(it)
                        texture = it
                    }
                }
            )
        }

        // Tap anywhere toggles the controls.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        if (controlsVisible) controlsVisible = false else touch()
                    })
                }
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250))
        ) {
            when (style) {
                PlayerStyle.Glass -> GlassControls(backdrop, player, isPlaying, position, duration, ::touch)
                PlayerStyle.Quiet -> QuietControls(backdrop, player, isPlaying, position, duration, ::touch)
            }
        }
    }
}

@Composable
private fun GlassControls(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    player: ExoPlayer,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    touch: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 18.dp)) {
        GlassCircle(backdrop, 54.dp, Modifier.align(Alignment.TopStart), onClick = touch) {
            Chevron(Modifier.size(22.dp))
        }

        Row(
            Modifier
                .align(Alignment.TopEnd)
                .height(54.dp)
                .glass(backdrop, Capsule())
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("PiP", "Lock", "Together").forEach { label ->
                Box(Modifier.size(44.dp).clickable { touch() }, contentAlignment = Alignment.Center) {
                    BasicText(label, style = TextStyle(Color.White, 12.sp))
                }
            }
        }

        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircle(backdrop, 64.dp, onClick = {
                player.seekTo((position - 10_000).coerceAtLeast(0)); touch()
            }) {
                BasicText("10", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
            }
            GlassCircle(backdrop, 96.dp, onClick = {
                if (player.isPlaying) player.pause() else player.play(); touch()
            }) {
                PlayPause(isPlaying, Modifier.size(34.dp))
            }
            GlassCircle(backdrop, 64.dp, onClick = {
                player.seekTo((position + 10_000).coerceAtMost(duration)); touch()
            }) {
                BasicText("10", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
            }
        }

        Column(Modifier.align(Alignment.BottomStart).padding(bottom = 130.dp)) {
            BasicText("Sample", style = TextStyle(Color.White, 26.sp, FontWeight.Bold))
            BasicText("Black and white test", style = TextStyle(Color.White.copy(alpha = 0.8f), 16.sp))
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(bottom = 130.dp),
            horizontalAlignment = Alignment.End
        ) {
            BasicText(
                "${formatTime(position)} / ${formatTime(duration)}",
                style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
            )
        }

        ThreadScrubber(
            progress = (position.toFloat() / duration).coerceIn(0f, 1f),
            onSeek = { p -> player.seekTo((p * duration).toLong()); touch() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(44.dp)
                .padding(bottom = 72.dp)
        )

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .glass(backdrop, Capsule())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Subs", "Audio", "Quality", "Episodes", "More").forEach { label ->
                BasicText(
                    label,
                    Modifier.clickable { touch() }.padding(horizontal = 8.dp),
                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold, textAlign = TextAlign.Center)
                )
            }
        }
    }
}

@Composable
private fun QuietControls(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    player: ExoPlayer,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    touch: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 18.dp)) {
        GlassCircle(backdrop, 44.dp, Modifier.align(Alignment.TopStart), onClick = touch) {
            Chevron(Modifier.size(18.dp))
        }

        GlassCircle(backdrop, 84.dp, Modifier.align(Alignment.Center), onClick = {
            if (player.isPlaying) player.pause() else player.play(); touch()
        }) {
            PlayPause(isPlaying, Modifier.size(30.dp))
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
                .glass(backdrop, Capsule())
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                formatTime(position),
                style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
            )
            ThreadScrubber(
                progress = (position.toFloat() / duration).coerceIn(0f, 1f),
                onSeek = { p -> player.seekTo((p * duration).toLong()); touch() },
                modifier = Modifier.weight(1f).height(44.dp).padding(horizontal = 14.dp)
            )
            BasicText(
                formatTime(duration),
                style = TextStyle(Color.White.copy(alpha = 0.7f), 14.sp)
            )
        }
    }
}

// MARK: Glass

/** Real refraction from the Backdrop library: vibrancy, blur, lens (Android 13+). */
private fun Modifier.glass(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
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
    }
)

@Composable
private fun GlassCircle(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(size)
            .glass(backdrop, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// MARK: Glyphs (no icon library, B&W only)

@Composable
private fun PlayPause(playing: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        if (playing) {
            val w = size.width * 0.22f
            drawRect(Color.White, Offset(size.width * 0.18f, 0f), Size(w, size.height))
            drawRect(Color.White, Offset(size.width * 0.6f, 0f), Size(w, size.height))
        } else {
            val p = Path().apply {
                moveTo(size.width * 0.22f, 0f)
                lineTo(size.width, size.height / 2)
                lineTo(size.width * 0.22f, size.height)
                close()
            }
            drawPath(p, Color.White)
        }
    }
}

@Composable
private fun Chevron(modifier: Modifier) {
    Canvas(modifier) {
        val p = Path().apply {
            moveTo(size.width * 0.65f, size.height * 0.2f)
            lineTo(size.width * 0.35f, size.height * 0.5f)
            lineTo(size.width * 0.65f, size.height * 0.8f)
        }
        drawPath(p, Color.White, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

// MARK: Thread scrubber: real progress, drag to seek

@Composable
private fun ThreadScrubber(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier
) {
    Canvas(
        modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onSeek(it.x / size.width) })
        }
    ) {
        val mid = size.height / 2f
        val px = size.width * progress
        // One hairline track, a bright played part, and a knob. Black and white only.
        drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, mid), Offset(size.width, mid), 1.5.dp.toPx())
        drawLine(Color.White, Offset(0f, mid), Offset(px, mid), 2.dp.toPx())
        drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(px, mid))
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%02d:%02d".format(s / 60, s % 60)
}
