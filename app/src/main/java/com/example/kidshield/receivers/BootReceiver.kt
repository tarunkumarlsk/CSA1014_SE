package com.example.kidshield.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.kidshield.services.ParentAlertService
import com.example.kidshield.utils.SessionManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val session = SessionManager.getInstance(context)
            if (session.isLoggedIn && session.role == "parent") {
                val serviceIntent = Intent(context, ParentAlertService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
