package com.example.kidshield.services

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.example.kidshield.network.ApiService
import com.example.kidshield.models.ScreenUsageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScreenUsageMonitor(
    private val context: Context,
    private val apiService: ApiService,
    private val childId: Int
) {
    private var isMonitoring = false
    private val monitorJob = CoroutineScope(Dispatchers.IO)

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        monitorJob.launch {
            while (isMonitoring) {
                reportScreenUsage()
                // Sleep for an hour
                delay(60 * 60 * 1000L)
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
    }

    private suspend fun reportScreenUsage() {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        for (usageStats in usageStatsList) {
            val totalTimeInForeground = usageStats.totalTimeInForeground
            if (totalTimeInForeground > 0) {
                val usageTimeMinutes = (totalTimeInForeground / (1000 * 60)).toInt()
                if (usageTimeMinutes > 0) {
                    val request = ScreenUsageRequest(
                        childId = childId,
                        appName = usageStats.packageName,
                        usageTimeMinutes = usageTimeMinutes,
                        date = currentDate
                    )
                    try {
                        apiService.uploadScreenUsage(request)
                        Log.d("ScreenUsageMonitor", "Uploaded usage for \${usageStats.packageName}")
                    } catch (e: Exception) {
                        Log.e("ScreenUsageMonitor", "Error uploading usage stats for \${usageStats.packageName}", e)
                    }
                }
            }
        }
    }
}
