package com.aghatis.asmal.ui.quran

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import com.aghatis.asmal.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QuranPlayerScreen(
    navController: NavController,
    viewModel: QuranViewModel,
    surahNo: Int
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val bufferedProgress by viewModel.bufferedProgress.collectAsState()
    val allSurahs by viewModel.allSurahs.collectAsState()
    val qoriList by viewModel.qoriList.collectAsState()
    val selectedQoriId by viewModel.selectedQoriId.collectAsState()

    // Determine current Surah and Qori
    // If playback state surah != surahNo, it means we navigated here but player might be idle or playing something else.
    // Ideally we sync with player state. User expects clicking surah -> play it.
    // If we are here, we should be playing 'surahNo'.
    // Logic: If state is Idle or Playing other surah, start this one.
    
    // Determine the active surah number based on playback state or initial route
    val activeSurahNo = remember(playbackState, surahNo) {
        (playbackState as? AudioPlaybackState.Playing)?.surahNo
            ?: (playbackState as? AudioPlaybackState.Buffering)?.surahNo
            ?: (playbackState as? AudioPlaybackState.Loading)?.surahNo
            ?: surahNo
    }

    // Side effect to start playing only if player is Idle or playing something else than the initial selection
    LaunchedEffect(Unit) {
        val currentState = playbackState
        val currentSurahPlaying = (currentState as? AudioPlaybackState.Playing)?.surahNo 
            ?: (currentState as? AudioPlaybackState.Loading)?.surahNo
            ?: (currentState as? AudioPlaybackState.Buffering)?.surahNo
            
        if (currentSurahPlaying != surahNo) {
            viewModel.playAudio(surahNo)
        }
    }

    val currentSurah = allSurahs.find { it.surahNo == activeSurahNo }
    val currentQori = qoriList.find { it.idReciter == selectedQoriId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface) // Theme-aware background
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Now Playing", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { /* Menu */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Circular Player
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(320.dp)
        ) {
            CircularProgress(
                progress = progress,
                bufferedProgress = bufferedProgress,
                onProgressChanged = { viewModel.seekTo(it) }
            )
            
            // Shimmer effect for loading
            val transition = rememberInfiniteTransition(label = "shimmer")
            val translateAnim by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmerTranslation"
            )

            val shimmerColors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            )

            val shimmerBrush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset.Zero,
                end = Offset(x = translateAnim, y = translateAnim)
            )
            
            // Gradient Album Art / Shimmer Loader
            // Use Crossfade for smooth transition
            val isLoading = playbackState is AudioPlaybackState.Loading || playbackState is AudioPlaybackState.Buffering
            
            Crossfade(
                targetState = isLoading, 
                animationSpec = tween(500),
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape),
                label = "loadingTransition"
            ) { loading ->
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(shimmerBrush)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                ),
                                    start = Offset.Zero,
                                    end = Offset.Infinite
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                         // Art
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Info
        Text(
            text = currentSurah?.surahName ?: "Loading...",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = currentQori?.reciterName ?: "Unknown Reciter",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { /* Shuffle */ }) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = MaterialTheme.colorScheme.primary)
            }
            
            val isFirstSurah = (currentSurah?.surahNo ?: 1) <= 1
            IconButton(
                onClick = { viewModel.playPreviousSurah() }, 
                modifier = Modifier.size(48.dp),
                enabled = !isFirstSurah
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (isFirstSurah) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Play/Pause
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }

            val isLastSurah = (currentSurah?.surahNo ?: 1) >= 114
            IconButton(
                onClick = { viewModel.playNextSurah() }, 
                modifier = Modifier.size(48.dp),
                enabled = !isLastSurah
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = if (isLastSurah) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = { /* Repeat */ }) {
                Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = MaterialTheme.colorScheme.outline)
            }
        }
        
    }
}

@Composable
fun CircularProgress(
    progress: Float,
    bufferedProgress: Float,
    onProgressChanged: (Float) -> Unit
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val bufferedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val progressColor = MaterialTheme.colorScheme.primary
    
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    val displayProgress = if (isDragging) dragProgress else progress
    // Animate progress smoothly only when not dragging
    // But seeker needs immediate feedback, so maybe don't animate value too slow
    
    val radius = 140.dp
    
    Box(
        modifier = Modifier
            .size(radius * 2 + 20.dp) // padding for knob
            .pointerInput(Unit) {
               detectDragGestures(
                   onDragStart = { offset ->
                       isDragging = true
                       dragProgress = calculateAngle(offset, size.toSize()) / 360f
                   },
                   onDrag = { change, _ ->
                       dragProgress = calculateAngle(change.position, size.toSize()) / 360f
                   },
                   onDragEnd = {
                       isDragging = false
                       onProgressChanged(dragProgress)
                   }
               )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val strokeWidth = 2.dp.toPx()
            val r = (size.width - 20.dp.toPx()) / 2 // Reduce for knob space

            // Track
            drawCircle(
                color = trackColor,
                radius = r,
                style = Stroke(width = strokeWidth)
            )

            // Buffered Progress Arc
            drawArc(
                color = bufferedColor,
                startAngle = -90f,
                sweepAngle = bufferedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            // Progress Arc
            // We want start from top (-90 deg)
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = displayProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            // Knob
            val angle = (displayProgress * 360f) - 90f
            val angleRad = Math.toRadians(angle.toDouble())
            val knobX = center.x + r * cos(angleRad).toFloat()
            val knobY = center.y + r * sin(angleRad).toFloat()

            drawCircle(
                color = progressColor,
                radius = 6.dp.toPx(),
                center = Offset(knobX, knobY)
            )
        }
    }
}

fun calculateAngle(touch: Offset, size: Size): Float {
    val center = Offset(size.width / 2, size.height / 2)
    val dx = touch.x - center.x
    val dy = touch.y - center.y
    // atan2 gives -PI to PI. 0 is right (3 o'clock).
    // We want 0 at top (12 o'clock).
    // touch angle:
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    // angle is now: Right=0, Down=90, Left=180/-180, Up=-90
    
    // Shift so Up is 0
    angle += 90f 
    // Now: Up=0, Right=90, Down=180, Left=270, TopLeft=360 but atan2 wraps
    
    if (angle < 0) angle += 360f
    return angle.coerceIn(0f, 360f)
}
