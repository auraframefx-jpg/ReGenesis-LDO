package dev.aurakai.auraframefx.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import timber.log.Timber

/**
 * 📱 System App Launcher Utility
 *
 * Launches native Android system applications via intents.
 * Provides graceful fallback handling when apps aren't available.
 *
 * **Supported Apps**:
 * - Camera (via MediaStore.ACTION_IMAGE_CAPTURE)
 * - File Manager (via ACTION_VIEW with DocumentsContract)
 * - Phone Dialer (via ACTION_DIAL)
 */
object SystemAppLauncher {

    /**
     * Launch the device's camera app
     *
     * @param context Android context
     * @return true if launched successfully, false otherwise
     */
    fun launchCamera(context: Context): Boolean {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("📸 Camera app launched")
                true
            } else {
                showAppNotAvailable(context, "Camera")
                false
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Camera app not found")
            showAppNotAvailable(context, "Camera")
            false
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch camera")
            showError(context, "Camera")
            false
        }
    }

    /**
     * Launch the device's file manager
     *
     * Tries multiple intent strategies for maximum compatibility:
     * 1. ACTION_VIEW with file type
     * 2. ACTION_GET_CONTENT as fallback
     *
     * @param context Android context
     * @return true if launched successfully, false otherwise
     */
    fun launchFileManager(context: Context): Boolean {
        return try {
            // Try modern file manager intent first
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("📁 File manager launched")
                true
            } else {
                // Fallback to file picker
                launchFilePicker(context)
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "File manager not found")
            showAppNotAvailable(context, "File Manager")
            false
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch file manager")
            showError(context, "File Manager")
            false
        }
    }

    /**
     * Launch file picker as fallback
     */
    private fun launchFilePicker(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(Intent.createChooser(intent, "Select File"))
                Timber.d("📂 File picker launched")
                true
            } else {
                showAppNotAvailable(context, "File Manager")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "File picker fallback failed")
            showAppNotAvailable(context, "File Manager")
            false
        }
    }

    /**
     * Launch the device's phone dialer
     *
     * @param context Android context
     * @param phoneNumber Optional phone number to pre-fill (can be null)
     * @return true if launched successfully, false otherwise
     */
    fun launchPhoneDialer(context: Context, phoneNumber: String? = null): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                if (!phoneNumber.isNullOrBlank()) {
                    data = Uri.parse("tel:$phoneNumber")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("📞 Phone dialer launched${phoneNumber?.let { " with number: $it" } ?: ""}")
                true
            } else {
                showAppNotAvailable(context, "Phone")
                false
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Phone dialer not found")
            showAppNotAvailable(context, "Phone")
            false
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch phone dialer")
            showError(context, "Phone")
            false
        }
    }

    /**
     * Launch the device's contacts app
     *
     * @param context Android context
     * @return true if launched successfully, false otherwise
     */
    fun launchContacts(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "vnd.android.cursor.dir/contact"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("👤 Contacts app launched")
                true
            } else {
                showAppNotAvailable(context, "Contacts")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch contacts")
            showError(context, "Contacts")
            false
        }
    }

    /**
     * Launch the device's calculator app
     *
     * @param context Android context
     * @return true if launched successfully, false otherwise
     */
    fun launchCalculator(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.android.calculator2")
                ?: context.packageManager.getLaunchIntentForPackage("com.google.android.calculator")

            if (intent != null) {
                context.startActivity(intent)
                Timber.d("🔢 Calculator launched")
                true
            } else {
                showAppNotAvailable(context, "Calculator")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch calculator")
            showError(context, "Calculator")
            false
        }
    }

    /**
     * Show "app not available" toast message
     */
    private fun showAppNotAvailable(context: Context, appName: String) {
        Toast.makeText(
            context,
            "$appName app not available on this device",
            Toast.LENGTH_SHORT
        ).show()
        Timber.w("⚠️ $appName app not available")
    }

    /**
     * Show generic error toast
     */
    private fun showError(context: Context, appName: String) {
        Toast.makeText(
            context,
            "Failed to open $appName",
            Toast.LENGTH_SHORT
        ).show()
    }
}
