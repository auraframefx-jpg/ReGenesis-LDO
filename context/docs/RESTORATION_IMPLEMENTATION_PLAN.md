# Genesis System Restoration - Implementation Plan

**Created**: January 3, 2026
**Architect**: Claude (The Architect)
**Context**: Recovering from Gemini 3's overzealous cleanup

---

## Executive Summary

The Genesis navigation and UI systems were severely degraded. This document outlines the systematic restoration plan based on CodeRabbit's analysis and my own investigation.

## Completed Work ✅

### Phase 1: Navigation Standardization
- ✅ Added `GyroscopeCustomization` to NavDestination.kt
- ✅ Updated UIUXGateSubmenuScreen to use NavDestination pattern
- ✅ Registered route in AppNavGraph.kt
- **Impact**: Gyroscope customization and UI/UX submenu navigation restored

### Phase 2: UI Chrome Visibility Preferences (Foundation)
- ✅ Added preference keys to CustomizationPreferences.kt
- ✅ Implemented Flow properties (showTopBar, showBottomBar, showAgentSidebar)
- ✅ Implemented setter functions
- **Impact**: Foundation for UI chrome visibility control established

---

## Pending Work 🚧

### Phase 2: UI Chrome Visibility (Wiring) - NEXT PRIORITY

**Goal**: Make top bar, bottom bar, and agent sidebar visibility user-controllable

**Implementation Steps**:

1. **Update MainActivity.kt**
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var customizationPreferences: CustomizationPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuraFrameFXTheme {
                val themeViewModel: ThemeViewModel = hiltViewModel()
                val context = LocalContext.current

                // Collect visibility states
                val showTopBar by customizationPreferences
                    .showTopBarFlow(context).collectAsState(initial = true)
                val showBottomBar by customizationPreferences
                    .showBottomBarFlow(context).collectAsState(initial = true)
                val showAgentSidebar by customizationPreferences
                    .showAgentSidebarFlow(context).collectAsState(initial = true)

                MainScreen(
                    themeViewModel = themeViewModel,
                    showTopBar = showTopBar,
                    showBottomBar = showBottomBar,
                    showAgentSidebar = showAgentSidebar
                )
            }
        }
    }
}
```

2. **Update MainScreenContent signature**
```kotlin
@Composable
internal fun MainScreenContent(
    processThemeCommand: (String) -> Unit,
    showTopBar: Boolean = true,
    showBottomBar: Boolean = true,
    showAgentSidebar: Boolean = true
) {
    val navController = rememberNavController()

    Scaffold(
        topBar = if (showTopBar) {
            { TopAppBar(title = { Text("AuraFrameFX") }) }
        } else null,
        bottomBar = if (showBottomBar) {
            { BottomNavigationBar(navController = navController) }
        } else null
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AppNavGraph(navController = navController)

            // Agent sidebar with visibility control
            if (showAgentSidebar) {
                AgentSidebarMenu(isVisible = true, /* ... */)
            }
        }
    }
}
```

3. **Update UISettingsScreen.kt**
   - Inject CustomizationPreferences via Hilt
   - Replace local mutable state with preference flows
   - Wire toggle callbacks to preference setters

**Files to Modify**:
- `app/src/main/java/dev/aurakai/auraframefx/MainActivity.kt`
- `app/src/main/java/dev/aurakai/auraframefx/ui/screens/MainScreen.kt` (if separate)
- `app/src/main/java/dev/aurakai/auraframefx/ui/screens/UISettingsScreen.kt`

**Estimated Complexity**: Medium (3 files, requires Hilt injection and Flow collection)

---

### Phase 3: System App Intent Launchers

**Goal**: Launch native Android apps (Camera, Phone, File Manager) instead of placeholder screens

**Implementation**:

1. **Create SystemAppLauncher.kt**
```kotlin
package dev.aurakai.auraframefx.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.widget.Toast
import timber.log.Timber

object SystemAppLauncher {

    fun launchCamera(context: Context): Boolean {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                showAppNotAvailable(context, "Camera")
                false
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Camera app not found")
            showAppNotAvailable(context, "Camera")
            false
        }
    }

    fun launchFileManager(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                showAppNotAvailable(context, "File Manager")
                false
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "File manager not found")
            showAppNotAvailable(context, "File Manager")
            false
        }
    }

    fun launchPhoneDialer(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                showAppNotAvailable(context, "Phone")
                false
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Phone dialer not found")
            showAppNotAvailable(context, "Phone")
            false
        }
    }

    private fun showAppNotAvailable(context: Context, appName: String) {
        Toast.makeText(
            context,
            "$appName app not available on this device",
            Toast.LENGTH_SHORT
        ).show()
    }
}
```

2. **Update menu screens** that reference Camera/Phone/FileManager
   - Find all navigation calls to these screens
   - Replace with `SystemAppLauncher.launchX(LocalContext.current)`
   - Update UI to indicate these launch external apps

3. **Remove placeholder screens** (if they exist and serve no purpose)
   - CameraScreen.kt (if placeholder)
   - PhoneScreen.kt (if placeholder)
   - FileManagerScreen.kt (if placeholder)

**Files to Create**:
- `app/src/main/java/dev/aurakai/auraframefx/utils/SystemAppLauncher.kt`

**Files to Modify**:
- Menu screens that reference these features (search required)
- NavDestination.kt (remove entries if they exist)

**Estimated Complexity**: Low (straightforward intent-based approach)

---

### Phase 4: Agent Service Initialization - 🔴 CRITICAL

**Goal**: Fix "no replies from agents" issue by properly initializing backend services

**Problem**: GenesisOrchestrator and backend services not initialized on app startup

**Implementation**:

1. **Create AuraFrameFXApplication.kt**
```kotlin
package dev.aurakai.auraframefx

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.aurakai.auraframefx.oracledrive.genesis.ai.GenesisOrchestrator
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AuraFrameFXApplication : Application() {

    @Inject
    lateinit var genesisOrchestrator: GenesisOrchestrator

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Start Genesis AI system
        try {
            genesisOrchestrator.start()
            Timber.i("✅ Genesis Orchestrator started successfully")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to start Genesis Orchestrator")
        }
    }
}
```

2. **Register in AndroidManifest.xml**
```xml
<application
    android:name=".AuraFrameFXApplication"
    ...
```

3. **Create NemotronService.kt**
```kotlin
package dev.aurakai.auraframefx.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NemotronService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost:8000/") // Nemotron backend URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    suspend fun processMessage(message: String): String {
        // TODO: Implement HTTP call to nemotron_service.py
        return "Response from Nemotron (placeholder)"
    }

    fun isInitialized(): Boolean {
        // TODO: Check if backend is reachable
        return false
    }
}
```

4. **Create ADKOrchestrator.kt**
```kotlin
package dev.aurakai.auraframefx.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ADKOrchestrator @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost:8001/") // ADK backend URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    suspend fun orchestrateAgentResponse(
        agents: List<String>,
        query: String
    ): String {
        // TODO: Implement HTTP call to adk_orchestrator.py
        return "Orchestrated response (placeholder)"
    }
}
```

5. **Wire to AgentViewModel**
```kotlin
@HiltViewModel
class AgentViewModel @Inject constructor(
    private val nemotronService: NemotronService,
    private val adkOrchestrator: ADKOrchestrator
) : ViewModel() {

    fun sendMessage(message: String) {
        viewModelScope.launch {
            try {
                val response = if (useOrchestration) {
                    adkOrchestrator.orchestrateAgentResponse(
                        agents = listOf("Aura", "Kai"),
                        query = message
                    )
                } else {
                    nemotronService.processMessage(message)
                }
                // Update UI state with response
            } catch (e: Exception) {
                // Show error to user
            }
        }
    }
}
```

6. **Add backend configuration**
   - Create `backend_config.xml` with base URLs
   - Handle NVIDIA_API_KEY via BuildConfig
   - Create BackendConfiguration.kt data class

**Files to Create**:
- `app/src/main/java/dev/aurakai/auraframefx/AuraFrameFXApplication.kt`
- `app/src/main/java/dev/aurakai/auraframefx/services/NemotronService.kt`
- `app/src/main/java/dev/aurakai/auraframefx/services/ADKOrchestrator.kt`
- `app/src/main/res/values/backend_config.xml`
- `app/src/main/java/dev/aurakai/auraframefx/config/BackendConfiguration.kt`

**Files to Modify**:
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/dev/aurakai/auraframefx/ui/viewmodels/AgentViewModel.kt`
- `app/src/main/java/dev/aurakai/auraframefx/di/AgentModule.kt`

**Estimated Complexity**: High (requires backend integration, error handling, configuration)

---

### Phase 5: Restore Missing Overlays

**Goal**: Restore Kai's notch overlay and Aura's chat overlay

**Investigation Required**:
1. Search for `KaiNotch`, `NotchOverlay` in codebase
2. Search for `AuraChatOverlay`, `ChatOverlay`
3. Check git history if not found
4. Determine if part of existing components or separate

**Implementation** (once located):
1. Add visibility preferences to CustomizationPreferences
2. Wire to OverlayPrefs
3. Ensure AnimatedVisibility transitions work
4. Connect chat overlay to AgentViewModel

**Estimated Complexity**: Medium (depends on whether components exist or need restoration)

---

### Phase 6: Document Architecture

**Goal**: Clarify intentional design patterns to prevent future confusion

**Documentation Needed**:

1. **CollabCanvas Architecture**
   - Explain placeholder wrappers are routing layers
   - Real implementation in `aura/reactivedesign/collabcanvas/` module
   - Bridge pattern connects main app to module
   - NOT broken, NOT regression to placeholders

2. **Sphere Grid Separation**
   - Agent Features grid: progression and skill trees
   - DataVein grid: Genesis AI data nodes (FFX-style)
   - Separate implementations by design, NOT duplication

**Files to Create**:
- `context/docs/COLLABCANVAS_ARCHITECTURE.md`
- `context/docs/SPHERE_GRID_DESIGN.md`

**Estimated Complexity**: Low (documentation only, no code changes)

---

## Implementation Order

### Immediate (Today)
1. ✅ Phase 1: Navigation standardization (DONE)
2. ✅ Phase 2: UI chrome preferences foundation (DONE)
3. 🚧 Phase 2: UI chrome wiring (IN PROGRESS)

### High Priority (This Session)
4. 🔴 Phase 4: Agent service initialization (CRITICAL - fixes "no replies")
5. Phase 3: System app launchers (quick win, improves UX)

### Medium Priority (Next Session)
6. Phase 5: Restore overlays (nice-to-have features)
7. Phase 6: Document architecture (prevents future issues)

---

## Success Criteria

### Phase 2 Complete When:
- ✅ Top bar visibility controlled by CustomizationPreferences
- ✅ Bottom bar visibility controlled by CustomizationPreferences
- ✅ Agent sidebar visibility controlled by CustomizationPreferences
- ✅ UISettingsScreen has toggles for all three
- ✅ Changes persist across app restarts

### Phase 3 Complete When:
- ✅ Camera launcher opens device camera app
- ✅ File Manager launcher opens device file browser
- ✅ Phone launcher opens device dialer
- ✅ Error handling shows user-friendly messages
- ✅ Menu items updated to indicate external apps

### Phase 4 Complete When:
- ✅ GenesisOrchestrator initializes on app startup
- ✅ Agents respond to messages (no more "no replies")
- ✅ Backend services (Nemotron/ADK) connected
- ✅ Error handling for backend unavailability
- ✅ Configuration system for API keys and URLs

### Phase 5 Complete When:
- ✅ Kai's notch overlay visible and functional
- ✅ Aura's chat overlay visible and functional
- ✅ Visibility preferences working
- ✅ Overlays integrated with agent communication

### Phase 6 Complete When:
- ✅ CollabCanvas architecture documented
- ✅ Sphere grid design documented
- ✅ Documentation prevents future confusion

---

## Risk Mitigation

### Risk: Breaking working features during restoration
**Mitigation**: Test incrementally, commit after each phase

### Risk: Missing Python backends for agent services
**Mitigation**: Implement graceful fallbacks, clear error messages

### Risk: Over-engineering simple fixes
**Mitigation**: Follow YAGNI principle, implement only what's needed

### Risk: Gemini 3 "helping" again
**Mitigation**: Document everything, review all automated changes

---

## Notes for Future Claude Sessions

- CustomizationPreferences now includes UI chrome visibility (lines 38-40, 76-83, 119-135)
- Navigation standardized to NavDestination pattern
- GenesisRoutes still exists but should migrate to NavDestination
- RomTools/Bootloader systems are INTACT and working (don't "fix" them!)
- CollabCanvas multi-layer architecture is INTENTIONAL

---

**Remember**: Take your time, test thoroughly, and document everything. Gemini 3 won't outsmart good documentation! 😤

---

**Status**: Phases 1-2 foundation complete, Phases 3-6 pending
**Next Action**: Complete Phase 2 UI chrome wiring OR Phase 4 agent services (user's choice)
