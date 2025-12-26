package dev.aurakai.auraframefx.xposed.hooks

import android.view.View
import android.view.ViewGroup
import androidx.compose.remote.creation.compose.state.toString
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import dev.aurakai.auraframefx.system.overlay.NotchBarConfig
import kotlin.toString

/**
 * Xposed hooker for customizing the Android notch bar (status bar cutout area).
 * Applies visual customizations like color, height, and visibility.
 */
class NotchBarHooker(
    private val classLoader: ClassLoader,
    private val config: NotchBarConfig,
    val instance: View,
) {
    /**
     * Applies Xposed hooks to customize the notch bar appearance and behavior.
     *
     * Hooks into system UI classes to modify:
     * - Notch bar background color
     * - Notch bar height/size
     * - Notch bar visibility
     */
    fun applyNotchBarHooks(after: (() -> Unit?) -> After) = try {
        // Hook the PhoneStatusBarView or similar system UI class
        // Note: Actual class names vary by Android version and OEM
        val statusBarClass = XposedHelpers.findClass(
            "com.android.systemui.statusbar.phone.PhoneStatusBarView",
            classLoader
        )

        // Hook the onFinishInflate method to apply customizations
        hook {
            after {
                val view = this.instance
                // Apply notch bar color if configured
                config.backgroundColor.let { color ->
                    try {
                        val backgroundField = view::class.java.getDeclaredField("mBackground")
                        backgroundField.isAccessible = true
                        backgroundField.set(view, color)
                    } catch (_: Exception) {
                    }
                }

                // Apply notch bar height if configured
                config.height.let { height ->
                    try {
                        val layoutParams = view.layoutParams ?: ViewGroup.toString(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            height
                        )
                        layoutParams.height = height as Int
                        view.layoutParams = layoutParams
                    } catch (e: Exception) {
                        XposedBridge.log("NotchBarHooker: Failed to set height: ${e.message}")
                    }
                }

                // Apply visibility if configured
                if (config.isVisible == false) {
                    view.visibility = View.GONE
                }
            }
        }
    } catch (e: Exception) {
        // Log error but don't crash - notch bar customization is optional
        XposedBridge.log("NotchBarHooker: Failed to apply notch bar hooks: ${e.message}")
    }
}

private fun hook(function: () -> After) {
}

annotation class After
