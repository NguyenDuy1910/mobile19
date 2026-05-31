package com.minlish.app.onboarding.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.minlish.app.core.model.ProfileDto
import com.minlish.app.core.network.UiState
import com.minlish.app.core.ui.components.*

private val goals = listOf("IELTS", "TOEIC", "Communication", "Business", "Other")
private val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")

@Composable
fun ProfileSetupScreen(
    state: UiState<*>,
    initial: ProfileDto,
    onSave: (String, String, String, Int, Int, String?) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var goal by remember(initial) { mutableStateOf(initial.learningGoal ?: "IELTS") }
    var level by remember(initial) { mutableStateOf(initial.englishLevel ?: "B1") }
    var newWords by remember(initial) { mutableStateOf(initial.dailyNewWords.toString()) }
    var reviewLimit by remember(initial) { mutableStateOf(initial.dailyReviewLimit.toString()) }
    var notificationTime by remember(initial) { mutableStateOf(initial.notificationTime ?: "20:00:00") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("Set up your learning plan ✨", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("You can change these preferences any time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        MinLishTextField(name, { name = it }, "Your name", leadingIcon = Icons.Default.Person)

        ChipSelector("Learning goal", Icons.Default.Flag, goals, goal) { goal = it }
        ChipSelector("English level", Icons.Default.School, levels, level) { level = it }

        MinLishTextField(newWords, { newWords = it.filter(Char::isDigit) }, "Daily new words")
        MinLishTextField(reviewLimit, { reviewLimit = it.filter(Char::isDigit) }, "Daily review limit")
        MinLishTextField(notificationTime, { notificationTime = it }, "Reminder time (HH:mm:ss)")
        if (state is UiState.Error) Text(state.message, color = MaterialTheme.colorScheme.error)
        PrimaryButton("Save learning plan", onClick = {
            onSave(name, goal, level, newWords.toIntOrNull() ?: 10, reviewLimit.toIntOrNull() ?: 50, notificationTime)
        }, enabled = state !is UiState.Loading && name.isNotBlank())
        if (state is UiState.Loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun ChipSelector(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        MinLishSectionHeader(label, icon = icon)
        FlowRowSimple(options) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) },
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

/** Minimal wrapping row that lays chips out across lines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSimple(items: List<String>, chip: @Composable (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        items.forEach { chip(it) }
    }
}
