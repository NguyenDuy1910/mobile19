package com.minlish.app.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minlish.app.BuildConfig
import com.minlish.app.core.model.MeDto
import com.minlish.app.core.network.NetworkConfig
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors

@Composable
fun SettingsScreen(me: MeDto, onProfile: () -> Unit, onNotifications: () -> Unit, onPractice: () -> Unit, onAgent: () -> Unit, onLogout: () -> Unit) {
    Scaffold(topBar = { MinLishTopBar("Settings") }) { padding ->
        Column(Modifier.padding(padding).padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            // Profile header
            MinLishCard(onClick = onProfile) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Box(
                        Modifier.size(56.dp).background(MinLishColors.PrimaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(me.profile.name.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall, color = MinLishColors.PrimaryDark, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(me.profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(me.email, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(Spacing.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            me.profile.learningGoal?.let { MinLishChip(it, color = MinLishColors.Info) }
                            me.profile.englishLevel?.let { MinLishChip(it, color = MinLishColors.Accent) }
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            MinLishSectionHeader("Preferences")
            SettingsRow("Profile & learning plan", Icons.Default.Person, MinLishColors.Primary, onProfile)
            SettingsRow("Notification settings", Icons.Default.NotificationsActive, MinLishColors.Secondary, onNotifications)
            SettingsRow("Practice quiz", Icons.Default.Quiz, MinLishColors.Info, onPractice)
            SettingsRow("AI word helper", Icons.Default.AutoAwesome, MinLishColors.Accent, onAgent)

            if (BuildConfig.DEBUG) {
                MinLishSectionHeader("Developer")
                MinLishCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        IconBadge(Icons.Default.BugReport, color = MinLishColors.Warning)
                        Column {
                            Text("Backend URL", fontWeight = FontWeight.SemiBold)
                            Text(NetworkConfig.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }
            Text("MinLish ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun SettingsRow(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    MinLishCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(icon, color = color)
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
