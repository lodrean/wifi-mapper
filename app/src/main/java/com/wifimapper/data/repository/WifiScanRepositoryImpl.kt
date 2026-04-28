package com.wifimapper.data.repository

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.wifimapper.domain.model.WifiScanResult
import com.wifimapper.domain.repository.WifiScanRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

class WifiScanRepositoryImpl(
    private val context: Context
) : WifiScanRepository {

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    @SuppressLint("MissingPermission")
    override suspend fun scan(): List<WifiScanResult> {
        if (!hasPermission()) return emptyList()

        wifiManager.startScan()
        return wifiManager.scanResults.map { result ->
            WifiScanResult(
                ssid = result.SSID ?: "",
                bssid = result.BSSID ?: "",
                rssiDbm = result.level,
                frequencyMHz = result.frequency,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun getScanResultsStream(): Flow<List<WifiScanResult>> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    if (success) {
                        val results = wifiManager.scanResults.map { result ->
                            WifiScanResult(
                                ssid = result.SSID ?: "",
                                bssid = result.BSSID ?: "",
                                rssiDbm = result.level,
                                frequencyMHz = result.frequency,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                        trySend(results)
                    }
                }
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        ContextCompat.registerReceiver(
            context,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        wifiManager.startScan()

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    override fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    override suspend fun setWifiEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
        }
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
