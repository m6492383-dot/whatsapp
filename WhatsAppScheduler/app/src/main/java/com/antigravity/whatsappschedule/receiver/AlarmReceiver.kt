package com.antigravity.whatsappschedule.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.antigravity.whatsappschedule.util.AlarmScheduler
import com.antigravity.whatsappschedule.worker.SendWhatsAppWorker

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(AlarmScheduler.EXTRA_MESSAGE_ID, -1L)
        if (messageId != -1L) {
            val inputData = Data.Builder()
                .putLong(SendWhatsAppWorker.KEY_MESSAGE_ID, messageId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SendWhatsAppWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
