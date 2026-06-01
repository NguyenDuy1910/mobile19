package com.minlish.app.practice.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minlish.app.app.PracticeData
import com.minlish.app.core.model.PracticeAnswerDto
import com.minlish.app.core.network.UiState
import com.minlish.app.core.ui.components.*
import com.minlish.app.core.ui.theme.MinLishColors

@Composable
fun PracticeScreen(state: UiState<PracticeData>, onGenerate: () -> Unit, onSubmit: (String) -> Unit, onNext: () -> Unit, onHistory: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { MinLishTopBar("Practice quiz", onBack) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                UiState.Idle -> PracticeSetup(onGenerate, onHistory)
                UiState.Loading -> LoadingState("Generating your quiz...")
                UiState.Empty -> EmptyState("Add words before practicing", "Your quiz is generated from your saved vocabulary.", "Back to decks", onBack, emoji = "✏️")
                is UiState.Error -> ErrorState(state.message, onGenerate)
                is UiState.Success -> when {
                    state.data.completed -> PracticeResult(state.data, onGenerate, onHistory)
                    state.data.feedback != null -> AnswerFeedback(state.data, onNext)
                    else -> QuizQuestion(state.data, onSubmit)
                }
            }
        }
    }
}

@Composable
private fun PracticeSetup(onGenerate: () -> Unit, onHistory: () -> Unit) {
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        GradientHeroCard {
            Text("Ready for a quick quiz? 🧠", style = MaterialTheme.typography.headlineSmall, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(Spacing.xs))
            Text("MinLish builds a short multiple-choice quiz from your saved words.", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f), style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton("Generate 5 questions", onGenerate, icon = Icons.Default.Quiz)
        SecondaryButton("View practice history", onHistory, icon = Icons.Default.History)
    }
}

@Composable
private fun QuizQuestion(data: PracticeData, onSubmit: (String) -> Unit) {
    val question = data.current ?: return
    var selected by remember(data.index) { mutableStateOf<String?>(null) }
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        MinLishProgressBar((data.index + 1f) / data.questions.size, height = 12)
        Text("Question ${data.index + 1} of ${data.questions.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        MinLishCard {
            Text(question.question, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        question.options.forEach { option ->
            OptionCard(option, selected == option) {
                selected = option
                onSubmit(option)
            }
        }
    }
}

@Composable
private fun OptionCard(option: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MinLishColors.PrimaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Text(
            option,
            Modifier.fillMaxWidth().padding(Spacing.md),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun AnswerFeedback(data: PracticeData, onNext: () -> Unit) {
    val answer = data.feedback ?: return
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(96.dp)
                .background(
                    if (answer.isCorrect) MinLishColors.SuccessContainer else MinLishColors.ErrorContainer,
                    RoundedCornerShape(48.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (answer.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (answer.isCorrect) MinLishColors.Success else MinLishColors.Error,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            if (answer.isCorrect) "Correct! 🎉" else "Not quite",
            style = MaterialTheme.typography.headlineSmall,
            color = if (answer.isCorrect) MinLishColors.Success else MinLishColors.Error,
            fontWeight = FontWeight.ExtraBold,
        )
        if (!answer.isCorrect) {
            MinLishCard {
                Text("Correct answer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(answer.correctAnswer, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        answer.aiFeedback?.takeIf { it.isNotBlank() }?.let {
            MinLishCard {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MinLishColors.Warning)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        PrimaryButton("Next question", onNext)
    }
}

@Composable
private fun PracticeResult(data: PracticeData, onGenerate: () -> Unit, onHistory: () -> Unit) {
    val correct = data.answers.count { it.isCorrect }
    val total = data.answers.size
    val pct = if (total > 0) correct * 100 / total else 0
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingEmoji(if (pct >= 80) "🏆" else if (pct >= 50) "👍" else "🌱")
        Text("Quiz complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        MinLishCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ScoreStat("Score", "$pct%", MinLishColors.Primary)
                ScoreStat("Correct", correct.toString(), MinLishColors.Success)
                ScoreStat("Wrong", (total - correct).toString(), MinLishColors.Error)
            }
        }
        PrimaryButton("Practice again", onGenerate, icon = Icons.Default.Quiz)
        SecondaryButton("View history", onHistory, icon = Icons.Default.History)
    }
}

@Composable
private fun ScoreStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PracticeHistoryScreen(state: UiState<List<PracticeAnswerDto>>, onRetry: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { MinLishTopBar("Practice history", onBack) }) { padding ->
        when (state) {
            UiState.Idle, UiState.Loading -> LoadingState("Loading practice history...")
            UiState.Empty -> EmptyState("No practice history yet", "Finish a quiz and your answers will appear here.", emoji = "📝")
            is UiState.Error -> ErrorState(state.message, onRetry)
            is UiState.Success -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(state.data, key = { it.id }) { answer ->
                    MinLishCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Icon(
                                if (answer.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (answer.isCorrect) MinLishColors.Success else MinLishColors.Error,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(answer.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("You: ${answer.userAnswer}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
