package com.example.indexing

import java.util.Locale

object ContentNormalizer {

    // Map of known aliases to canonical speaker names
    private val speakerCanonicalMap = mapOf(
        "apostle joshua selman nimak" to "Apostle Joshua Selman",
        "apostle joshua selman" to "Apostle Joshua Selman",
        "joshua selman" to "Apostle Joshua Selman",
        "dr paul enenche" to "Dr. Paul Enenche",
        "paul enenche" to "Dr. Paul Enenche",
        "dr andrew huberman" to "Dr. Andrew Huberman",
        "andrew huberman" to "Dr. Andrew Huberman",
        "pastor e a adeboye" to "Pastor E.A. Adeboye",
        "pastor e.a. adeboye" to "Pastor E.A. Adeboye",
        "enoch adeboye" to "Pastor E.A. Adeboye",
        "ryan holiday" to "Ryan Holiday"
    )

    // Map of categories to standard category names
    private val categoryCanonicalMap = mapOf(
        "faith & prayer" to "Faith & Prayer",
        "faith" to "Faith & Prayer",
        "prayer" to "Faith & Prayer",
        "miracles & healing" to "Miracles & Healing",
        "healing" to "Miracles & Healing",
        "christian living" to "Christian Living",
        "self-improvement" to "Self-Improvement",
        "self improvement" to "Self-Improvement",
        "mental health & science" to "Mental Health & Science",
        "science" to "Mental Health & Science",
        "philosophy" to "Philosophy",
        "sermon" to "Sermon"
    )

    /**
     * Clean up general whitespace and redundant punctuation.
     */
    fun cleanText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ") // normalize whitespace
            .replace(Regex("[!?.]{2,}")) { match -> match.value.first().toString() } // collapse duplicate punctuation (e.g. !!, ??? to !, ?)
    }

    /**
     * Standardizes a speaker's name to its canonical version.
     */
    fun normalizeSpeaker(name: String): String {
        val cleaned = cleanText(name)
        val lookupKey = cleaned.lowercase(Locale.getDefault())
            .replace(Regex("[.\\-,]"), "") // strip punctuation for key matching
            .replace(Regex("\\s+"), " ")
            .trim()

        return speakerCanonicalMap[lookupKey] ?: capitalizeWords(cleaned)
    }

    /**
     * Standardizes categories to standard formats.
     */
    fun normalizeCategory(category: String?): String {
        if (category.isNullOrBlank()) return "Uncategorized"
        val cleaned = cleanText(category)
        val lookupKey = cleaned.lowercase(Locale.getDefault()).trim()
        return categoryCanonicalMap[lookupKey] ?: capitalizeWords(cleaned)
    }

    /**
     * Standardizes the duration string format (e.g., from "58:40" or "1:45" to canonical HH:MM:SS format if possible).
     */
    fun normalizeDuration(duration: String): String {
        val cleaned = cleanText(duration).replace(" ", "")
        val parts = cleaned.split(":")
        
        return when (parts.size) {
            1 -> {
                val seconds = parts[0].toIntOrNull() ?: 0
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                val s = seconds % 60
                String.format("%02d:%02d:%02d", h, m, s)
            }
            2 -> {
                val m = parts[0].toIntOrNull() ?: 0
                val s = parts[1].toIntOrNull() ?: 0
                if (m >= 60) {
                    val h = m / 60
                    val remM = m % 60
                    String.format("%02d:%02d:%02d", h, remM, s)
                } else {
                    String.format("00:%02d:%02d", m, s)
                }
            }
            3 -> {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                val s = parts[2].toIntOrNull() ?: 0
                String.format("%02d:%02d:%02d", h, m, s)
            }
            else -> "00:00:00"
        }
    }

    /**
     * Helper to capitalize the first letter of each word.
     */
    private fun capitalizeWords(text: String): String {
        return text.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
    }

    /**
     * Normalizes an entire raw record into a sanitized record.
     */
    fun normalize(record: RawContentRecord): RawContentRecord {
        return record.copy(
            title = cleanText(record.title),
            speakerName = normalizeSpeaker(record.speakerName),
            durationString = normalizeDuration(record.durationString),
            categoryName = normalizeCategory(record.categoryName),
            description = cleanText(record.description),
            podcastTitle = record.podcastTitle?.let { cleanText(it) }
        )
    }
}
