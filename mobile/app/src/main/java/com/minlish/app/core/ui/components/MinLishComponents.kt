package com.minlish.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minlish.app.core.ui.theme.MinLishColors

/** Standard screen container that applies the soft app background. */
@Composable
fun MinLishScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        content = content,
    )
}

/** A small rounded chip for tags, synonyms, collocations, etc. */
@Composable
fun MinLishChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MinLishColors.Primary,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = Spacing.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

/** A horizontally scrollable group of chips with a title. */
@Composable
fun ChipFlowSection(title: String, items: List<String>, color: Color = MinLishColors.Primary, icon: ImageVector? = null) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        MinLishSectionHeader(title, icon = icon)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(items.size) { i -> MinLishChip(items[i], color = color) }
        }
    }
}

@Composable
fun MinLishSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = Spacing.sm)) { Text(action) }
        }
    }
}

/** A rounded, animated progress bar. */
@Composable
fun MinLishProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MinLishColors.Primary,
    trackColor: Color = MinLishColors.SurfaceVariant,
    height: Int = 10,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.85f), color))),
        )
    }
}

/** A soft gradient hero card used at the top of dashboards. */
@Composable
fun GradientHeroCard(
    gradient: List<Color> = MinLishColors.HeroGradient,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(gradient))
            .padding(Spacing.lg),
    ) {
        Column(content = content)
    }
}

/** Circular icon badge with a soft tinted background. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MinLishColors.Primary,
    size: Int = 44,
    contentDescription: String? = null,
) {
    Box(
        modifier
            .size(size.dp)
            .background(color.copy(alpha = 0.16f), RoundedCornerShape((size / 3).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = color, modifier = Modifier.size((size * 0.5f).dp))
    }
}

/** A small speaker pill shown when audio is available for a word. */
@Composable
fun SpeakerButton(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play pronunciation", modifier = Modifier.size(20.dp))
    }
}

/** A rounded badge for part of speech (noun, verb, etc.). */
@Composable
fun PartOfSpeechBadge(partOfSpeech: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MinLishColors.Accent.copy(alpha = 0.16f))
            .padding(horizontal = Spacing.sm, vertical = 3.dp),
    ) {
        Text(partOfSpeech.lowercase(), style = MaterialTheme.typography.labelSmall, color = MinLishColors.Target, fontWeight = FontWeight.Bold)
    }
}

/**
 * Visual deck theme derived from a deck's name + tags so each deck gets a
 * consistent cute icon, accent color, and emoji.
 */
data class DeckTheme(val icon: ImageVector, val color: Color, val emoji: String)

fun deckThemeFor(name: String, tags: List<String> = emptyList()): DeckTheme {
    val haystack = (name + " " + tags.joinToString(" ")).lowercase()
    return when {
        "ielts" in haystack -> DeckTheme(Icons.Default.School, MinLishColors.Info, "🎓")
        "toeic" in haystack -> DeckTheme(Icons.Default.WorkOutline, MinLishColors.Secondary, "💼")
        "business" in haystack -> DeckTheme(Icons.Default.BusinessCenter, MinLishColors.Target, "📈")
        "communication" in haystack || "speaking" in haystack || "chat" in haystack ->
            DeckTheme(Icons.Default.ChatBubbleOutline, MinLishColors.Mint, "💬")
        "academic" in haystack || "study" in haystack -> DeckTheme(Icons.AutoMirrored.Filled.MenuBook, MinLishColors.Accent, "📚")
        else -> DeckTheme(Icons.Default.Style, MinLishColors.Primary, "🗂️")
    }
}

/** AI badge used by agent screens. */
@Composable
fun AiBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(MinLishColors.Accent.copy(alpha = 0.14f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MinLishColors.Accent, modifier = Modifier.size(14.dp))
        Text("AI helper", style = MaterialTheme.typography.labelSmall, color = MinLishColors.Accent, fontWeight = FontWeight.Bold)
    }
}



