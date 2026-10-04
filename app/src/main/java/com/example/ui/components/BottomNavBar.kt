package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrystalGlassWhiteBorder
import com.example.ui.viewmodel.AppDestination

data class NavItem(
    val destination: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

/**
 * BottomNavBar
 *
 * Implements Part 2 "Crystal" Glassmorphic Navigation Dock:
 * - Translucent backdrop with active theme surface color.
 * - Ultra-thin 1dp frosted top border with specular sheen gradient.
 * - Active pill indicator with glowing accent border.
 * - Dynamically recolors with Imperial Gold, Midnight, Cyberpunk, and Hollywood Rouge palettes.
 */
@Composable
fun BottomNavBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(
            destination = AppDestination.HOME,
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            tag = "nav_home"
        ),
        NavItem(
            destination = AppDestination.CATEGORIES,
            label = "Categories",
            selectedIcon = Icons.Filled.GridView,
            unselectedIcon = Icons.Outlined.GridView,
            tag = "nav_categories"
        ),
        NavItem(
            destination = AppDestination.WATCHLIST,
            label = "Watchlist",
            selectedIcon = Icons.Filled.Bookmark,
            unselectedIcon = Icons.Outlined.BookmarkBorder,
            tag = "nav_watchlist"
        ),
        NavItem(
            destination = AppDestination.DOWNLOADS,
            label = "Downloads",
            selectedIcon = Icons.Filled.FileDownload,
            unselectedIcon = Icons.Outlined.FileDownload,
            tag = "nav_downloads"
        ),
        NavItem(
            destination = AppDestination.SETTINGS,
            label = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            tag = "nav_settings"
        )
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Ultra-thin 1dp frosted top border with specular sheen
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            CrystalGlassWhiteBorder,
                            primaryColor.copy(alpha = 0.40f),
                            CrystalGlassWhiteBorder,
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentDestination == item.destination
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigate(item.destination) }
                        .testTag(item.tag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        primaryColor.copy(alpha = 0.18f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (isSelected) primaryColor else onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) primaryColor else onSurfaceVariant.copy(alpha = 0.70f)
                        )
                    }
                }
            }
        }
    }
}
