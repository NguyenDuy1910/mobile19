package com.minlish.app.learning.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.minlish.app.app.LearningData
import com.minlish.app.core.model.DueWordDto
import com.minlish.app.core.model.VocabItemDto
import com.minlish.app.core.network.UiState
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors
import com.minlish.app.core.ui.theme.MinLishTheme

private val motivations = listOf(
    "You've got this 💪", "Keep the streak alive 🔥", "Small steps, big wins ✨",
    "Brain gains incoming 🧠", "One more, you're doing great 🌟",
)

@Composable
fun FlashcardScreen(state: UiState<LearningData>, onRetry: () -> Unit, onBack: () -> Unit, onRate: (String) -> Unit) {
    Scaffold(topBar = { MinLishTopBar("Review", onBack) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                UiState.Idle, UiState.Loading -> LoadingState("Loading your review queue...")
                UiState.Empty -> EmptyState("All caught up!", "No reviews due right now. Come back when your next cards are ready.", "Go back", onBack, emoji = "🎉")
                is UiState.Error -> ErrorState(state.message, onRetry)
                is UiState.Success -> if (state.data.completed) {
                    CompletionState(state.data, onBack)
                } else {
                    ReviewCard(state.data, onRate)
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(data: LearningData, onRate: (String) -> Unit) {
    val current = data.current ?: return
    var flipped by remember(current.vocabItem.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, spring(stiffness = Spring.StiffnessLow), label = "flashcardFlip")
    val motivation = remember(current.vocabItem.id) { motivations[current.vocabItem.id.hashCode().mod(motivations.size)] }

    Column(
        Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MinLishProgressBar((data.index + 1f) / data.words.size, height = 12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${data.index + 1} / ${data.words.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Text(motivation, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { flipped = !flipped },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.raised),
        ) {
            Box(Modifier.fillMaxSize().padding(Spacing.lg), contentAlignment = Alignment.Center) {
                if (rotation <= 90f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        current.vocabItem.partOfSpeech?.takeIf { it.isNotBlank() }?.let { PartOfSpeechBadge(it) }
                        Text(current.vocabItem.word, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        (current.vocabItem.pronunciation ?: current.vocabItem.phonetic)?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(Spacing.md))
                        Text("👆 Tap to reveal meaning", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(
                        Modifier.graphicsLayer { rotationY = 180f }.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(current.vocabItem.word, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(current.vocabItem.meaning, style = MaterialTheme.typography.bodyLarge)
                        current.vocabItem.example?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(Spacing.xs))
                            Text("\"$it\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                        current.vocabItem.note?.takeIf { it.isNotBlank() }?.let {
                            Text("📝 $it", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(flipped) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("How well did you remember it?", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    RatingButton("Again", "again", MinLishColors.RatingAgain, Icons.Default.Refresh, Modifier.weight(1f), onRate)
                    RatingButton("Hard", "hard", MinLishColors.RatingHard, Icons.Default.Whatshot, Modifier.weight(1f), onRate)
                    RatingButton("Good", "good", MinLishColors.RatingGood, Icons.Default.ThumbUp, Modifier.weight(1f), onRate)
                    RatingButton("Easy", "easy", MinLishColors.RatingEasy, Icons.Default.Bolt, Modifier.weight(1f), onRate)
                }
            }
        }
        if (!flipped) {
            Text("Flip the card before rating your recall.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RatingButton(label: String, rating: String, color: Color, icon: ImageVector, modifier: Modifier, onRate: (String) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "ratingBounce")
    Button(
        onClick = { pressed = true; onRate(rating) },
        modifier = modifier.heightIn(min = 64.dp).scale(scale),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(vertical = Spacing.sm, horizontal = 2.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompletionState(data: LearningData, onBack: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "celebrate")
    val scale by transition.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "celebrateScale")
    Column(Modifier.fillMaxSize().padding(Spacing.xl), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(120.dp).scale(scale).background(MinLishColors.PrimaryContainer, RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.SentimentVerySatisfied, contentDescription = null, tint = MinLishColors.Primary, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(Spacing.lg))
        Text("Review complete! 🎉", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(Spacing.xs))
        Text("${data.correct} remembered · ${data.wrong} to revisit", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Great job — keep your streak going!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton("Back to MinLish", onBack)
    }
}

@Preview(showBackground = true)
@Composable
private fun FlashcardPreview() {
    MinLishTheme {
        FlashcardScreen(
            UiState.Success(LearningData(listOf(DueWordDto(VocabItemDto("1", "d", "resilient", meaning = "Able to recover quickly from difficulties.", partOfSpeech = "adjective", phonetic = "/rɪˈzɪl.i.ənt/"), "new", 0, 0, 2.5, "")))),
            {}, {}, {},
        )
    }
}
