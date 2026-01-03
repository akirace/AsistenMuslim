package com.aghatis.asmal.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(val route: String, val iconFilled: ImageVector, val iconOutlined: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Filled.Home, Icons.Outlined.Home, "Home")
    object Assistant : BottomNavItem("assistant", Icons.Filled.Star, Icons.Outlined.Star, "Assistant")
    object Profile : BottomNavItem("profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "Profile")
}


@Composable
fun CustomBottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Assistant,
        BottomNavItem.Profile
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Container for the whole bottom bar area
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp) // Outer margin
            // Add padding for the system navigation bar so it doesn't overlap
            // We apply it here so the visual bar sits above the system gestures
            .windowInsetsPadding(WindowInsets.navigationBars) 
            .height(80.dp), // Height to accommodate the floating item
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Background Layer
        // This Row holds the background shape and the inactive items
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp) // The actual 'bar' height
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(30.dp)),
            color = Color.White,
            shape = RoundedCornerShape(30.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            // Invisible placeholder to keep spacing correct
                            Box(modifier = Modifier.size(50.dp))
                        } else {
                            // Inactive Item
                            BottomNavItemView(
                                item = item,
                                isSelected = false,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Floating Overlay Layer
        // This Row mirrors the structure of the background row but contains ONLY the active floating item.
        // It ensures the floating item is positioned exactly above its corresponding placeholder.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp), // Match full container height to allow floating up
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom // Align bottom to match the visual base
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (isSelected) {
                        // The Active Item (Floating)
                        // We need to offset it UP relative to this Row's bottom.
                        FloatingActiveItem(item = item)
                    } else {
                        // Placeholder for inactive items to maintain exact spacing distribution
                        // Use a transparent box with no size requirements, the weight handles the width.
                        Spacer(modifier = Modifier.size(50.dp)) 
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavItemView(item: BottomNavItem, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp) // Enforce size for consistency
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
            contentDescription = item.label,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun FloatingActiveItem(item: BottomNavItem) {
    val activeColor = MaterialTheme.colorScheme.primary // Use Primary (DarkGreen/Teal)
    
    // The Floating Item
    // Since this is inside a Row with Alignment.Bottom, 0 offset is at the bottom.
    // The background bar is 60dp high. The container is 80dp high.
    // We want this circle (56dp) to be protruding.
    // We offset it up.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(y = (-20).dp) // Move up
    ) {
        // White Circle with Shadow
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface, // Match surface color
            shadowElevation = 4.dp,
            modifier = Modifier.size(56.dp)
        ) {
             Box(
                 modifier = Modifier.fillMaxSize(),
                 contentAlignment = Alignment.Center
             ) {
                 Icon(
                     imageVector = item.iconOutlined,
                     contentDescription = item.label,
                     tint = activeColor,
                     modifier = Modifier.size(30.dp)
                 )
             }
        }
        
        // Label
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            color = activeColor,
            fontSize = 12.sp
        )
    }
}
