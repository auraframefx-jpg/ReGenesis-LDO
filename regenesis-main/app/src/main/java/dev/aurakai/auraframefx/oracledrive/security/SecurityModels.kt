package dev.aurakai.auraframefx.oracledrive.security

import kotlinx.serialization.Serializable

/**
 * Security check result
 */
@Serializable
data class SecurityCheck(
    val isSecure: Boolean,
    val threatLevel: ThreatLevel = ThreatLevel.NONE,
    val issues: List<SecurityIssue> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val checkType: String = "GENERAL"
)

/**
 * Access check result for file operations
 */
@Serializable
data class AccessCheck(
    val isAllowed: Boolean,
    val requiredPermissions: List<String> = emptyList(),
    val missingPermissions: List<String> = emptyList(),
    val accessLevel: AccessLevel = AccessLevel.NONE,
    val reason: String = ""
)

/**
 * Validation result for file deletion
 */
@Serializable
data class DeletionValidation(
    val isValid: Boolean,
    val reason: String = "",
    val warnings: List<String> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val affectedFiles: List<String> = emptyList()
)

/**
 * Threat level enumeration
 */
@Serializable
enum class ThreatLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Access level enumeration
 */
@Serializable
enum class AccessLevel {
    NONE,
    READ_ONLY,
    READ_WRITE,
    ADMIN,
    SYSTEM
}

/**
 * Security issue details
 */
@Serializable
data class SecurityIssue(
    val id: String,
    val type: String,
    val severity: ThreatLevel,
    val description: String,
    val recommendation: String? = null
)
