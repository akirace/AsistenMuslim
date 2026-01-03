package com.aghatis.asmal.ui.home

import android.Manifest
import android.widget.Toast
import android.widget.Toast.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Warning // Delete this later if not used
import androidx.compose.material.icons.filled.Info // Placeholder for pause if needed, or use a drawable
// Actually Compose has Icons.Filled.Pause but needs import or it's standard
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.* // Pause is often not in material3 direct but in icons package
// Pause is generic. 
// I will assume users have standard icon set.
// Actually, `androidx.compose.material.icons.filled.Pause` is the path.
// Let's add that import manually.
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aghatis.asmal.data.model.PrayerData
import com.aghatis.asmal.data.repository.PrayerRepository
import com.aghatis.asmal.data.repository.PrefsRepository
import com.aghatis.asmal.ui.home.HomeUiState
import com.aghatis.asmal.ui.home.HomeViewModel
import com.aghatis.asmal.utils.PrayerTimeUtils

import android.media.MediaPlayer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import com.aghatis.asmal.data.model.AyahResponse
import com.aghatis.asmal.data.repository.QuranRepository
import com.aghatis.asmal.ui.home.AyahUiState

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val prefsRepository = PrefsRepository(context)
    val prayerRepository = PrayerRepository()
    val quranRepository = QuranRepository()
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(prefsRepository, prayerRepository, quranRepository)
    )

    val user by viewModel.userState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val ayahState by viewModel.ayahState.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.fetchPrayerTimesWithLocation(context)
        } else {
             Toast.makeText(context, "Location permission needed for prayer times", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf<String>(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(screenPadding())
            .verticalScroll(rememberScrollState()) // Allow scrolling
    ) {
        // 1. Welcome Section
        WelcomeHeader(userName = user?.displayName, photoUrl = user?.photoUrl)

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Prayer Time Section
        Text(
            text = "Jadwal Sholat",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is HomeUiState.Error -> {
                ErrorCard(message = state.message)
            }
            is HomeUiState.Success -> {
                PrayerTimesCard(prayerData = state.prayerData, locationName = state.locationName)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Today Ayah Section
        Text(
            text = "Ayat Hari Ini",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = ayahState) {
             is AyahUiState.Loading -> {
                 Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                     CircularProgressIndicator(modifier = Modifier.size(24.dp))
                 }
             }
             is AyahUiState.Error -> {
                 ErrorCard(message = "Gagal memuat ayat hari ini: ${state.message}")
             }
             is AyahUiState.Success -> {
                 TodayAyahCard(ayah = state.ayah)
             }
        }
        
        // Add extra spacer for bottom navigation overlap prevention
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun TodayAyahCard(ayah: AyahResponse) {
    var expanded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    val context = LocalContext.current

    // Dispose MediaPlayer when card is removed or recomposed
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    
    // Stop audio if collapsed
    LaunchedEffect(expanded) {
        if (!expanded && isPlaying) {
             mediaPlayer?.stop()
             mediaPlayer?.prepare() // Prepare for next play
             isPlaying = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessLow)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(20.dp)
        ) {
            // Header: Surah Info
            Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.Top
            ) {
                 Column(modifier = Modifier.weight(1f)) {
                     Text(
                         text = "${ayah.surahName} : ${ayah.ayahNo}",
                         style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                         color = Color.DarkGray
                     )
                     Text(
                         text = ayah.surahNameTranslation,
                         style = MaterialTheme.typography.bodySmall,
                         color = Color.Gray
                     )
                 }
                 
                 Icon(
                     imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                     contentDescription = "Expand",
                     tint = Color.Gray
                 )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Arabic Text
             Text(
                 text = ayah.arabic1,
                 style = MaterialTheme.typography.headlineMedium.copy(
                     fontWeight = FontWeight.SemiBold,
                     fontFamily = androidx.compose.ui.text.font.FontFamily.Default // Or generic sans serif if Arabic font issues
                 ),
                 color = Color.Black,
                 modifier = Modifier.fillMaxWidth(),
                 textAlign = androidx.compose.ui.text.style.TextAlign.End
             )
             
             Spacer(modifier = Modifier.height(8.dp))
             
             // English Text
             Text(
                 text = ayah.english,
                 style = MaterialTheme.typography.bodyMedium.copy(
                     fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                 ),
                 color = Color.DarkGray
             )

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Audio Player
                val audioUrl = ayah.audio["1"]?.url // Mishary Rashid
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (audioUrl != null) {
                                if (mediaPlayer == null) {
                                     mediaPlayer = MediaPlayer().apply {
                                         setDataSource(audioUrl)
                                         setOnPreparedListener { 
                                             start()
                                             isPlaying = true
                                         }
                                         setOnCompletionListener { 
                                             isPlaying = false 
                                         }
                                         prepareAsync()
                                     }
                                } else {
                                    if (isPlaying) {
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                    } else {
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    }
                                }
                            } else {
                                 Toast.makeText(context, "Audio not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .background(Color(0xFFFF5722), CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = if (isPlaying) "Playing Recitation..." else "Play Audio (Mishary Rashid)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun screenPadding() = PaddingValues(horizontal = 24.dp, vertical = 24.dp)

@Composable
fun WelcomeHeader(userName: String?, photoUrl: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
             AsyncImage(
                model = photoUrl ?: "https://ui-avatars.com/api/?name=${userName ?: "User"}&background=random",
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Assalamu'alaikum,",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = userName ?: "Saudaraku",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp
                )
            )
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
         modifier = Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun PrayerTimesCard(prayerData: PrayerData, locationName: String) {
    var expanded by remember { mutableStateOf(false) }
    val nextPrayer = remember(prayerData) { PrayerTimeUtils.getNextPrayer(prayerData.times) }
    val cardBgColor = Color(0xFFF5F7FA) // Very light gray/blue
    val activeColor = Color(0xFFFF5722) // Orange accent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column(
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(20.dp)
        ) {
            // Header Row: Toggle Button & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Column {
                    Text(
                        text = "Lokasi: $locationName",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = prayerData.date.readable,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.DarkGray
                    )
                 }
                 
                 IconButton(onClick = { expanded = !expanded }) {
                     Icon(
                         imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                         contentDescription = "Toggle",
                         tint = Color.Gray
                     )
                 }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!expanded) {
                // COLLAPSED: Show Next Prayer Only
                nextPrayer?.let { (name, time) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Menuju Sholat",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        DisplayTimeBig(name, time, activeColor)
                    }
                }
            } else {
                // EXPANDED: Show List
                // Map of prayers to iterate easily while keeping order
                val prayers = listOf(
                    "Fajr" to prayerData.times.fajr,
                    "Dhuhr" to prayerData.times.dhuhr,
                    "Asr" to prayerData.times.asr,
                    "Maghrib" to prayerData.times.maghrib,
                    "Isha" to prayerData.times.isha
                )
                
                prayers.forEach { (name, time) ->
                    // Check if this is the 'next' prayer to highlight
                    val isNext = nextPrayer?.first == name
                    
                    PrayerItemRow(
                        name = name, 
                        time = time, 
                        isNext = isNext,
                        activeColor = activeColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun DisplayTimeBig(name: String, time: String, activeColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.DarkGray
        )
        Text(
            text = time,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = activeColor
        )
    }
}

@Composable
fun PrayerItemRow(name: String, time: String, isNext: Boolean, activeColor: Color) {
    val bgColor = if (isNext) activeColor.copy(alpha = 0.1f) else Color.Transparent
    val textColor = if (isNext) activeColor else Color.DarkGray
    val weight = if (isNext) FontWeight.Bold else FontWeight.Medium
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = weight, color = textColor)
        Text(text = time, style = MaterialTheme.typography.bodyLarge, fontWeight = weight, color = textColor)
    }
}
