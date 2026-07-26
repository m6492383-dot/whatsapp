package com.antigravity.whatsappschedule.data.dao

import androidx.room.*
import com.antigravity.whatsappschedule.data.model.MessageStatus
import com.antigravity.whatsappschedule.data.model.ScheduledMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {

    @Query("SELECT * FROM messages ORDER BY scheduled_datetime ASC")
    fun getAllMessages(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): ScheduledMessage?

    @Query("SELECT * FROM messages WHERE status = 'PENDING' AND scheduled_datetime <= :currentTimeMillis")
    suspend fun getDueMessages(currentTimeMillis: Long): List<ScheduledMessage>

    @Query("SELECT * FROM messages WHERE status = 'PENDING' ORDER BY scheduled_datetime ASC")
    suspend fun getPendingMessages(): List<ScheduledMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ScheduledMessage): Long

    @Update
    suspend fun updateMessage(message: ScheduledMessage)

    @Query("UPDATE messages SET status = :status, failure_reason = :failureReason, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: MessageStatus, failureReason: String?, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteMessage(message: ScheduledMessage)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
}
