package com.aghatis.asmal.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aghatis.asmal.data.model.SurahDetailResponse
import com.aghatis.asmal.data.repository.QuranRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranDetailScreen(
    navController: NavController,
    surahNo: Int
) {
    val context = LocalContext.current
    val repository = remember { QuranRepository(context) }
    val viewModel: QuranDetailViewModel = viewModel(
        factory = QuranDetailViewModel.Factory(repository, surahNo)
    )
    val uiState by viewModel.uiState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()

    // Handle audio error toasts
    LaunchedEffect(audioState) {
        if (audioState is AudioState.Error) {
            android.widget.Toast.makeText(context, (audioState as AudioState.Error).message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is QuranDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is QuranDetailUiState.Success -> {
                SurahDetailContent(
                    surah = state.surah,
                    audioState = audioState,
                    onPlayClick = { ayahNo -> viewModel.playAyahAudio(ayahNo) },
                    onStopClick = { viewModel.stopAudio() }
                )
            }
            is QuranDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Floating Back Button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
                .statusBarsPadding()
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Composable
fun SurahDetailContent(
    surah: SurahDetailResponse,
    audioState: AudioState,
    onPlayClick: (Int) -> Unit,
    onStopClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Header Card
        item {
            SurahHeaderCard(surah = surah)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bismillah Calligraphy (Placeholder/Text)
        if (surah.surahNo != 9) {
            item {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Ayah List
        itemsIndexed(surah.arabic1) { index, arabicText ->
            val ayahNo = index + 1
            val englishText = surah.english.getOrElse(index) { "" }
            
            val isLoading = audioState is AudioState.Loading && audioState.ayahNo == ayahNo
            val isPlaying = audioState is AudioState.Playing && audioState.ayahNo == ayahNo

            AyahItem(
                number = ayahNo,
                arabicText = arabicText,
                englishText = englishText,
                isLoading = isLoading,
                isPlaying = isPlaying,
                onPlayToggle = {
                    if (isPlaying) onStopClick() else onPlayClick(ayahNo)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SurahHeaderCard(surah: SurahDetailResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE0C39C), // Earth tone from reference
                            Color(0xFFC49A6C)
                        )
                    )
                )
        ) {
            // Or use an actual background image if available
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = surah.surahName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = surah.surahNameTranslation,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
                Divider(
                    modifier = Modifier.width(200.dp).padding(vertical = 16.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
                Text(
                    text = "${surah.revelationPlace} • ${surah.totalAyah} Verses",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.9f)
                    )
                )
            }
        }
    }
}

@Composable
fun AyahItem(
    number: Int,
    arabicText: String,
    englishText: String,
    isLoading: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Ayah Header (Number and Actions)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah Number Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$number",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Icons
                IconButton(onClick = onPlayToggle) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = { /* Share */ }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { /* Bookmark */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Bookmark", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Arabic Text
        Text(
            text = arabicText,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                lineHeight = 44.sp,
                textAlign = TextAlign.End
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // English Translation
        Text(
            text = englishText,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }
}
