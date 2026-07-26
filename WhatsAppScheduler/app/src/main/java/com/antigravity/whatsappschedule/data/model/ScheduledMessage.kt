package com.antigravity.whatsappschedule.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageStatus {
    PENDING,
    SENT,
    FAILED,
    CANCELLED
}

enum class RepeatType {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class FailureReason(val label: String) {
    WHATSAPP_NOT_INSTALLED("WhatsApp not installed"),
    CONTACT_NOT_FOUND("Contact not found"),
    ATTACHMENT_MISSING("Attachment missing"),
    ACCESSIBILITY_DISABLED("Accessibility service disabled"),
    PERMISSION_DENIED("Permission denied"),
    USER_CANCELLED("User cancelled"),
    SEND_BUTTON_NOT_DETECTED("Send button not detected"),
    TIMEOUT_WAITING_FOR_UI("Timeout waiting for WhatsApp UI"),
    UNKNOWN_ERROR("Unknown error")
}

@Entity(tableName = "messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "contact_name")
    val contactName: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "attachment_path")
    val attachmentPath: String? = null,

    @ColumnInfo(name = "scheduled_datetime")
    val scheduledDateTime: Long, // epoch ms

    @ColumnInfo(name = "repeat_type")
    val repeatType: RepeatType = RepeatType.ONCE,

    @ColumnInfo(name = "status")
    val status: MessageStatus = MessageStatus.PENDING,

    @ColumnInfo(name = "failure_reason")
    val failureReason: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
