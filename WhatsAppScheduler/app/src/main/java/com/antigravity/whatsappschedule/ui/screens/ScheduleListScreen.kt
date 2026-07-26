package com.antigravity.whatsappschedule.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.whatsappschedule.data.model.MessageStatus
import com.antigravity.whatsappschedule.ui.components.AccessibilityStatusCard
import com.antigravity.whatsappschedule.ui.components.BatteryOptimizationCard
import com.antigravity.whatsappschedule.ui.components.ScheduleCard
import com.antigravity.whatsappschedule.viewmodel.ScheduleUiState
import com.antigravity.whatsappschedule.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Messages", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = "New Schedule") },
                text = { Text("+ New Schedule") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Accessibility Warning Banner if service disabled
            AccessibilityStatusCard(isEnabled = uiState.isAccessibilityEnabled)

            // Battery Optimization Banner
            BatteryOptimizationCard()

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search schedules by contact or text...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStatusFilter == null,
                    onClick = { viewModel.onStatusFilterSelected(null) },
                    label = { Text("All") }
                )
                MessageStatus.values().forEach { status ->
                    FilterChip(
                        selected = uiState.selectedStatusFilter == status,
                        onClick = { viewModel.onStatusFilterSelected(if (uiState.selectedStatusFilter == status) null else status) },
                        label = { Text(status.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Empty State Handling
            if (uiState.filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Scheduled Messages",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ New Schedule' below to schedule a WhatsApp message.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Scheduled Messages List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = uiState.filteredMessages,
                        key = { it.id }
                    ) { message ->
                        ScheduleCard(
                            message = message,
                            onEdit = { viewModel.openCreateDialog(message) },
                            onDelete = { viewModel.deleteSchedule(message) },
                            onDuplicate = { viewModel.duplicateSchedule(message) },
                            onCancel = { viewModel.cancelSchedule(message) }
                        )
                    }
                }
            }
        }

        // Schedule Creation / Editing Modal
        if (uiState.isCreateDialogOpen) {
            CreateScheduleDialog(
                initialMessage = uiState.messageToEdit,
                onDismiss = { viewModel.closeCreateDialog() },
                onSave = { contactName, phoneNumber, messageText, attachmentPath, scheduledDateTime, repeatType ->
                    viewModel.saveSchedule(
                        contactName,
                        phoneNumber,
                        messageText,
                        attachmentPath,
                        scheduledDateTime,
                        repeatType
                    )
                }
            )
        }
    }
}
