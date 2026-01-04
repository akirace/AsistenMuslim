package com.aghatis.asmal.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aghatis.asmal.ui.theme.DarkGreen
import com.aghatis.asmal.ui.theme.Teal

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun MenuTabScreen() {
    val menuItems = listOf(
        MenuItem("Al-Qur'an", Icons.Filled.Star, Teal), // Star for Guidance
        MenuItem("How to Pray", Icons.Filled.Person, Color(0xFFE91E63)), // Person for Body/Posture
        MenuItem("Qibla", Icons.Filled.LocationOn, Color(0xFFFF9800)), // Location for Direction
        MenuItem("Dzikir", Icons.Filled.Favorite, Color(0xFF9C27B0)), // Heart for Soul
        MenuItem("Doa", Icons.Filled.CheckCircle, Color(0xFF2196F3)), // Check for Granting
        MenuItem("Hadith", Icons.Filled.Info, Color(0xFF009688)) // Info for Knowledge
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9)) // Very light gray background
            .padding(24.dp)
    ) {
        // Section 1: Intro / Guidance
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lengkapi Ibadahmu",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Temukan kedamaian hati dan kemudahan dalam mendekatkan diri kepada Sang Pencipta melalui fitur-fitur pilihan ini.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Section 2: Main Menu Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for bottom nav
        ) {
            items(menuItems) { item ->
                MenuGridItem(item = item)
            }
        }
    }
}

@Composable
fun MenuGridItem(item: MenuItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f) // Slightly rectangular
            .clickable { /* Handle click */ },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Container
            Surface(
                shape = CircleShape,
                color = item.color.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = Color.DarkGray, // Dark text
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
