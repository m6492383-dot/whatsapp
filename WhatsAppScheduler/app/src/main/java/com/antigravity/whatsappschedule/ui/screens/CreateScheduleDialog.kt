package com.antigravity.whatsappschedule.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antigravity.whatsappschedule.data.model.RepeatType
import com.antigravity.whatsappschedule.data.model.ScheduledMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScheduleDialog(
    initialMessage: ScheduledMessage?,
    onDismiss: () -> Unit,
    onSave: (
        contactName: String,
        phoneNumber: String,
        messageText: String,
        attachmentPath: String?,
        scheduledDateTime: Long,
        repeatType: RepeatType
    ) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember {
        Calendar.getInstance().apply {
            if (initialMessage != null) {
                timeInMillis = initialMessage.scheduledDateTime
            } else {
                add(Calendar.MINUTE, 5) // Default to 5 mins in future
            }
        }
    }

    var contactName by remember { mutableStateOf(initialMessage?.contactName ?: "") }
    var phoneNumber by remember { mutableStateOf(initialMessage?.phoneNumber ?: "") }
    var messageText by remember { mutableStateOf(initialMessage?.message ?: "") }
    var attachmentPath by remember { mutableStateOf(initialMessage?.attachmentPath) }
    var repeatType by remember { mutableStateOf(initialMessage?.repeatType ?: RepeatType.ONCE) }
    var selectedDateTimeMillis by remember { mutableLongStateOf(calendar.timeInMillis) }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    var dateText by remember { mutableStateOf(dateFormatter.format(Date(selectedDateTimeMillis))) }
    var timeText by remember { mutableStateOf(timeFormatter.format(Date(selectedDateTimeMillis))) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { attachmentPath = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialMessage == null) "Schedule New Message" else "Edit Scheduled Message",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Contact Name Input
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Phone Number Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (with Country Code)") },
                    placeholder = { Text("+1234567890") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date & Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val dialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    selectedDateTimeMillis = calendar.timeInMillis
                                    dateText = dateFormatter.format(Date(selectedDateTimeMillis))
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            dialog.show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateText, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = {
                            val dialog = TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    calendar.set(Calendar.MINUTE, minute)
                                    selectedDateTimeMillis = calendar.timeInMillis
                                    timeText = timeFormatter.format(Date(selectedDateTimeMillis))
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                            )
                            dialog.show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(timeText, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Message Body Input
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message") },
                    leadingIcon = { Icon(Icons.Default.Message, contentDescription = null) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // Attachment Picker Button & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") }
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (attachmentPath.isNullOrEmpty()) "Attach File" else "Change Attachment")
                    }

                    if (!attachmentPath.isNullOrEmpty()) {
                        IconButton(onClick = { attachmentPath = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove Attachment")
                        }
                    }
                }

                if (!attachmentPath.isNullOrEmpty()) {
                    Text(
                        text = "Attached: ${attachmentPath?.substringAfterLast('/')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Repeat Dropdown Selection
                Text("Repeat", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RepeatType.values().forEach { type ->
                        FilterChip(
                            selected = repeatType == type,
                            onClick = { repeatType = type },
                            label = { Text(type.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = contactName.isNotBlank() && phoneNumber.isNotBlank() && messageText.isNotBlank(),
                onClick = {
                    onSave(
                        contactName,
                        phoneNumber,
                        messageText,
                        attachmentPath,
                        selectedDateTimeMillis,
                        repeatType
                    )
                }
            ) {
                Text(if (initialMessage == null) "Schedule" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
