package com.aghatis.asmal.ui.home

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.aghatis.asmal.data.model.AyahResponse
import com.aghatis.asmal.data.repository.Mosque
import com.aghatis.asmal.data.repository.QuranRepository
import com.aghatis.asmal.data.repository.MosqueRepository
import com.aghatis.asmal.data.model.PrayerLog
import com.aghatis.asmal.data.repository.PrayerLogRepository
import com.aghatis.asmal.data.repository.BackgroundRepository

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    
    // Database and DAO instantiation
    val db = remember {
        androidx.room.Room.databaseBuilder(
            context.applicationContext,
            com.aghatis.asmal.data.local.AppDatabase::class.java, "asmal-db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    val prefsRepository = remember { PrefsRepository(context) } // Remember these to avoid recreation
    val prayerRepository = remember { PrayerRepository(db.prayerDao()) }
    val quranRepository = remember { QuranRepository(context) }
    val mosqueRepository = remember { MosqueRepository(context, db.mosqueDao()) }
    val prayerLogRepository = remember { PrayerLogRepository() }
    val backgroundRepository = remember { BackgroundRepository() }
    
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            prefsRepository, 
            prayerRepository, 
            quranRepository, 
            mosqueRepository, 
            prayerLogRepository,
            backgroundRepository
        )
    )

    val user by viewModel.userState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val ayahState by viewModel.ayahState.collectAsState()
    val mosqueState by viewModel.mosqueState.collectAsState()
    val prayerLog by viewModel.prayerLogState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val prayerProgress by viewModel.prayerProgress.collectAsState()
    val backgroundUrl by viewModel.currentBackgroundUrl.collectAsState()
    
    // Detect system theme and update ViewModel
    val isDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(isDarkTheme) {
        viewModel.updateTheme(isDarkTheme)
    }

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
            .verticalScroll(rememberScrollState()) // Allow scrolling
    ) {
        // 1. New Home Header Section (Includes Greeting, Clock, and Prayer Times)
        val currentPrayerData = (uiState as? HomeUiState.Success)?.prayerData
        val currentLocationName = (uiState as? HomeUiState.Success)?.locationName
        HomeHeaderSection(
            userName = user?.displayName,
            photoUrl = user?.photoUrl,
            prayerData = currentPrayerData,
            locationName = currentLocationName,
            backgroundUrl = backgroundUrl
        )

        Column(modifier = Modifier.padding(screenPadding())) {
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
            
            NearestMosqueSection(
                mosqueState = mosqueState,
                onMosqueClick = { mosque ->
                     val gmmIntentUri = android.net.Uri.parse("google.navigation:q=${mosque.lat},${mosque.lon}")
                     val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                     mapIntent.setPackage("com.google.android.apps.maps")
                     
                     try {
                         context.startActivity(mapIntent)
                     } catch (e: Exception) {
                         // Fallback to browser
                         val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${mosque.lat},${mosque.lon}"))
                         context.startActivity(browserIntent)
                     }
                },
                onRefresh = { viewModel.refresh(context) }
            )

            // Add extra spacer for bottom navigation overlap prevention
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun HomeHeaderSection(
    userName: String?,
    photoUrl: String?,
    prayerData: PrayerData?,
    locationName: String?,
    isLoading: Boolean = false,
    backgroundUrl: String? = null
) {
    // Background Image URL - Use dynamic from API if available, otherwise fallback to default
    val bgImage = backgroundUrl ?: "https://api.aghatis.id/uploads/masjid_background_84c945e1cc.png"

    // Current Time State
    var currentTime by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    // ... existing launched effect ...
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = java.util.Calendar.getInstance()
            kotlinx.coroutines.delay(1000L) // Update every second
        }
    }
    
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val timeString = timeFormat.format(currentTime.time)
    
    // Determine next prayer
    val nextPrayerPair = remember(prayerData) {
        prayerData?.let { PrayerTimeUtils.getNextPrayer(it.times) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Color.Black) // Fallback color
    ) {
        // Background Image with Caching (handled by Coil default)
        AsyncImage(
            model = androidx.compose.ui.platform.LocalContext.current.let { context ->
                coil.request.ImageRequest.Builder(context)
                    .data(bgImage)
                    .crossfade(true)
                    .build()
            },
            contentDescription = "Mosque Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        
        // Dark Overlay for readability
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp) // Maintain bottom padding for content
                .padding(top = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            // Top Row: Greeting + Avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "As-salamu alaykum",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userName ?: "Saudaraku",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                     AsyncImage(
                        model = photoUrl ?: "https://ui-avatars.com/api/?name=${userName ?: "User"}&background=random&color=fff",
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Center Clock
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 Text(
                    text = timeString,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 64.sp
                    ),
                    color = Color.White
                 )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Prayer Times Horizontal List Section
            Column(modifier = Modifier.fillMaxWidth()) {
                // Location Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationName ?: "Menemukan Lokasi...",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                // Prayer Times Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                     Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .horizontalScroll(rememberScrollState()),
                             horizontalArrangement = Arrangement.SpaceEvenly, // Distribute evenly
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             if (isLoading) {
                                  // Shimmer Loading
                                  val brush = com.aghatis.asmal.ui.components.shimmerBrush()
                                  repeat(5) {
                                      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                          Box(modifier = Modifier.width(40.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                                          Spacer(modifier = Modifier.height(8.dp))
                                          Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(brush))
                                          Spacer(modifier = Modifier.height(8.dp))
                                          Box(modifier = Modifier.width(30.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                                      }
                                  }
                             } else if (prayerData != null) {
                                val times = prayerData.times
                                val prayers = listOf(
                                    Triple("Fajr", times.fajr, Icons.Filled.WbTwilight),
                                    Triple("Dzuhr", times.dhuhr, Icons.Filled.WbSunny),
                                    Triple("Asr", times.asr, Icons.Filled.WbCloudy),
                                    Triple("Maghrib", times.maghrib, Icons.Filled.WbTwilight),
                                    Triple("Isha", times.isha, Icons.Filled.NightsStay)
                                )
                                
                                prayers.forEach { (name, time, icon) ->
                                    val isNext = nextPrayerPair?.first == name
                                    PrayerTimeItem(name = name, time = time, icon = icon, isNext = isNext)
                                }
                             } else {
                                 // Skeleton / Loading state
                                 Text("Loading Prayer Times...", modifier = Modifier.padding(16.dp))
                             }
                         }
                         
                         // Reminder Text below list inside the card (or outside?)
                         // User said "below Prayer Times Horizontal List", let's put it inside the card for cleaner UI or just below the row
                         if (nextPrayerPair != null) {
                             Spacer(modifier = Modifier.height(16.dp))
                             HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                             Spacer(modifier = Modifier.height(12.dp))
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.Center,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                  Icon(
                                      imageVector = Icons.Filled.NightsStay, // Or relevant icon
                                      contentDescription = null,
                                      tint = MaterialTheme.colorScheme.primary,
                                      modifier = Modifier.size(16.dp)
                                  )
                                  Spacer(modifier = Modifier.width(8.dp))
                                  Text(
                                      text = "Jangan lupa sholat ${nextPrayerPair.first} pukul ${nextPrayerPair.second}",
                                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                      color = MaterialTheme.colorScheme.onSurface
                                  )
                             }
                         }
                     }
                }
            }
        }
    }
}

@Composable
fun PrayerTimeItem(
    name: String, 
    time: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isNext: Boolean
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val contentColor = if (isNext) activeColor else MaterialTheme.colorScheme.onSurface
    
    // Scale or background change if next
    val scale = if (isNext) 1.1f else 1f
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium),
            color = if (isNext) activeColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Icon Circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isNext) {
                         androidx.compose.ui.graphics.Brush.linearGradient(
                             colors = listOf(activeColor, activeColor.copy(alpha = 0.7f))
                         )
                    } else {
                         androidx.compose.ui.graphics.Brush.linearGradient(
                             colors = listOf(Color(0xFFFDBB2D), Color(0xFF22C1C3)) // Default Gradient
                         )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                 imageVector = icon,
                 contentDescription = name,
                 tint = Color.White,
                 modifier = Modifier.size(24.dp)
             )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium),
            color = if (isNext) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


    @Composable
    fun NearestMosqueSection(
        mosqueState: MosqueUiState, 
        onMosqueClick: (Mosque) -> Unit = {},
        onRefresh: () -> Unit = {}
    ) {
        if (mosqueState is MosqueUiState.Error) {
             ErrorCard(
                 message = mosqueState.message.ifEmpty { "Gagal memuat masjid terdekat" },
                 onRefresh = onRefresh
             )
             return
        }

        val isLoading = mosqueState is MosqueUiState.Loading
        val mosques = (mosqueState as? MosqueUiState.Success)?.mosques ?: emptyList()

        if (isLoading) {
             LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(3) { 
                     // Shimmer Card
                     val brush = com.aghatis.asmal.ui.components.shimmerBrush()
                     Card(
                        modifier = Modifier.width(240.dp).height(140.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                     ) {
                         Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                             Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                 Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(brush))
                                 Box(modifier = Modifier.width(60.dp).height(20.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                             }
                             Column {
                                 Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                                 Spacer(modifier = Modifier.height(4.dp))
                                 Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                             }
                         }
                     }
                }
            }
        } else if (mosques.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tidak ada masjid ditemukan di sekitar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f km", mosque.distance / 1000),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondary
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
                        color = MaterialTheme.colorScheme.onSurface, // Darker text for better readability
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info, // Placeholder for address icon or just use text
                            contentDescription = null, // decorative
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = mosque.address ?: "Alamat tidak tersedia",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ayah.surahNameTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
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
                                    Toast.makeText(
                                        context,
                                        "Audio not available",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = if (isPlaying) "Playing Recitation..." else "Play Audio (Mishary Rashid)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        AsyncImage(
                            model = photoUrl
                                ?: "https://ui-avatars.com/api/?name=${userName ?: "User"}&background=random",
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }

    @Composable
    fun ErrorCard(message: String, onRefresh: (() -> Unit)? = null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                    alpha = 0.5f
                )
            )
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
        val cardBgColor =
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Subtle background
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = prayerData.date.readable,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurface
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
        val textColor =
            if (isNext) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = weight,
                color = textColor
            )
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = weight,
                color = textColor
            )
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            color = MaterialTheme.colorScheme.onSurface
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (prayerData == null) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
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
                            val textColor =
                                if (isTimePassed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.6f
                                )
                            val checkEnabled = isTimePassed

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checkEnabled) {
                                            onToggle(key, !isChecked)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Belum masuk waktunya",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                                        color = if (isTimePassed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (checkEnabled) {
                                            onToggle(key, it)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Belum masuk waktunya",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    enabled = true, // We handle "enabled" logic manually to allow click for Toast
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = if (isTimePassed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                )
                            }
                            if (index < prayers.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = 0.2f
                                    )
                                )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Date Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateComponents.first, // Day Name
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateComponents.second, // Date
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, // Use a hollow circle logic if needed, but standard Outline check is fine or just a gray circle
                            contentDescription = if (isDone) "Completed" else "Not Completed",
                            tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.3f
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}





