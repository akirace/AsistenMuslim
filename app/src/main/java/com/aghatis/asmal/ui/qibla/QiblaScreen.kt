package com.aghatis.asmal.ui.qibla

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: QiblaViewModel = viewModel(factory = QiblaViewModel.Factory(context))
    
    val heading by viewModel.heading.collectAsState()
    val qiblaBearing by viewModel.qiblaBearing.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Smooth heading rotation
    val animatedHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "HeadingAnimation"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Kiblat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Location Info
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Compass Disk
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // The Compass View
                CompassView(
                    heading = animatedHeading,
                    qiblaBearing = qiblaBearing,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text(
                    text = "${qiblaBearing.toInt()}° Ke arah Ka'bah",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Arahkan perangkat Anda ke arah tanda Ka'bah",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompassView(
    heading: Float,
    qiblaBearing: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Rotate the entire canvas based on heading
        withTransform({
            rotate(-heading, center)
        }) {
            // Draw Outer Circle
            drawCircle(
                color = surfaceVariant.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw Compass Ticks
            for (i in 0 until 360 step 5) {
                val angleRad = Math.toRadians(i.toDouble() - 90)
                val isMajor = i % 30 == 0
                val tickLength = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                
                val start = Offset(
                    (center.x + (radius - tickLength) * cos(angleRad)).toFloat(),
                    (center.y + (radius - tickLength) * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + radius * cos(angleRad)).toFloat(),
                    (center.y + radius * sin(angleRad)).toFloat()
                )
                
                drawOutlineLine(start, end, if (isMajor) onSurface else onSurface.copy(alpha = 0.4f), tickWidth)
            }

            // Cardinal Points
            val cardinals = listOf("U" to 0f, "T" to 90f, "S" to 180f, "B" to 270f)
            cardinals.forEach { (label, angle) ->
                val angleRad = Math.toRadians(angle.toDouble() - 90)
                val pos = Offset(
                    (center.x + (radius - 30.dp.toPx()) * cos(angleRad)).toFloat(),
                    (center.y + (radius - 30.dp.toPx()) * sin(angleRad)).toFloat()
                )
                // In a real app we'd use drawText, but for simplicity we'll skip text on canvas
                // or use a helper if available. Let's just draw markers.
                drawCircle(
                    color = if (label == "U") Color.Red else onSurface,
                    radius = if (label == "U") 4.dp.toPx() else 3.dp.toPx(),
                    center = pos
                )
            }

            // Qibla Indicator (Needle)
            val qiblaRad = Math.toRadians(qiblaBearing.toDouble() - 90)
            val needleLength = radius - 40.dp.toPx()
            val needleWidth = 6.dp.toPx()

            val needleEnd = Offset(
                (center.x + needleLength * cos(qiblaRad)).toFloat(),
                (center.y + needleLength * sin(qiblaRad)).toFloat()
            )

            drawLine(
                color = primaryColor,
                start = center,
                end = needleEnd,
                strokeWidth = needleWidth,
                cap = StrokeCap.Round
            )
            
            // Needle Head (Kaaba direction)
            drawCircle(
                color = primaryColor,
                radius = 8.dp.toPx(),
                center = needleEnd
            )
        }

        // Draw Center Point (Not rotated)
        drawCircle(
            color = onSurface,
            radius = 6.dp.toPx(),
            center = center
        )
        drawCircle(
            color = surfaceColor,
            radius = 3.dp.toPx(),
            center = center
        )
        
        // Fixed Top Indicator
        drawLine(
            color = secondaryColor,
            start = Offset(center.x, center.y - radius - 10.dp.toPx()),
            end = Offset(center.x, center.y - radius + 10.dp.toPx()),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOutlineLine(start: Offset, end: Offset, color: Color, width: Float) {
    drawLine(color = color, start = start, end = end, strokeWidth = width, cap = StrokeCap.Round)
}
