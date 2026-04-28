package com.wifimapper.domain.repository

import com.wifimapper.domain.model.WifiScanResult
import kotlinx.coroutines.flow.Flow

interface WifiScanRepository {
    suspend fun scan(): List<WifiScanResult>
    fun getScanResultsStream(): Flow<List<WifiScanResult>>
    fun isWifiEnabled(): Boolean
    suspend fun setWifiEnabled(enabled: Boolean)
}
