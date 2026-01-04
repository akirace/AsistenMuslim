package com.aghatis.asmal.ui.home

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Info 
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.* 
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.aghatis.asmal.utils.PrayerTimeUtils
import android.media.MediaPlayer
import com.aghatis.asmal.data.model.AyahResponse
import com.aghatis.asmal.data.repository.Mosque
import com.aghatis.asmal.data.repository.QuranRepository
import com.aghatis.asmal.data.repository.MosqueRepository
import com.aghatis.asmal.data.model.PrayerLog
import com.aghatis.asmal.data.repository.PrayerLogRepository

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val prefsRepository = PrefsRepository(context)
    val prayerRepository = PrayerRepository()
    val quranRepository = QuranRepository()
    val mosqueRepository = MosqueRepository(context)
    val prayerLogRepository = PrayerLogRepository()
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(prefsRepository, prayerRepository, quranRepository, mosqueRepository, prayerLogRepository)
    )

    val user by viewModel.userState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val ayahState by viewModel.ayahState.collectAsState()
    val mosqueState by viewModel.mosqueState.collectAsState()
    val prayerLog by viewModel.prayerLogState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val prayerProgress by viewModel.prayerProgress.collectAsState()

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
                ErrorCard(
                    message = "Ada kendala di third party nya, silahkan coba lagi",
                    onRefresh = { viewModel.fetchPrayerTimesWithLocation(context) }
                )
            }
            is HomeUiState.Success -> {
                PrayerTimesCard(prayerData = state.prayerData, locationName = state.locationName)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Progress and Date Navigator Section
        var showProgressDialog by remember { mutableStateOf(false) }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // Enforce equal height
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DailyProgressCard(
                prayerLog = prayerLog,
                onClick = { showProgressDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            DateNavigatorCard(
                date = selectedDate,
                onPrevious = { viewModel.changeDate(-1) },
                onNext = { viewModel.changeDate(1) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
        
        if (showProgressDialog) {
             val currentPrayerData = (uiState as? HomeUiState.Success)?.prayerData
             PrayerProgressDetailDialog(
                 prayerLog = prayerLog,
                 prayerData = currentPrayerData,
                 onDismiss = { showProgressDialog = false }
             )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Prayer Checklist Section
        val currentPrayerData = (uiState as? HomeUiState.Success)?.prayerData
        PrayerChecklistCard(
            prayerLog = prayerLog,
            prayerData = currentPrayerData,
            selectedDate = selectedDate,
            onToggle = { prayer, isChecked ->
                viewModel.togglePrayerStatus(prayer, isChecked)
            }
        )

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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 4. Nearest Mosque Section (Third Feature Section)
        Text(
            text = "Masjid Terdekat",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        when (val state = mosqueState) {
            is MosqueUiState.Loading -> {
                 Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                     CircularProgressIndicator(modifier = Modifier.size(24.dp))
                 }
            }
            is MosqueUiState.Error -> {
                ErrorCard(
                    message = "Ada kendala di third party nya, silahkan coba lagi",
                    onRefresh = { viewModel.fetchPrayerTimesWithLocation(context) }
                )
            }
            is MosqueUiState.Success -> {
                 NearestMosqueSection(mosques = state.mosques) { mosque ->
                     val gmmIntentUri = android.net.Uri.parse("google.navigation:q=${mosque.lat},${mosque.lon}")
                     val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                     mapIntent.setPackage("com.google.android.apps.maps")
                     
                     // Verify if the intent can be resolved
                     // Since standard Android often has maps, we try. If null, maybe open in browser.
                     try {
                         context.startActivity(mapIntent)
                     } catch (e: Exception) {
                         // Fallback to browser
                         val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${mosque.lat},${mosque.lon}"))
                         context.startActivity(browserIntent)
                     }
                 }
            }
        }

        // Add extra spacer for bottom navigation overlap prevention
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun NearestMosqueSection(mosques: List<Mosque>, onMosqueClick: (Mosque) -> Unit) {
    if (mosques.isEmpty()) {
        Card(
             modifier = Modifier.fillMaxWidth(),
             colors = CardDefaults.cardColors(containerColor = Color.White),
             elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
             shape = RoundedCornerShape(20.dp)
        ) {
             Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                 Text("Tidak ada masjid ditemukan di sekitar.", color = Color.Gray)
             }
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(mosques.take(10)) { mosque ->
                MosqueItemCard(mosque = mosque, onClick = { onMosqueClick(mosque) })
            }
        }
    }
}

@Composable
fun MosqueItemCard(mosque: Mosque, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .height(140.dp) // Fixed height for uniformity
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Initial elevation
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon + Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = "Mosque",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Distance Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Distance",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f km", mosque.distance / 1000),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Content: Name + Address
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = mosque.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.Black, // Darker text for better readability
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(
                         imageVector = Icons.Filled.Info, // Placeholder for address icon or just use text
                         contentDescription = null, // decorative
                         tint = Color.Gray,
                         modifier = Modifier.size(12.dp) 
                     )
                     Spacer(modifier = Modifier.width(4.dp))
                     Text(
                        text = mosque.address ?: "Alamat tidak tersedia",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                     )
                }
            }
        }
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
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
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
fun ErrorCard(message: String, onRefresh: (() -> Unit)? = null) {
    Card(
         modifier = Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            
            onRefresh?.let {
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = it,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Coba Lagi", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun PrayerTimesCard(prayerData: PrayerData, locationName: String) {
    var expanded by remember { mutableStateOf(false) }
    val nextPrayer = remember(prayerData) { PrayerTimeUtils.getNextPrayer(prayerData.times) }
    val cardBgColor = Color(0xFFF5F7FA) // Very light gray/blue
    val activeColor = MaterialTheme.colorScheme.primary // Brand color

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

@Composable
fun PrayerChecklistCard(
    prayerLog: PrayerLog,
    prayerData: PrayerData?,
    selectedDate: String,
    onToggle: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Date Validation Logic
    val isTodayOrPast = remember(selectedDate) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(selectedDate) ?: java.util.Date()
        val today = java.util.Date()
        // Determine relationship: 0=today, <0=past, >0=future (roughly)
        // Simplification: compare strings if format is fixed
        val todayStr = sdf.format(today)
        selectedDate <= todayStr
    }
    
    val isStrictlyPast = remember(selectedDate) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = sdf.format(java.util.Date())
        selectedDate < todayStr
    }

    // Find next prayer or current active one
    val nextPrayerPair = remember(prayerData) {
        prayerData?.let { PrayerTimeUtils.getNextPrayer(it.times) }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tracker Ibadah",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.DarkGray
                    )
                    if (!expanded && nextPrayerPair != null) {
                        Text(
                            text = "Yuk, persiapkan sholat ${nextPrayerPair.first}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color.Gray
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                if (prayerData == null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    val prayers = listOf(
                        Triple("Subuh", "fajr", prayerData.times.fajr),
                        Triple("Dzuhur", "dhuhr", prayerData.times.dhuhr),
                        Triple("Ashar", "asr", prayerData.times.asr),
                        Triple("Maghrib", "maghrib", prayerData.times.maghrib),
                        Triple("Isya", "isha", prayerData.times.isha)
                    )
                    
                    prayers.forEachIndexed { index, (label, key, time) ->
                        val isChecked = when (key) {
                            "fajr" -> prayerLog.fajr
                            "dhuhr" -> prayerLog.dhuhr
                            "asr" -> prayerLog.asr
                            "maghrib" -> prayerLog.maghrib
                            "isha" -> prayerLog.isha
                            else -> false
                        }
                        
                        val isTimePassed = remember(time, isStrictlyPast, isTodayOrPast) {
                            if (isStrictlyPast) true
                            else if (!isTodayOrPast) false // Future date
                            else PrayerTimeUtils.hasTimePassed(time) // Today
                        }
                        
                        // Styling for state
                        val textColor = if (isTimePassed) Color.DarkGray else Color.Gray.copy(alpha = 0.6f)
                        val checkEnabled = isTimePassed
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (checkEnabled) {
                                        onToggle(key, !isChecked) 
                                    } else {
                                        Toast.makeText(context, "Belum masuk waktunya", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = textColor
                                )
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTimePassed) Color.Gray else Color.Gray.copy(alpha = 0.4f)
                                )
                            }
                            
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { 
                                    if (checkEnabled) {
                                        onToggle(key, it) 
                                    } else {
                                        Toast.makeText(context, "Belum masuk waktunya", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = true, // We handle "enabled" logic manually to allow click for Toast
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = if (isTimePassed) Color.Gray else Color.LightGray.copy(alpha = 0.4f),
                                    disabledCheckedColor = Color.LightGray,
                                    disabledUncheckedColor = Color.LightGray.copy(alpha = 0.2f)
                                )
                            )
                        }
                        if (index < prayers.lastIndex) {
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        }
                    }
                }
            } else {
               // Collapsed View Content (Optional specific UI if needed besides header)
            }
        }
    }
}

@Composable
fun DailyProgressCard(
    prayerLog: PrayerLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine completed count for text
    val completedCount = listOf(
        prayerLog.fajr,
        prayerLog.dhuhr,
        prayerLog.asr,
        prayerLog.maghrib,
        prayerLog.isha
    ).count { it }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Progress Sholat",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(contentAlignment = Alignment.Center) {
                // Segmented Progress
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    
                    // 5 segments, 360 degrees. Each segment ~72 degrees.
                    // Let's leave a small gap of ~4 degrees.
                    val segmentAngle = 72f
                    val gapAngle = 6f // Gap between segments
                    val sweepAngle = segmentAngle - gapAngle
                    
                    // Order: Fajr (top-ish?), Dhuhr, Asr, Maghrib, Isha
                    // Starting from -90 (top)
                    
                    val prayersStatus = listOf(
                        prayerLog.fajr,
                        prayerLog.dhuhr,
                        prayerLog.asr,
                        prayerLog.maghrib,
                        prayerLog.isha
                    )
                    
                    prayersStatus.forEachIndexed { index, isCompleted ->
                        val startAngle = -90f + (index * segmentAngle)
                        
                        drawArc(
                            color = if (isCompleted) primaryColor else trackColor,
                            startAngle = startAngle + (gapAngle / 2),
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                }
                
                // Text in center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$completedCount/5",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DateNavigatorCard(
    date: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Parse date for separate components
    // Input: yyyy-MM-dd
    // Output 1: Day Name (e.g. Senin)
    // Output 2: Date (e.g. 5 Jan 2026)
    
    val dateComponents = remember(date) {
        try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val dateObj = inputFormat.parse(date) ?: java.util.Date()
            
            val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale("id", "ID"))
            val dateFormat = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale("id", "ID"))
            
            Pair(dayFormat.format(dateObj), dateFormat.format(dateObj))
        } catch (e: Exception) {
            Pair(date, "")
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
             Text(
                text = "Tanggal",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateComponents.first, // Day Name
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.Gray
                )
                Text(
                    text = dateComponents.second, // Date
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.DarkGray
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Navigation Buttons Row
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly, // Centered/Evenly spaced
                verticalAlignment = Alignment.CenterVertically
            ) {
                 IconButton(
                    onClick = onPrevious, 
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Prev",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                 IconButton(
                    onClick = onNext, 
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerProgressDetailDialog(
    prayerLog: PrayerLog,
    prayerData: PrayerData?,
    onDismiss: () -> Unit
) {
    if (prayerData == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Detail Ibadah",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val prayers = listOf(
                    Triple("Subuh", prayerLog.fajr, prayerData.times.fajr),
                    Triple("Dzuhur", prayerLog.dhuhr, prayerData.times.dhuhr),
                    Triple("Ashar", prayerLog.asr, prayerData.times.asr),
                    Triple("Maghrib", prayerLog.maghrib, prayerData.times.maghrib),
                    Triple("Isya", prayerLog.isha, prayerData.times.isha)
                )

                prayers.forEach { (name, isDone, time) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.DarkGray
                            )
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        
                        Icon(
                            imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, // Use a hollow circle logic if needed, but standard Outline check is fine or just a gray circle
                            contentDescription = if (isDone) "Completed" else "Not Completed",
                            tint = if (isDone) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}



