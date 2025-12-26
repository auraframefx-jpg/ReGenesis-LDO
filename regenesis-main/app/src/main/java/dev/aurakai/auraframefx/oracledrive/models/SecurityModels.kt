package dev.aurakai.auraframefx.oracledrive.models

import kotlinx.serialization.Serializable

/**
 * Sync configuration for OracleDrive
 */
@Serializable
data class SyncConfig(
    val enabled: Boolean = true,
    val interval: Long = 3600000L // 1 hour default
) {
    fun isValid(): Boolean = enabled && interval > 0
    val reason: String get() = if (!enabled) "Sync disabled" else if (interval <= 0) "Invalid interval" else "Valid"
}