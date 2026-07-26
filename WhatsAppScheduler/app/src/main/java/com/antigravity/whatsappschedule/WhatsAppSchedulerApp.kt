package com.antigravity.whatsappschedule

import android.app.Application
import com.antigravity.whatsappschedule.data.database.AppDatabase
import com.antigravity.whatsappschedule.util.NotificationHelper

class WhatsAppSchedulerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
