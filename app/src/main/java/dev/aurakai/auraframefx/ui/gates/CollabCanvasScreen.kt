package dev.aurakai.auraframefx.ui.gates

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Bridge screen that routes to the real CollabCanvas module implementation.
 *
 * This is an intentional routing layer that connects the main app navigation
 * to the aura/reactivedesign/collabcanvas module.
 *
 * @param navController Optional NavHostController for back navigation
 * @param onNavigateBack Callback invoked when user navigates back
 */
@Composable
fun CollabCanvasScreen(navController: NavHostController? = null, onNavigateBack: () -> Unit = {}) {
    // Route to the real CollabCanvas module implementation
    collabcanvas.ui.CanvasScreen()
}
