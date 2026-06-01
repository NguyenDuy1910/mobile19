package com.minlish.app.progress.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.minlish.app.app.ProgressData
import com.minlish.app.core.model.*
import com.minlish.app.core.network.UiState
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors
import com.minlish.app.core.ui.theme.MinLishTheme

@Composable
fun ProgressDashboardScreen(state: UiState<ProgressData>, onRetry: () -> Unit) {
    Scaffold(topBar = { MinLishTopBar("Progress", subtitle = "Your learning journey") }) { padding ->
        when (state) {
            UiState.Idle, UiState.Loading -> LoadingState("Calculating progress...")
            UiState.Empty -> EmptyState("No progress yet", "Review a few words and your progress will appear here.", emoji = "📊")
            is UiState.Error -> ErrorState(state.message, onRetry)
            is UiState.Success -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item { MotivationBanner(state.data.summary) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        StatCard("Learned words", state.data.summary.learnedWords.toString(), Modifier.weight(1f), Icons.Default.EmojiEvents, MinLishColors.Trophy)
                        StatCard("Day streak", state.data.summary.streakDays.toString(), Modifier.weight(1f), Icons.Default.LocalFireDepartment, MinLishColors.Streak)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        StatCard("Accuracy", "${state.data.summary.accuracyPercentage.toInt()}%", Modifier.weight(1f), Icons.Default.MyLocation, MinLishColors.Target)
                        StatCard("Due reviews", state.data.summary.dueReviews.toString(), Modifier.weight(1f), Icons.Default.Schedule, MinLishColors.Info)
                    }
                }
                item {
                    MinLishCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            IconBadge(Icons.AutoMirrored.Filled.TrendingUp, color = MinLishColors.Primary)
                            Column {
                                Text("Estimated level", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(state.data.summary.levelEstimation, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                item { ActivityChart(state.data.activity) }
                item {
                    MinLishCard {
                        MinLishSectionHeader("Retention", icon = Icons.Default.CheckCircle)
                        Spacer(Modifier.height(Spacing.sm))
                        MinLishProgressBar((state.data.retention.retentionPercentage / 100).toFloat(), color = MinLishColors.Success)
                        Spacer(Modifier.height(Spacing.xs))
                        Text("${state.data.retention.retentionPercentage.toInt()}% across ${state.data.retention.totalReviews} reviews", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.data.decks.isNotEmpty()) item {
                    MinLishCard {
                        MinLishSectionHeader("Deck progress")
                        state.data.decks.forEach { (deck, progress) ->
                            Spacer(Modifier.height(Spacing.md))
                            val theme = deckThemeFor(deck.name, deck.tags)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(theme.emoji)
                                Text(deck.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("${progress.learnedWords}/${progress.totalWords}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(Spacing.xs))
                            val pct = if (progress.totalWords > 0) progress.learnedWords.toFloat() / progress.totalWords else 0f
                            MinLishProgressBar(pct, color = theme.color, height = 8)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MotivationBanner(summary: ProgressSummaryDto) {
    val message = when {
        summary.streakDays >= 3 -> "Great job, keep your ${summary.streakDays}-day streak! 🔥"
        summary.accuracyPercentage >= 80 -> "You're improving every day. ✨"
        summary.learnedWords > 0 -> "Nice progress — every word counts! 🌱"
        else -> "Start learning today and watch your progress grow! 🚀"
    }
    GradientHeroCard {
        Text("Keep going, you're doing great!", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.xs))
        Text(message, color = Color.White.copy(alpha = 0.95f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActivityChart(activity: List<DailyActivityDto>) {
    MinLishCard {
        MinLishSectionHeader("Daily activity", icon = Icons.AutoMirrored.Filled.TrendingUp)
        Spacer(Modifier.height(Spacing.md))
        if (activity.isEmpty()) {
            Text("Complete a review to start your activity chart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val values = activity.take(7).reversed()
            val max = values.maxOf { it.reviews }.coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Bottom) {
                values.forEach { day ->
                    val fraction by animateFloatAsState(day.reviews.toFloat() / max, label = "bar")
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(day.reviews.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((84 * fraction).coerceAtLeast(8f).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MinLishColors.Primary.copy(alpha = 0.35f + 0.65f * fraction)),
                        )
                        Text(day.date.takeLast(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressPreview() {
    MinLishTheme {
        ProgressDashboardScreen(
            UiState.Success(
                ProgressData(
                    ProgressSummaryDto(120, 7, 82.5, 8, "Intermediate"),
                    RetentionDto(80, 66, 14, 82.5),
                    listOf(DailyActivityDto("2026-05-31", 8, 7, 1), DailyActivityDto("2026-05-30", 5, 4, 1)),
                    emptyList(),
                ),
            ), {},
        )
    }
}
