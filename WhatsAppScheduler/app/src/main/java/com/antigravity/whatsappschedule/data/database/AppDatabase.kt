package com.antigravity.whatsappschedule.data.database

import android.content.Context
import androidx.room.*
import com.antigravity.whatsappschedule.data.dao.ScheduledMessageDao
import com.antigravity.whatsappschedule.data.model.MessageStatus
import com.antigravity.whatsappschedule.data.model.RepeatType
import com.antigravity.whatsappschedule.data.model.ScheduledMessage

class Converters {
    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = try {
        MessageStatus.valueOf(value)
    } catch (e: Exception) {
        MessageStatus.PENDING
    }

    @TypeConverter
    fun fromRepeatType(value: RepeatType): String = value.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType = try {
        RepeatType.valueOf(value)
    } catch (e: Exception) {
        RepeatType.ONCE
    }
}

@Database(entities = [ScheduledMessage::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduledMessageDao(): ScheduledMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whatsapp_scheduler_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
