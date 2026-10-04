package com.example.data.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Categorized network speed profile to drive intelligent stream quality decisions.
 */
enum class NetworkSpeedCategory(
    val label: String,
    val estimatedKbps: Int,
    val recommendedQuality: PlaybackQualityProfile
) {
    EXCELLENT("Ultra High Speed (WiFi/5G)", 25_000, PlaybackQualityProfile.FHD_1080P),
    GOOD("Fast Connection (4G/LTE)", 8_000, PlaybackQualityProfile.HD_720P),
    MODERATE("Moderate Speed (3G/HSPA)", 2_500, PlaybackQualityProfile.SD_480P),
    LOW("Low Bandwidth / Weak Signal", 1_000, PlaybackQualityProfile.SD_480P)
}

/**
 * Real-time Network Speed & Bandwidth Estimator.
 * - Queries ConnectivityManager for hardware downstream bandwidth capabilities
 * - Manages a shared, calibrated Media3 DefaultBandwidthMeter with instant initial estimates
 * - Eliminates the 5-10 second initial measurement delay in ExoPlayer adaptive streams
 */
@OptIn(UnstableApi::class)
object NetworkSpeedManager {

    private const val TAG = "NetworkSpeedManager"

    private val _currentSpeedCategory = MutableStateFlow(NetworkSpeedCategory.GOOD)
    val currentSpeedCategory: StateFlow<NetworkSpeedCategory> = _currentSpeedCategory.asStateFlow()

    private val _currentSpeedDescription = MutableStateFlow("Optimized for Auto Playback")
    val currentSpeedDescription: StateFlow<String> = _currentSpeedDescription.asStateFlow()

    @Volatile
    private var bandwidthMeterInstance: DefaultBandwidthMeter? = null

    /**
     * Obtains or initializes the tuned DefaultBandwidthMeter.
     * Starts with instant calibrated estimates based on the active connection type.
     */
    fun getBandwidthMeter(context: Context): DefaultBandwidthMeter {
        val existing = bandwidthMeterInstance
        if (existing != null) return existing

        return synchronized(this) {
            bandwidthMeterInstance ?: run {
                val initialEstimate = getInstantEstimatedBitrate(context)
                val meter = DefaultBandwidthMeter.Builder(context.applicationContext)
                    .setInitialBitrateEstimate(initialEstimate)
                    .setSlidingWindowMaxWeight(2000)
                    .build()
                bandwidthMeterInstance = meter
                registerNetworkCallback(context.applicationContext)
                meter
            }
        }
    }

    /**
     * Evaluates current active network type and OS-reported downstream bandwidth.
     */
    fun getInstantEstimatedBitrate(context: Context): Long {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return 6_000_000L

            val network = cm.activeNetwork ?: return 3_000_000L
            val caps = cm.getNetworkCapabilities(network) ?: return 3_000_000L

            val linkKbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                caps.linkDownstreamBandwidthKbps
            } else {
                -1
            }

            val estimatedBps = when {
                linkKbps > 15_000 -> {
                    _currentSpeedCategory.value = NetworkSpeedCategory.EXCELLENT
                    _currentSpeedDescription.value = "High Speed (${linkKbps / 1000} Mbps) • 1080p/4K Ready"
                    linkKbps * 1000L
                }
                linkKbps > 5_000 -> {
                    _currentSpeedCategory.value = NetworkSpeedCategory.GOOD
                    _currentSpeedDescription.value = "Good Connection (${linkKbps / 1000} Mbps) • 720p/1080p"
                    linkKbps * 1000L
                }
                linkKbps in 1..5_000 -> {
                    _currentSpeedCategory.value = NetworkSpeedCategory.MODERATE
                    _currentSpeedDescription.value = "Balanced (${linkKbps / 1000} Mbps) • Smooth 480p/720p"
                    linkKbps * 1000L
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    _currentSpeedCategory.value = NetworkSpeedCategory.EXCELLENT
                    _currentSpeedDescription.value = "WiFi Connected • High Speed Stream"
                    15_000_000L
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    _currentSpeedCategory.value = NetworkSpeedCategory.GOOD
                    _currentSpeedDescription.value = "Cellular Network • Adaptive Balance"
                    6_000_000L
                }
                else -> {
                    _currentSpeedCategory.value = NetworkSpeedCategory.MODERATE
                    _currentSpeedDescription.value = "Connected • Adaptive Stream"
                    3_000_000L
                }
            }

            estimatedBps.coerceAtLeast(1_000_000L)
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating instant bitrate estimate: ${e.message}")
            6_000_000L
        }
    }

    private var isCallbackRegistered = false

    private fun registerNetworkCallback(context: Context) {
        if (isCallbackRegistered) return
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val linkKbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        caps.linkDownstreamBandwidthKbps
                    } else -1

                    when {
                        linkKbps > 15_000 || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            _currentSpeedCategory.value = NetworkSpeedCategory.EXCELLENT
                            _currentSpeedDescription.value = "Fast Network • Seamless HD Flow"
                        }
                        linkKbps > 5_000 || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            _currentSpeedCategory.value = NetworkSpeedCategory.GOOD
                            _currentSpeedDescription.value = "Cellular Active • Smooth 720p/1080p"
                        }
                        else -> {
                            _currentSpeedCategory.value = NetworkSpeedCategory.MODERATE
                            _currentSpeedDescription.value = "Conserving Data • Zero-Buffer Stream"
                        }
                    }
                }
            })
            isCallbackRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback: ${e.message}")
        }
    }
}
