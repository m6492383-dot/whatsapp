package com.antigravity.whatsappschedule.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antigravity.whatsappschedule.WhatsAppSchedulerApp
import com.antigravity.whatsappschedule.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? WhatsAppSchedulerApp ?: return
            val repository = com.antigravity.whatsappschedule.data.repository.ScheduleRepository(app.database.scheduledMessageDao())

            CoroutineScope(Dispatchers.IO).launch {
                val pendingMessages = repository.getPendingMessages()
                val now = System.currentTimeMillis()
                for (message in pendingMessages) {
                    if (message.scheduledDateTime > now) {
                        AlarmScheduler.scheduleAlarm(context, message)
                    }
                }
            }
        }
    }
}
