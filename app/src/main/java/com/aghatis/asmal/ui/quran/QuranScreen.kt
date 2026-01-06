package com.aghatis.asmal.ui.quran

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aghatis.asmal.data.model.SurahEntity
import com.aghatis.asmal.data.repository.QuranRepository
import com.aghatis.asmal.data.repository.QoriRepository
import com.aghatis.asmal.data.model.QoriEntity
import androidx.compose.foundation.lazy.LazyRow
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val repository = remember { QuranRepository(context) }
    // Instantiate QoriRepository using database from QuranRepository (shared db preferred, but for now new instance via context)
    // Note: Best practice is dependency injection or shared DB instance. 
    // Assuming AppDatabase is singleton or cheap, or extracting DB access.
    // For now, let's assume we can get it via context similarly to QuranRepository internal.
    // Ideally we should refactor QuranRepository to take DB or Context and separate DAO.
    // For this specific task, we'll follow existing pattern but create QoriRepository.
    
    // We need to access the database instance. Since QuranRepository creates its own private DB instance, 
    // we should ideally expose it or create a singleton AppDatabase.
    // Blocked by private db in QuranRepository. 
    // Workaround: Create new QoriRepository that also creates DB instance (not optimal for consistency but works for separation)
    // OR: Modify QuranRepository to expose DB or be Singleton.
    // Seeing QuranRepository creates DB in init: 
    // private val db = androidx.room.Room.databaseBuilder(...).build()
    
    // Let's create QoriRepository similar to QuranRepository structure for now, 
    // passing Context and letting it create its own DB ref or use a shared one in future refactor.
    // Wait, QoriRepository takes QoriDao. 
    // So we need to create DB here or inside a factory helper.
    
    // Let's Quick Fix: Create a helper to get database since we are in Composable.
    val db = remember { 
         androidx.room.Room.databaseBuilder(
            context.applicationContext,
            com.aghatis.asmal.data.local.AppDatabase::class.java, "asmal-db"
        ).fallbackToDestructiveMigration().build()
    }
    
    val qoriRepository = remember { com.aghatis.asmal.data.repository.QoriRepository(db.qoriDao()) }
    
    // We also need separate QuranRepository that ideally shares this DB, but existing one creates its own.
    // To avoid DB open conflict or resource waste, we should ideally use the same DB.
    // But modifying QuranRepository deeply might be risky without viewing it all. 
    // Let's use the new db instance for Qori, and let QuranRepository keep its own for now (sqlite supports multiple open connections usually, though singleton recommended).
    // Better yet: Pass db to QuranRepository if possible? No, constructor takes Context.
    
    val viewModel: QuranViewModel = viewModel(
        factory = QuranViewModel.Factory(repository, qoriRepository)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val qoriList by viewModel.qoriList.collectAsState()
    val selectedQoriId by viewModel.selectedQoriId.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val allSurahs by viewModel.allSurahs.collectAsState()

    val tabs = listOf("Qur'an", "Qur'an Audio", "Baca")
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TabRow
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                )
            }
        }

        // Pager Content
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> SurahListContent(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onSurahClick = { surahNo ->
                        navController.navigate("quran_detail/$surahNo")
                    }
                )
                1 -> QuranAudioScreen(
                    qoriList = qoriList,
                    selectedQoriId = selectedQoriId,
                    allSurahs = allSurahs,
                    playbackState = playbackState,
                    onQoriSelected = { viewModel.onSelectQori(it) },
                    onPlaySurah = { viewModel.playAudio(it) }
                )
                2 -> TerakhirBacaScreen()
            }
        }
    }
}

@Composable
fun SurahListContent(
    uiState: QuranUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSurahClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = {
                Text(
                    "Cari surah atau nomor...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is QuranUiState.Loading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            bottom = 16.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                    ) {
                        items(10) {
                            ShimmerSurahItem()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                is QuranUiState.Success -> {
                    if (state.surahs.isEmpty() && searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Surah tidak ditemukan",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                bottom = 16.dp,
                                start = 16.dp,
                                end = 16.dp
                            )
                        ) {
                            items(state.surahs) { surah ->
                                SurahItem(
                                    surah = surah,
                                    onClick = { onSurahClick(surah.surahNo) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                is QuranUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun QuranAudioScreen(
    qoriList: List<QoriEntity>,
    selectedQoriId: String,
    allSurahs: List<SurahEntity>,
    playbackState: AudioPlaybackState,
    onQoriSelected: (String) -> Unit,
    onPlaySurah: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp), // Extra padding for potential bottom play bar
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Reciters",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(qoriList) { qori ->
                    QoriItem(
                        qori = qori,
                        isSelected = qori.idReciter == selectedQoriId,
                        onClick = { onQoriSelected(qori.idReciter) }
                    )
                }
            }
        }
        
        item {
             Spacer(modifier = Modifier.height(24.dp))
             Text(
                text = "Surah List",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(allSurahs) { surah ->
            // Determine state for this specific surah
            val isPlaying = playbackState is AudioPlaybackState.Playing && (playbackState as AudioPlaybackState.Playing).surahNo == surah.surahNo
            val isLoading = playbackState is AudioPlaybackState.Loading && (playbackState as AudioPlaybackState.Loading).surahNo == surah.surahNo
            
            AudioSurahItem(
                surah = surah,
                isPlaying = isPlaying,
                isLoading = isLoading,
                onPlayClick = { onPlaySurah(surah.surahNo) }
            )
        }
    }
}

@Composable
fun QoriItem(
    qori: QoriEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                AsyncImage(
                    model = qori.photoUrl ?: "https://ui-avatars.com/api/?name=${qori.reciterName}&background=random",
                    contentDescription = qori.reciterName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Color.Gray)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = qori.reciterName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AudioSurahItem(
    surah: SurahEntity,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayClick: () -> Unit
) {
    val borderColor = if (isPlaying) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isPlaying) 2.dp else 0.dp

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Number & Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)
                ) {
                    Text(
                        text = "${surah.surahNo}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = surah.surahName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${surah.revelationPlace} • ${surah.totalAyah} Ayats",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right Side: Play Button
            // Right Side: Play Button with state
            IconButton(
                onClick = onPlayClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow, // Using Close/Pause icon for active state
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TerakhirBacaScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Menu, // Use Menu as placeholder
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "History Reading Coming Soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SurahItem(
    surah: SurahEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Number & Latin Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Number Frame (Star-like shape placeholder using Box with border/rotation or image)
                // Using a simple Box with rotation for now to mimic the star shape
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack, // Placeholder for Star/Badge icon if custom asset not available
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    )
                    // Or preferably use a Shape drawable or draw behind
                    // Simulating the star badge from the image:
                     Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) // Placeholder shape
                    )
                    
                    Text(
                        text = "${surah.surahNo}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = surah.surahName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${surah.revelationPlace} • ${surah.totalAyah} Verses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right Side: Arabic Name
            Text(
                text = surah.surahNameArabic,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif // Try to use serif for Arabic if no custom font
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ShimmerSurahItem() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Box(modifier = Modifier.height(20.dp).width(120.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.height(14.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.height(24.dp).width(60.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        }
    }
}
