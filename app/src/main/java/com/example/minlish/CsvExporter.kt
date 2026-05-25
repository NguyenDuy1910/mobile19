package com.example.minlish

// ════════════════════════════════════════════════════════════════
//  CsvExporter.kt — Fixed
//
//  Vấn đề cũ: header chỉ có 3 cột (word, meaning, deckId) nhưng
//  CsvImportActivity parse theo 7 cột:
//    word, pronunciation, meaning, example, collocation, relatedWords, note
//
//  Fix: export đúng 7 cột, escape dấu phẩy và xuống dòng trong data.
// ════════════════════════════════════════════════════════════════

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object CsvExporter {

    /**
     * Export danh sách từ của một deck ra file CSV rồi mở share dialog.
     *
     * Format (khớp với CsvImportActivity.parseCsv):
     *   word, pronunciation, meaning, example, collocation, relatedWords, note
     *
     * Dòng đầu là header — CsvImportActivity bỏ qua dòng này (drop(1)).
     */
    fun exportDeck(
        context: Context,
        deckName: String,
        words: List<VocabularyEntity>
    ) {
        val sb = StringBuilder()

        // ── Header (phải khớp thứ tự mà parseCsv đọc) ────────────
        sb.appendLine("word,pronunciation,meaning,example,collocation,relatedWords,note")

        // ── Rows ──────────────────────────────────────────────────
        for (word in words) {
            sb.appendLine(
                listOf(
                    word.word,
                    word.pronunciation,
                    word.meaning,
                    word.example,
                    word.collocation,
                    word.relatedWords,
                    word.note
                ).joinToString(",") { field ->
                    // Escape: nếu field có dấu phẩy hoặc xuống dòng → bọc trong ""
                    // Nếu field có dấu " → double nó trước
                    val escaped = field.replace("\"", "\"\"")
                    if (escaped.contains(',') || escaped.contains('\n') || escaped.contains('"'))
                        "\"$escaped\""
                    else escaped
                }
            )
        }

        // ── Ghi file vào cache ────────────────────────────────────
        val safeName = deckName
            .replace(" ", "_")
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "")
            .take(50)
            .ifEmpty { "deck" }

        val fileName = "minlish_${safeName}.csv"
        val file     = File(context.cacheDir, fileName)
        file.writeText(sb.toString(), Charsets.UTF_8)

        // ── Share ─────────────────────────────────────────────────
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MinLish Export — $deckName")
            putExtra(Intent.EXTRA_TEXT, "Exported ${words.size} từ từ deck \"$deckName\" bằng MinLish.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Xuất deck \"$deckName\""))
    }
}