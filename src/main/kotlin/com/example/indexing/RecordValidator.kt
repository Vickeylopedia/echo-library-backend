package com.example.indexing

import org.slf4j.LoggerFactory

object RecordValidator {
    private val logger = LoggerFactory.getLogger(RecordValidator::class.java)

    /**
     * Validation Result with a flag indicating success and optional reasons for rejection.
     */
    data class ValidationResult(val isValid: Boolean, val reasons: List<String>)

    /**
     * Validates a RawContentRecord to ensure structural integrity and required fields.
     */
    fun validate(record: RawContentRecord): ValidationResult {
        val reasons = mutableListOf<String>()

        if (record.title.isBlank()) {
            reasons.add("Title is missing or empty")
        }
        if (record.speakerName.isBlank()) {
            reasons.add("Speaker name is missing or empty")
        }
        if (record.providerName.isBlank()) {
            reasons.add("Provider name is missing or empty")
        }
        if (record.audioUrl.isBlank()) {
            reasons.add("Audio URL is missing or empty")
        }

        val isValid = reasons.isEmpty()
        if (!isValid) {
            logger.warn("Validation failed for record [ID: ${record.externalId}]: ${reasons.joinToString(", ")}")
        }

        return ValidationResult(isValid, reasons)
    }
}
