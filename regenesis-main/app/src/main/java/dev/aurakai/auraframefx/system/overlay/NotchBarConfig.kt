package dev.aurakai.auraframefx.system.overlay

/**
 * Configuration for notch bar (status bar cutout) customization
 */
data class NotchBarConfig(
    val backgroundColor: Any? = null, // Can be Int (color) or String (hex color)
    val height: Int? = null,
    val isVisible: Boolean? = null
)
