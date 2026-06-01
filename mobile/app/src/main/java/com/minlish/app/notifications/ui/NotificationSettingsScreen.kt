package com.minlish.app.notifications.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minlish.app.app.SettingsData
import com.minlish.app.core.model.ProfileDto
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(profile: ProfileDto, state: SettingsData, onSave: (Boolean, String, String) -> Unit, onRegisterDevice: () -> Unit, onTest: () -> Unit, onBack: () -> Unit) {
    var enabled by remember(profile) { mutableStateOf(profile.notificationsEnabled) }
    var time by remember(profile) { mutableStateOf(profile.notificationTime ?: "20:00:00") }
    val timezone = remember(profile) { profile.timezone.ifBlank { TimeZone.getDefault().id } }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val parts = time.split(":")
        val pickerState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 20,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Reminder time") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    time = "%02d:%02d:00".format(pickerState.hour, pickerState.minute)
                    showTimePicker = false
                }) { Text("Set time", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }

    Scaffold(topBar = { MinLishTopBar("Notifications", onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            // Cute bell hero
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(Icons.Default.NotificationsActive, color = MinLishColors.Secondary, size = 72)
                Spacer(Modifier.height(Spacing.sm))
                Text("Daily reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("We'll remind you when it's time to review.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            MinLishCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily reminders", fontWeight = FontWeight.SemiBold)
                        Text("Gentle nudges to keep your streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(enabled, { enabled = it })
                }
            }
            MinLishCard(onClick = { showTimePicker = true }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    IconBadge(Icons.Default.Schedule, color = MinLishColors.Info)
                    Column(Modifier.weight(1f)) {
                        Text("Reminder time", fontWeight = FontWeight.SemiBold)
                        Text(time.take(5), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Tap to change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Timezone: $timezone", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, color = MinLishColors.Success, fontWeight = FontWeight.SemiBold) }
            PrimaryButton("Save reminder", { onSave(enabled, time, timezone) }, enabled = !state.loading)
            SecondaryButton("Register this Android device", onRegisterDevice, icon = Icons.Default.Smartphone)
            SecondaryButton("Send a test notification", onTest, icon = Icons.Default.NotificationsActive)
            Text(
                "Push delivery is simulated in this MVP. Test actions store a backend notification record.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
