package com.minlish.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.minlish.app.core.ui.theme.MinLishColors
import com.minlish.app.core.ui.theme.MinLishTheme

@Preview(showBackground = true, name = "Empty state")
@Composable
private fun EmptyStatePreview() {
    MinLishTheme { EmptyState("No decks yet", "Create your first deck to begin.", "Create deck", {}) }
}

@Preview(showBackground = true, name = "Error state")
@Composable
private fun ErrorStatePreview() {
    MinLishTheme { ErrorState("Cannot reach MinLish. Check your connection.", {}) }
}

@Preview(showBackground = true, name = "Loading state")
@Composable
private fun LoadingStatePreview() {
    MinLishTheme { LoadingState("Loading your words...") }
}

@Preview(showBackground = true, name = "Design atoms")
@Composable
private fun AtomsPreview() {
    MinLishTheme {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MinLishSectionHeader("Section header")
            StatCard("Day streak", "7", icon = Icons.Default.LocalFireDepartment, accent = MinLishColors.Streak)
            MinLishChip("IELTS")
            PartOfSpeechBadge("adjective")
            MinLishProgressBar(0.65f)
            PrimaryButton("Primary button", {})
            SecondaryButton("Secondary button", {})
        }
    }
}

