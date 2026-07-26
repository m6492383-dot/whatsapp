package com.antigravity.whatsappschedule.worker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antigravity.whatsappschedule.WhatsAppSchedulerApp
import com.antigravity.whatsappschedule.data.model.FailureReason
import com.antigravity.whatsappschedule.data.model.MessageStatus
import com.antigravity.whatsappschedule.data.model.RepeatType
import com.antigravity.whatsappschedule.data.model.ScheduledMessage
import com.antigravity.whatsappschedule.data.repository.ScheduleRepository
import com.antigravity.whatsappschedule.service.SendTaskPayload
import com.antigravity.whatsappschedule.service.WhatsAppAutomationService
import com.antigravity.whatsappschedule.util.AlarmScheduler
import com.antigravity.whatsappschedule.util.NotificationHelper
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.util.Calendar

class SendWhatsAppWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MESSAGE_ID = "key_message_id"
        private const val TAG = "SendWhatsAppWorker"
    }

    override suspend fun doWork(): Result {
        val messageId = inputData.getLong(KEY_MESSAGE_ID, -1L)
        if (messageId == -1L) return Result.failure()

        val app = context.applicationContext as WhatsAppSchedulerApp
        val repository = ScheduleRepository(app.database.scheduledMessageDao())
        val message = repository.getMessageById(messageId) ?: return Result.failure()

        if (message.status != MessageStatus.PENDING) {
            Log.w(TAG, "Message $messageId is not pending (status: ${message.status})")
            return Result.success()
        }

        // 1. Verify WhatsApp Installation
        if (!isWhatsAppInstalled(context)) {
            val reason = FailureReason.WHATSAPP_NOT_INSTALLED.label
            repository.updateStatus(messageId, MessageStatus.FAILED, reason)
            NotificationHelper.showFailureNotification(context, messageId, message.contactName, reason)
            return Result.failure()
        }

        // 2. Verify Accessibility Service Enabled
        if (!WhatsAppAutomationService.isServiceRunning()) {
            val reason = FailureReason.ACCESSIBILITY_DISABLED.label
            repository.updateStatus(messageId, MessageStatus.FAILED, reason)
            NotificationHelper.showFailureNotification(context, messageId, message.contactName, reason)
            return Result.failure()
        }

        // 3. Notify Sending
        NotificationHelper.showSendingNotification(context, messageId, message.contactName)

        // 4. Prepare Automation Payload & Launch WhatsApp
        val cleanPhone = message.phoneNumber.replace(Regex("[^0-9+]"), "")
        val payload = SendTaskPayload(
            messageId = message.id,
            contactName = message.contactName,
            phoneNumber = cleanPhone,
            messageText = message.message,
            attachmentPath = message.attachmentPath
        )

        WhatsAppAutomationService.currentPayload = payload

        try {
            val encodedMessage = URLEncoder.encode(message.message, "UTF-8")
            val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)

            // Wait for automation service to complete UI injection
            delay(4000)

            // Update status as SENT
            repository.updateStatus(messageId, MessageStatus.SENT, null)
            NotificationHelper.showSuccessNotification(context, messageId, message.contactName)

            // Handle recurring schedule
            handleRecurringSchedule(context, repository, message)

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WhatsApp or send message", e)
            val reason = e.localizedMessage ?: FailureReason.UNKNOWN_ERROR.label
            repository.updateStatus(messageId, MessageStatus.FAILED, reason)
            NotificationHelper.showFailureNotification(context, messageId, message.contactName, reason)
            return Result.failure()
        }
    }

    private fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                pm.getPackageInfo("com.whatsapp.w4b", PackageManager.GET_ACTIVITIES)
                true
            } catch (e2: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private suspend fun handleRecurringSchedule(
        context: Context,
        repository: ScheduleRepository,
        message: ScheduledMessage
    ) {
        if (message.repeatType == RepeatType.ONCE) return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = message.scheduledDateTime
        }

        when (message.repeatType) {
            RepeatType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            else -> return
        }

        val nextScheduledTime = calendar.timeInMillis
        val nextMessage = message.copy(
            id = 0,
            scheduledDateTime = nextScheduledTime,
            status = MessageStatus.PENDING,
            failureReason = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val newId = repository.insertMessage(nextMessage)
        val insertedMessage = nextMessage.copy(id = newId)
        AlarmScheduler.scheduleAlarm(context, insertedMessage)
    }
}
