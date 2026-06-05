package com.minlish.app.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.minlish.app.app.HomeData
import com.minlish.app.core.model.*
import com.minlish.app.core.network.UiState
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors
import com.minlish.app.core.ui.theme.MinLishTheme

@Composable
fun HomeScreen(
    state:        UiState<HomeData>,
    onRetry:      () -> Unit,
    onCreateDeck: () -> Unit,
    onAddWord:    () -> Unit,
    onReview:     () -> Unit,
    onWordHelp:   () -> Unit,
    onImportCsv:  () -> Unit,               // ← tham số mới
) {
    when (state) {
        UiState.Idle, UiState.Loading ->
            LoadingState("Preparing your learning plan...")
        UiState.Empty ->
            EmptyState("Your learning space is ready", "Create your first deck to start learning.", "Create first deck", onCreateDeck, emoji = "🗂️")
        is UiState.Error ->
            ErrorState(state.message, onRetry)
        is UiState.Success ->
            HomeContent(state.data, onCreateDeck, onAddWord, onReview, onWordHelp, onImportCsv)
    }
}

@Composable
private fun HomeContent(
    data:         HomeData,
    onCreateDeck: () -> Unit,
    onAddWord:    () -> Unit,
    onReview:     () -> Unit,
    onWordHelp:   () -> Unit,
    onImportCsv:  () -> Unit,
) {
    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { HeroCard(data, onReview) }

        // ── Stats ─────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatCard("Learned",    data.progress.learnedWords.toString(),               Modifier.weight(1f), Icons.Default.EmojiEvents,          MinLishColors.Trophy)
                StatCard("Day streak", data.progress.streakDays.toString(),                 Modifier.weight(1f), Icons.Default.LocalFireDepartment,   MinLishColors.Streak)
                StatCard("Accuracy",   "${data.progress.accuracyPercentage.toInt()}%",      Modifier.weight(1f), Icons.Default.AutoAwesome,           MinLishColors.Target)
            }
        }

        // ── Quick actions ─────────────────────────────────────────
        item { MinLishSectionHeader("Quick actions", icon = Icons.Default.PlayArrow) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                QuickAction("Review",      Icons.Default.PlayArrow,      MinLishColors.Primary,   Modifier.weight(1f), onReview)
                QuickAction("Add word",    Icons.Default.Add,            MinLishColors.Info,      Modifier.weight(1f), onAddWord,   enabled = data.decks.isNotEmpty())
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                QuickAction("Create deck", Icons.Default.CreateNewFolder, MinLishColors.Secondary, Modifier.weight(1f), onCreateDeck)
                QuickAction("Ask AI",      Icons.Default.AutoAwesome,    MinLishColors.Accent,    Modifier.weight(1f), onWordHelp)
            }
        }
        // ── Hàng mới: Import CSV ──────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                QuickAction("Import CSV",  Icons.Default.Upload,         MinLishColors.Secondary, Modifier.weight(1f), onImportCsv)
                // Slot trống để giữ layout cân đối
                Spacer(Modifier.weight(1f))
            }
        }

        // ── Your decks ────────────────────────────────────────────
        item { MinLishSectionHeader("Your decks", icon = Icons.AutoMirrored.Filled.MenuBook) }

        if (data.decks.isEmpty()) {
            item {
                MinLishCard {
                    EmptyState(
                        "No decks yet",
                        "Create your first deck and add a few words to begin.",
                        "Create your first deck",
                        onCreateDeck,
                        emoji = "🌱",
                    )
                }
            }
        } else {
            items(data.decks.take(4)) { deck ->
                val theme = deckThemeFor(deck.name, deck.tags)
                MinLishCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        IconBadge(theme.icon, color = theme.color)
                        Column(Modifier.weight(1f)) {
                            Text(deck.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            deck.description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        Text(theme.emoji, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

// ── Hero Card ─────────────────────────────────────────────────────

@Composable
private fun HeroCard(data: HomeData, onReview: () -> Unit) {
    GradientHeroCard {
        Text("Hi ${data.me.profile.name} 👋", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
        Text("A little practice today goes a long way.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(Spacing.md))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(Spacing.md),
        ) {
            Column {
                Text("Today's plan", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text("${data.plan.totalDue} words ready", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text("${data.plan.newWordsDue} new · ${data.plan.reviewsDue} reviews", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(Spacing.md))
                Button(
                    onClick  = onReview,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MinLishColors.PrimaryDark),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(if (data.plan.totalDue > 0) "Review now" else "Open learning queue", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Quick Action Card ─────────────────────────────────────────────

@Composable
private fun QuickAction(
    label:   String,
    icon:    ImageVector,
    color:   Color,
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Card(
        onClick   = onClick,
        enabled   = enabled,
        modifier  = modifier.height(92.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Column(
            Modifier.fillMaxSize().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconBadge(icon, color = if (enabled) color else MaterialTheme.colorScheme.outline, size = 36)
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    MinLishTheme {
        HomeScreen(
            UiState.Success(
                HomeData(
                    MeDto("1", "min@example.com", ProfileDto("p", "Duy", "IELTS", "B1")),
                    DailyPlanDto(10, 50, 3, 8, 11),
                    ProgressSummaryDto(120, 7, 82.5, 8, "Intermediate"),
                    listOf(DeckDto("d", "IELTS Basics", "Core exam vocabulary", listOf("IELTS"))),
                ),
            ),
            {}, {}, {}, {}, {}, {},
        )
    }
}
