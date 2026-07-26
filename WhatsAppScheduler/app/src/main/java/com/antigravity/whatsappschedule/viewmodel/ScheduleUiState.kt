package com.antigravity.whatsappschedule.viewmodel

import com.antigravity.whatsappschedule.data.model.MessageStatus
import com.antigravity.whatsappschedule.data.model.ScheduledMessage

data class ScheduleUiState(
    val messages: List<ScheduledMessage> = emptyList(),
    val filteredMessages: List<ScheduledMessage> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: MessageStatus? = null,
    val isAccessibilityEnabled: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val isCreateDialogOpen: Boolean = false,
    val messageToEdit: ScheduledMessage? = null
)
