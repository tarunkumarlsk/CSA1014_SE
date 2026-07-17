package com.example.kidshield.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.kidshield.network.BackendManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenTimeService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var childId: Int = 1
    private val POLLING_INTERVAL = 60000L // 1 minute

    private val screenUsageRunnable = object : Runnable {
        override fun run() {
            uploadRealScreenUsage()
            handler.postDelayed(this, POLLING_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ScreenTimeService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ScreenTimeService", "Service Started")
        
        val session = com.example.kidshield.utils.SessionManager.getInstance(this)
        childId = session.childId
        if (childId == -1) childId = 1 // Basic fallback
        
        handler.post(screenUsageRunnable)

        return START_STICKY
    }

    private fun uploadRealScreenUsage() {
        val usageStatsManager = getSystemService(android.content.Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val endTime = System.currentTimeMillis()
        
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis // Since midnight
        
        val usageStats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (usageStats != null && usageStats.isNotEmpty()) {
            var totalDailyMinutes = 0
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            for (stats in usageStats) {
                val usageTimeMinutes = (stats.totalTimeInForeground / (1000 * 60)).toInt()
                if (usageTimeMinutes > 0) {
                    totalDailyMinutes += usageTimeMinutes
                    
                    // Sync each app usage
                    BackendManager.uploadScreenUsage(
                        childId = childId,
                        appName = stats.packageName,
                        usageTimeMinutes = usageTimeMinutes,
                        date = todayDate,
                        callback = object : BackendManager.ApiCallback<String> {
                            override fun onSuccess(result: String) {}
                            override fun onError(error: String) {}
                        }
                    )
                }
            }
            // Trigger alerts based on total minutes
            checkLimitsAndTriggerAlert(totalDailyMinutes)
        } else {
            Log.w("ScreenTimeService", "No usage stats available. Ensure 'Usage Access' permission is granted.")
        }
    }

    private fun checkLimitsAndTriggerAlert(currentUsageMinutes: Int) {
        BackendManager.getScreenTimeLimits(childId, object : BackendManager.ApiCallback<List<com.example.kidshield.models.ScreenTimeLimitResponse>> {
            override fun onSuccess(limits: List<com.example.kidshield.models.ScreenTimeLimitResponse>) {
                val overallLimit = limits.find { it.appName.isNullOrEmpty() }
                if (overallLimit != null) {
                    // 1. Check Daily Limit
                    if (currentUsageMinutes > overallLimit.limitMinutes) {
                        Log.d("ScreenTimeService", "Limit Exceeded! Usage: $currentUsageMinutes, Limit: ${overallLimit.limitMinutes}")
                        BackendManager.sendAlert(
                            childId, 
                            "screen_time", 
                            "Child has exceeded their daily screen time limit (${currentUsageMinutes}m / ${overallLimit.limitMinutes}m)",
                            object : BackendManager.ApiCallback<String> {
                                override fun onSuccess(res: String) {}
                                override fun onError(err: String) {}
                            }
                        )
                    }

                    // 2. Check Bedtime Mode
                    if (overallLimit.bedtimeEnabled && overallLimit.bedtimeStart != null && overallLimit.bedtimeEnd != null) {
                        if (isTimeInRange(overallLimit.bedtimeStart, overallLimit.bedtimeEnd)) {
                            Log.d("ScreenTimeService", "Bedtime Mode Active!")
                            BackendManager.sendAlert(
                                childId,
                                "screen_time",
                                "Child is using the device during Bedtime Mode (${overallLimit.bedtimeStart} - ${overallLimit.bedtimeEnd})",
                                object : BackendManager.ApiCallback<String> {
                                    override fun onSuccess(res: String) {}
                                    override fun onError(err: String) {}
                                }
                            )
                        }
                    }

                    // 3. Check School Hours
                    if (overallLimit.schoolHoursEnabled && overallLimit.schoolStart != null && overallLimit.schoolEnd != null) {
                        val calendar = java.util.Calendar.getInstance()
                        val day = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                        val isWeekday = day != java.util.Calendar.SATURDAY && day != java.util.Calendar.SUNDAY
                        if (isWeekday && isTimeInRange(overallLimit.schoolStart, overallLimit.schoolEnd)) {
                            Log.d("ScreenTimeService", "School Hours Block Active!")
                            BackendManager.sendAlert(
                                childId,
                                "school_hours",
                                "Child is using the device during School Hours (${overallLimit.schoolStart} - ${overallLimit.schoolEnd})",
                                object : BackendManager.ApiCallback<String> {
                                    override fun onSuccess(res: String) {}
                                    override fun onError(err: String) {}
                                }
                            )
                        }
                    }
                }
            }
            override fun onError(error: String) {
                Log.e("ScreenTimeService", "Failed to get limits: $error")
            }
        })
    }

    private fun isTimeInRange(startTime: String, endTime: String): Boolean {
        try {
            val now = java.util.Calendar.getInstance()
            val currentMins = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

            val s = startTime.split(":")
            val startMins = s[0].toInt() * 60 + s[1].toInt()

            val e = endTime.split(":")
            val endMins = e[0].toInt() * 60 + e[1].toInt()

            return if (startMins <= endMins) {
                currentMins in startMins..endMins
            } else {
                // Time range wraps around midnight (e.g., 9 PM to 7 AM)
                currentMins >= startMins || currentMins <= endMins
            }
        } catch (e: Exception) {
            return false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(screenUsageRunnable)
        Log.d("ScreenTimeService", "Service Destroyed")
    }
}
