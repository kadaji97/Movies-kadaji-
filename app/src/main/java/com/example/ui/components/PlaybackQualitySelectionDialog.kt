package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MediaItem
import com.example.data.service.NetworkSpeedManager
import com.example.data.service.PlaybackQualityProfile
import com.example.ui.theme.*

enum class QualityDialogMode {
    STREAM_PLAY,
    DOWNLOAD
}

/**
 * Aesthetic Dialog allowing users to select playback quality presets
 * or let Auto mode choose based on internet connection strength.
 */
@Composable
fun PlaybackQualitySelectionDialog(
    mediaItem: MediaItem,
    mode: QualityDialogMode,
    initialProfile: PlaybackQualityProfile = PlaybackQualityProfile.AUTO,
    onConfirm: (PlaybackQualityProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProfile by remember { mutableStateOf(initialProfile) }
    val networkDescription by NetworkSpeedManager.currentSpeedDescription.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("playback_quality_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (mode == QualityDialogMode.STREAM_PLAY) "STREAM PLAYBACK QUALITY" else "DOWNLOAD QUALITY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = networkDescription,
                            fontSize = 11.sp,
                            color = NeonAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = if (mode == QualityDialogMode.STREAM_PLAY)
                        "Select a quality profile. Auto mode automatically matches your internet speed for smooth, zero-buffering playback."
                    else
                        "Select quality for offline download storage:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                // List of profiles
                val availableProfiles = if (mode == QualityDialogMode.DOWNLOAD) {
                    listOf(
                        PlaybackQualityProfile.UHD_4K,
                        PlaybackQualityProfile.FHD_1080P,
                        PlaybackQualityProfile.HD_720P,
                        PlaybackQualityProfile.SD_480P
                    )
                } else {
                    PlaybackQualityProfile.entries
                }

                availableProfiles.forEach { profile ->
                    val isSelected = selectedProfile == profile
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CyberCyan else DarkOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedProfile = profile }
                            .testTag("dialog_quality_option_${profile.badge.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CyberCyan else TextPrimary
                                    )
                                    if (profile.isRecommended) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NeonGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ZERO BUFFER",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NeonGreen
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (mode == QualityDialogMode.DOWNLOAD) "${profile.subtitle} • Est. ${profile.downloadSizeEstimate}" else profile.subtitle,
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                    maxLines = 1
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onConfirm(selectedProfile) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color(0xFF070B11)
                        ),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(
                            text = if (mode == QualityDialogMode.STREAM_PLAY) "Play Stream" else "Start Download",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
