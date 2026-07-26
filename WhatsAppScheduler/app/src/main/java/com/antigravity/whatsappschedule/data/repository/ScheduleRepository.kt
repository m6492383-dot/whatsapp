package com.antigravity.whatsappschedule.data.repository

import com.antigravity.whatsappschedule.data.dao.ScheduledMessageDao
import com.antigravity.whatsappschedule.data.model.MessageStatus
import com.antigravity.whatsappschedule.data.model.ScheduledMessage
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val messageDao: ScheduledMessageDao) {

    val allMessages: Flow<List<ScheduledMessage>> = messageDao.getAllMessages()

    suspend fun getMessageById(id: Long): ScheduledMessage? {
        return messageDao.getMessageById(id)
    }

    suspend fun getPendingMessages(): List<ScheduledMessage> {
        return messageDao.getPendingMessages()
    }

    suspend fun insertMessage(message: ScheduledMessage): Long {
        return messageDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ScheduledMessage) {
        messageDao.updateMessage(message)
    }

    suspend fun updateStatus(id: Long, status: MessageStatus, failureReason: String? = null) {
        messageDao.updateStatus(id, status, failureReason)
    }

    suspend fun deleteMessage(message: ScheduledMessage) {
        messageDao.deleteMessage(message)
    }

    suspend fun deleteMessageById(id: Long) {
        messageDao.deleteMessageById(id)
    }
}
