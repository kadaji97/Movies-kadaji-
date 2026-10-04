package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.data.model.StreamServer
import com.example.ui.theme.*

@Composable
fun MultiServerModal(
    mediaItem: MediaItem,
    onServerSelected: (StreamServer) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("multi_server_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyanGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Servers",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Select Stream Server",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Multi-Server Redundancy Active",
                                fontSize = 11.sp,
                                color = CyberCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_server_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Streaming: ${mediaItem.title}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Server List
                mediaItem.streamServers.forEachIndexed { index, server ->
                    val isPrimary = index == 0
                    val isSecondary = index == 1

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceHighlight,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPrimary) CyberCyan.copy(alpha = 0.5f) else DarkOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onServerSelected(server) }
                            .testTag("server_item_${server.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Status Indicator Dot
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPrimary) NeonGreen else if (isSecondary) CyberCyan else NeonAmber
                                        )
                                )
                                Column {
                                    Text(
                                        text = server.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = server.quality,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPrimary) CyberCyan else NeonAmber
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 11.sp,
                                            color = TextTertiary
                                        )
                                        Text(
                                            text = server.bitrate,
                                            fontSize = 11.sp,
                                            color = TextTertiary
                                        )
                                    }
                                }
                            }

                            // Ping + Launch Play Action
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Ping",
                                        tint = if (server.pingMs < 50) NeonGreen else NeonAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${server.pingMs}ms",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (server.pingMs < 50) NeonGreen else NeonAmber
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isPrimary) CyberCyan else DarkSurface)
                                        .border(1.dp, if (isPrimary) CyberCyan else DarkOutline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play on server",
                                        tint = if (isPrimary) Color(0xFF07080B) else TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Automated Failover Engine: If your stream encounters jitter or network disconnection, Movieskadaji switches between redundant CDN paths automatically.",
                    fontSize = 10.sp,
                    color = TextTertiary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
