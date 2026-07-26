package com.antigravity.whatsappschedule.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.whatsappschedule.WhatsAppSchedulerApp
import com.antigravity.whatsappschedule.data.model.MessageStatus
import com.antigravity.whatsappschedule.data.model.RepeatType
import com.antigravity.whatsappschedule.data.model.ScheduledMessage
import com.antigravity.whatsappschedule.data.repository.ScheduleRepository
import com.antigravity.whatsappschedule.service.WhatsAppAutomationService
import com.antigravity.whatsappschedule.util.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScheduleRepository
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        val db = (application as WhatsAppSchedulerApp).database
        repository = ScheduleRepository(db.scheduledMessageDao())

        viewModelScope.launch {
            repository.allMessages.collect { list ->
                _uiState.update { state ->
                    val filtered = filterMessages(list, state.searchQuery, state.selectedStatusFilter)
                    state.copy(messages = list, filteredMessages = filtered)
                }
            }
        }

        checkServiceStatus()
    }

    fun checkServiceStatus() {
        val isAccessibilityOn = WhatsAppAutomationService.isServiceRunning()
        _uiState.update { it.copy(isAccessibilityEnabled = isAccessibilityOn) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = filterMessages(state.messages, query, state.selectedStatusFilter)
            state.copy(searchQuery = query, filteredMessages = filtered)
        }
    }

    fun onStatusFilterSelected(status: MessageStatus?) {
        _uiState.update { state ->
            val filtered = filterMessages(state.messages, state.searchQuery, status)
            state.copy(selectedStatusFilter = status, filteredMessages = filtered)
        }
    }

    fun openCreateDialog(messageToEdit: ScheduledMessage? = null) {
        _uiState.update { it.copy(isCreateDialogOpen = true, messageToEdit = messageToEdit) }
    }

    fun closeCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false, messageToEdit = null) }
    }

    fun saveSchedule(
        contactName: String,
        phoneNumber: String,
        messageText: String,
        attachmentPath: String?,
        scheduledDateTime: Long,
        repeatType: RepeatType
    ) {
        viewModelScope.launch {
            val existing = _uiState.value.messageToEdit
            val scheduledMessage = ScheduledMessage(
                id = existing?.id ?: 0,
                contactName = contactName,
                phoneNumber = phoneNumber,
                message = messageText,
                attachmentPath = attachmentPath,
                scheduledDateTime = scheduledDateTime,
                repeatType = repeatType,
                status = MessageStatus.PENDING,
                failureReason = null,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val id = repository.insertMessage(scheduledMessage)
            val finalMessage = scheduledMessage.copy(id = if (existing == null) id else existing.id)

            // Arm AlarmManager
            AlarmScheduler.scheduleAlarm(getApplication<Application>(), finalMessage)
            closeCreateDialog()
        }
    }

    fun cancelSchedule(message: ScheduledMessage) {
        viewModelScope.launch {
            repository.updateStatus(message.id, MessageStatus.CANCELLED, "User cancelled")
            AlarmScheduler.cancelAlarm(getApplication<Application>(), message.id)
        }
    }

    fun deleteSchedule(message: ScheduledMessage) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication<Application>(), message.id)
            repository.deleteMessage(message)
        }
    }

    fun duplicateSchedule(message: ScheduledMessage) {
        viewModelScope.launch {
            val duplicated = message.copy(
                id = 0,
                status = MessageStatus.PENDING,
                failureReason = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.insertMessage(duplicated)
            val finalMessage = duplicated.copy(id = newId)
            AlarmScheduler.scheduleAlarm(getApplication<Application>(), finalMessage)
        }
    }

    private fun filterMessages(
        list: List<ScheduledMessage>,
        query: String,
        statusFilter: MessageStatus?
    ): List<ScheduledMessage> {
        return list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.contactName.contains(query, ignoreCase = true) ||
                    item.phoneNumber.contains(query, ignoreCase = true) ||
                    item.message.contains(query, ignoreCase = true)
            val matchesStatus = statusFilter == null || item.status == statusFilter
            matchesQuery && matchesStatus
        }
    }
}
