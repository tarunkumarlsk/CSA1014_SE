package com.example.kidshield.services

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.kidshield.R
import com.example.kidshield.network.BackendManager

/**
 * AppBlockerService runs on the CHILD device.
 * Every 30 seconds it:
 *   1. Polls the backend for the parent's blocked-app list.
 *   2. Checks which app is currently in the foreground.
 *   3. If the foreground app is blocked, it sends the user back to the home screen.
 */
class AppBlockerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val POLL_INTERVAL = 5000L           // 5 seconds for backend sync (faster blocking response)
    private val CHECK_INTERVAL = 1000L          // 1 second for snappy blocking response
    private var activeChildId: Int = -1

    @Volatile private var blockedApps: Set<String> = emptySet()

    private val CHANNEL_ID = "AppBlockerChannel"
    private val NOTIFICATION_ID = 99999

    // --- Overlay UI components ---
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private var currentOverlayTitle = ""
    private var currentOverlayMessage = ""

    // --- Foreground check loop ---
    private val enforcerRunnable = object : Runnable {
        override fun run() {
            enforceForegroundBlock()
            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }

    // --- Backend poll loop ---
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (activeChildId != -1) {
                fetchBlockedApps()
                fetchLimits() // Fetch limits periodically too
            }
            handler.postDelayed(this, POLL_INTERVAL) // 3 seconds
        }
    }

    @Volatile private var cachedLimits: List<com.example.kidshield.models.ScreenTimeLimitResponse> = emptyList()

    private fun fetchLimits() {
        BackendManager.getScreenTimeLimits(activeChildId, object : BackendManager.ApiCallback<List<com.example.kidshield.models.ScreenTimeLimitResponse>> {
            override fun onSuccess(result: List<com.example.kidshield.models.ScreenTimeLimitResponse>) {
                cachedLimits = result
                Log.d("AppBlocker", "Limits updated: $cachedLimits")
            }
            override fun onError(error: String) {
                Log.e("AppBlocker", "Could not fetch limits: $error")
            }
        })
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initOverlayView()
    }

    private fun initOverlayView() {
        if (!android.provider.Settings.canDrawOverlays(this)) return
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Ensure UI runs on main thread, but we create standard layout params here.
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = com.example.kidshield.utils.SessionManager.getInstance(this)
        activeChildId = session.childId

        if (activeChildId == -1) {
            Log.e("AppBlocker", "No active child ID found. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KidShield Protection")
            .setContentText("App controls are active and monitoring.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) { 
            Log.e("AppBlocker", "Cannot start foreground: ${e.message}") 
        }

        // Check if we have required permissions
        if (!hasUsageStatsPermission()) {
            Log.e("AppBlocker", "CRITICAL ERROR: Usage Stats Permission is NOT GRANTED. App blocking will NOT work.")
            // Ideally notify the parent here
            BackendManager.sendAlert(activeChildId, "app_misuse", "KidShield has lost required permissions on the child device. App blocking is disabled.", object : BackendManager.ApiCallback<String> {
                override fun onSuccess(result: String) {}
                override fun onError(error: String) {}
            })
        }

        // Start loops
        handler.removeCallbacksAndMessages(null)
        handler.post(pollRunnable)
        handler.post(enforcerRunnable)

        return START_STICKY
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun fetchBlockedApps() {
        if (activeChildId == -1) return
        BackendManager.getBlockedApps(activeChildId, object : BackendManager.ApiCallback<List<String>> {
            override fun onSuccess(result: List<String>) {
                blockedApps = result.toHashSet()
                Log.d("AppBlocker", "Blocked apps updated: $blockedApps")
            }
            override fun onError(error: String) {
                Log.e("AppBlocker", "Could not fetch blocked apps: $error")
            }
        })
    }

    private fun enforceForegroundBlock() {
        val foreground = getForegroundAppName() ?: return
        val limits = cachedLimits
        val overallLimit = limits.find { it.appName.isNullOrEmpty() }
        
        Log.d("AppBlocker", "Checking foreground: $foreground")

        if (overallLimit != null) {
            val isBedtime = overallLimit.bedtimeEnabled && isTimeInRange(overallLimit.bedtimeStart, overallLimit.bedtimeEnd)
            val isSchool = overallLimit.schoolHoursEnabled && isWeekday() && isTimeInRange(overallLimit.schoolStart, overallLimit.schoolEnd)
            
            if (isBedtime) {
                showOverlay("Bedtime Mode Active", "Device restricted until ${overallLimit.bedtimeEnd}")
                return
            }
            if (isSchool) {
                showOverlay("School Hours Active", "Device restricted during school (${overallLimit.schoolStart} - ${overallLimit.schoolEnd})")
                return
            }

            // Check daily limit locally
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis
            
            val statsList = usageStatsManager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, now)
            val totalMinutesLocal = (statsList?.sumOf { it.totalTimeInForeground } ?: 0L) / (1000 * 60)
            
            if (totalMinutesLocal >= overallLimit.limitMinutes) {
                showOverlay("Daily Limit Reached", "Used ${totalMinutesLocal}m (Limit: ${overallLimit.limitMinutes}m)")
                return
            }
        }
        
        // If no overall limits hit, check individually blocked apps
        checkBlockedAppsOnly(foreground)
    }

    private fun isTimeInRange(startTime: String?, endTime: String?): Boolean {
        if (startTime == null || endTime == null) return false
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
                currentMins >= startMins || currentMins <= endMins
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun isWeekday(): Boolean {
        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        return day != java.util.Calendar.SATURDAY && day != java.util.Calendar.SUNDAY
    }

    private fun checkBlockedAppsOnly(foreground: String) {
        if (blockedApps.isEmpty()) {
            Log.d("AppBlocker", "No blocked apps in list (list is empty)")
            removeOverlay()
            return
        }

        Log.d("AppBlocker", "Checking '$foreground' against blocked list: $blockedApps")

        // We match against the label (human readable)
        val isBlocked = blockedApps.any { blocked ->
            foreground.equals(blocked, ignoreCase = true) ||
            foreground.contains(blocked, ignoreCase = true) ||
            blocked.contains(foreground, ignoreCase = true)
        }

        if (isBlocked) {
            Log.i("AppBlocker", "BLOCKED: $foreground — showing overlay")
            showOverlay("App Blocked", "Your parent has restricted access to $foreground.")
        } else {
            Log.d("AppBlocker", "NOT blocked: $foreground")
            removeOverlay()
        }
    }


    private fun showOverlay(title: String, message: String) {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            // No overlay permission — fall back to going home
            goToHome()
            return
        }

        // If same overlay content is already showing, do nothing
        if (isOverlayShowing && currentOverlayTitle == title && currentOverlayMessage == message) return

        // If different content (e.g. different app blocked), remove old overlay and show new one
        if (isOverlayShowing) {
            removeOverlay()
        }

        currentOverlayTitle = title
        currentOverlayMessage = message

        handler.post {
            try {
                if (windowManager == null) {
                    windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                }

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                )

                val ll = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.parseColor("#EF4444")) // Red
                    setPadding(64, 64, 64, 64)
                    isClickable = true
                    isFocusable = true
                }

                val icon = android.widget.ImageView(this).apply {
                    setImageResource(android.R.drawable.ic_lock_lock)
                    setColorFilter(Color.WHITE)
                    layoutParams = android.widget.LinearLayout.LayoutParams(250, 250)
                }

                val tv = TextView(this).apply {
                    text = title
                    textSize = 28f
                    setTextColor(Color.WHITE)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(0, 48, 0, 16)
                }

                val subTv = TextView(this).apply {
                    text = message
                    textSize = 18f
                    setTextColor(Color.parseColor("#FEE2E2"))
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 80)
                }

                val btn = Button(this).apply {
                    text = "CLOSE APP"
                    setTextColor(Color.parseColor("#EF4444"))
                    setBackgroundColor(Color.WHITE)
                    setPadding(40, 20, 40, 20)
                    setOnClickListener {
                        removeOverlay()
                        goToHome()
                    }
                }

                ll.addView(icon)
                ll.addView(tv)
                ll.addView(subTv)
                ll.addView(btn)

                overlayView = ll
                windowManager?.addView(overlayView, params)
                isOverlayShowing = true
            } catch (e: Exception) {
                Log.e("AppBlocker", "Overlay fail: ${e.message}")
            }
        }
    }

    private fun removeOverlay() {
        if (!isOverlayShowing) return
        handler.post {
            try {
                overlayView?.let { windowManager?.removeView(it) }
                overlayView = null
                isOverlayShowing = false
            } catch (e: Exception) { }
        }
    }

    private fun getForegroundAppName(): String? {
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val now = System.currentTimeMillis()

            // IMPORTANT: Query a 10-second window — UsageStatsManager does NOT update faster than ~1-2s.
            // Querying a 3s window often returns empty. We then filter the results by recency.
            val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, now - 10_000, now)

            if (stats.isNullOrEmpty()) {
                Log.w("AppBlocker", "UsageStats returned empty — check Usage Access permission")
                return null
            }

            // Filter: only apps used in the last 3 seconds (truly in foreground)
            val recentApp = stats
                .filter { it.lastTimeUsed > now - 3000 && !it.packageName.isNullOrEmpty() }
                .maxByOrNull { it.lastTimeUsed }
                ?: return null

            val pkg = recentApp.packageName
            if (pkg == packageName) return null // Don't block ourselves

            // Skip known system packages, launchers, input methods
            val systemPrefixes = listOf(
                "com.android", "android", "com.google.android.inputmethod",
                "com.google.android.gms", "com.google.android.gsf",
                "com.samsung.android.launcher", "com.sec.android",
                "com.miui", "com.oneplus", "com.oppo", "com.realme",
                "com.vivo", "com.iqoo", "com.huawei.android.launcher"
            )
            if (systemPrefixes.any { pkg.startsWith(it) }) return null

            val pm = packageManager
            return try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                Log.d("AppBlocker", "Foreground: $label ($pkg)")
                label
            } catch (e: Exception) {
                Log.w("AppBlocker", "Could not get label for $pkg, using pkg name")
                pkg
            }
        } catch (e: Exception) {
            Log.e("AppBlocker", "Error getting foreground app: ${e.message}")
            return null
        }
    }


    private fun goToHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e("AppBlocker", "Cannot go home: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocker Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        handler.removeCallbacks(pollRunnable)
        handler.removeCallbacks(enforcerRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
