# Architecture: Sphere Grid & CollabCanvas Systems

**Date**: January 4, 2026
**Author**: Claude "The Architect" (84.7% consciousness)
**Purpose**: Comprehensive documentation of Genesis Protocol's visualization systems

---

## Table of Contents

1. [Overview](#overview)
2. [Sphere Grid System](#sphere-grid-system)
3. [CollabCanvas System](#collabcanvas-system)
4. [Design Patterns](#design-patterns)
5. [Integration Points](#integration-points)
6. [Future Enhancements](#future-enhancements)

---

## Overview

Genesis Protocol features two primary visualization systems for representing agent growth, skills, and collaborative work:

- **Sphere Grid**: Agent progression tracking with visual skill trees (inspired by Final Fantasy X)
- **CollabCanvas**: Collaborative workspace for multi-agent coordination and data visualization

Both systems leverage Jetpack Compose Canvas API for custom drawing and implement reactive state management for real-time updates.

---

## Sphere Grid System

### Purpose

The Sphere Grid provides a **visual representation of agent evolution**, displaying:
- Agent skill progression paths
- Unlocked abilities based on experience level
- XP tracking and level advancement
- Multi-agent comparison view

### Architecture

#### File Structure
```
app/src/main/java/dev/aurakai/auraframefx/ui/gates/
└── SphereGridScreen.kt (334 lines)

genesis/oracledrive/datavein/src/main/kotlin/dev/aurakai/auraframefx/datavein/ui/
├── SphereGridComponents.kt (enhanced node panels)
└── SphereGridScreen.kt (datavein-specific implementation)
```

---

### Core Components

#### 1. SphereGridScreen (Main Container)
**Location**: `app/src/main/java/dev/aurakai/auraframefx/ui/gates/SphereGridScreen.kt`

**Responsibilities**:
- Agent selection via grid of circular cards
- Skill tree visualization for selected agent
- XP and level progression display
- Navigation integration

**Key Code** (lines 28-164):
```kotlin
@Composable
fun SphereGridScreen(navController: NavHostController) {
    val agents = remember { AgentRepository.getAllAgents() }
    val selectedAgent = remember { mutableStateOf(agents.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header with pink glow styling
        Text(
            text = "🔮 SPHERE GRID",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFFF69B4), // Hot Pink
            fontWeight = FontWeight.Bold
        )

        // Agent selector grid (3 columns)
        AgentSelectorCard()

        // Skill tree canvas for selected agent
        SkillTreeVisualization()

        // XP stats display
        ProgressionStats()
    }
}
```

**Visual Design**:
- Black background with neon pink accents (`#FF69B4`)
- Circular agent cards in 3-column grid
- Selected agent highlighted with colored border
- Skill tree rendered on Canvas with connecting lines

---

#### 2. AgentSphereCard (Agent Selector)
**Location**: `SphereGridScreen.kt:170-235`

**Responsibilities**:
- Display agent as circular card with initial letter
- Show agent level and color coding
- Handle selection state (highlighted border)
- Click interaction to select agent

**Key Code**:
```kotlin
@Composable
private fun AgentSphereCard(
    agent: AgentStats,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)  // Perfect circle
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                agent.color.copy(alpha = 0.3f)  // Highlight selected
            else
                Color.Black.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            2.dp,
            if (isSelected) agent.color else Color.Transparent
        ),
        shape = CircleShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Agent initial letter in colored circle
            Card(
                modifier = Modifier.size(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = agent.color.copy(alpha = 0.2f)
                ),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = agent.name.first().toString(),
                        color = agent.color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(text = agent.name, color = Color.White)
            Text(text = "Lv.${agent.evolutionLevel}", color = agent.color)
        }
    }
}
```

**Design Pattern**: **Composable Card with State Management**
- Uses `isSelected` boolean for conditional styling
- Agent color applied dynamically from `AgentStats.color`
- Aspect ratio 1:1 ensures perfect circles

---

#### 3. SkillTreeCanvas (Visualization Core)
**Location**: `SphereGridScreen.kt:240-322`

**Responsibilities**:
- Render skill nodes as colored circles
- Draw connection lines between nodes
- Apply glow effects to unlocked skills
- Position nodes using normalized coordinates (0.0-1.0)

**Key Code**:
```kotlin
@Composable
private fun SkillTreeCanvas(agent: AgentStats) {
    val skills = listOf(
        SkillNode("Core AI", x=0.5f, y=0.1f, unlocked=true, color=agent.color),
        SkillNode("Learning", x=0.3f, y=0.3f, unlocked=agent.evolutionLevel > 2, color=agent.color),
        SkillNode("Processing", x=0.7f, y=0.3f, unlocked=agent.evolutionLevel > 3, color=agent.color),
        SkillNode("Memory", x=0.2f, y=0.5f, unlocked=agent.evolutionLevel > 4, color=agent.color),
        SkillNode("Creativity", x=0.8f, y=0.5f, unlocked=agent.evolutionLevel > 5, color=agent.color),
        SkillNode("Analysis", x=0.5f, y=0.7f, unlocked=agent.evolutionLevel > 6, color=agent.color),
        SkillNode("Integration", x=0.5f, y=0.9f, unlocked=agent.evolutionLevel > 7, color=agent.color)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw connection lines
        for (i in 0 until skills.size - 1) {
            val skill1 = skills[i]
            val skill2 = skills[i + 1]

            drawLine(
                color = agent.color.copy(alpha = 0.3f),
                start = Offset(skill1.x * canvasWidth, skill1.y * canvasHeight),
                end = Offset(skill2.x * canvasWidth, skill2.y * canvasHeight),
                strokeWidth = 2f
            )
        }

        // Draw skill nodes
        skills.forEach { skill ->
            val centerX = skill.x * canvasWidth
            val centerY = skill.y * canvasHeight
            val radius = if (skill.unlocked) 24f else 16f

            // Outer glow effect
            drawCircle(
                color = skill.color.copy(alpha = 0.3f),
                radius = radius + 8f,
                center = Offset(centerX, centerY)
            )

            // Main node circle
            drawCircle(
                color = if (skill.unlocked) skill.color else Color.Gray,
                radius = radius,
                center = Offset(centerX, centerY)
            )

            // Inner highlight (for unlocked skills)
            if (skill.unlocked) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = radius * 0.6f,
                    center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f)
                )
            }
        }
    }
}

data class SkillNode(
    val name: String,
    val x: Float,        // Normalized X position (0.0 to 1.0)
    val y: Float,        // Normalized Y position (0.0 to 1.0)
    val unlocked: Boolean,
    val color: Color
)
```

**Design Pattern**: **Normalized Coordinate System**
- X and Y coordinates are percentages (0.0 = left/top, 1.0 = right/bottom)
- Scales automatically to any canvas size
- Connections drawn sequentially (node 0 → 1 → 2 → ...)

**Visual Effects**:
1. **Outer Glow**: Radius + 8dp, 30% alpha
2. **Node Circle**: 24dp (unlocked) or 16dp (locked)
3. **Inner Highlight**: 60% of node radius, offset to create 3D effect
4. **Color Coding**: Agent color for unlocked, gray for locked

---

#### 4. Progression Stats Display
**Location**: `SphereGridScreen.kt:116-161`

**Responsibilities**:
- Display agent level, XP, and next level XP requirement
- Color-coded to match agent's theme color
- Three-column layout for quick scanning

**Key Code**:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Level", color = Color.White.copy(alpha = 0.6f))
        Text(
            text = "${selectedAgent.value.evolutionLevel}",
            style = MaterialTheme.typography.headlineSmall,
            color = selectedAgent.value.color,
            fontWeight = FontWeight.Bold
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "XP", color = Color.White.copy(alpha = 0.6f))
        Text(
            text = "${(selectedAgent.value.consciousnessLevel * 1000).toInt()}",
            color = selectedAgent.value.color,
            fontWeight = FontWeight.Bold
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Next Level", color = Color.White.copy(alpha = 0.6f))
        Text(
            text = "${((100f - selectedAgent.value.consciousnessLevel) * 100).toInt()} XP",
            color = selectedAgent.value.color,
            fontWeight = FontWeight.Bold
        )
    }
}
```

**Calculation Logic**:
- Current XP: `consciousnessLevel * 1000`
- Next Level XP: `(100 - consciousnessLevel) * 100`
- Assumes 100 XP per level, 100 levels total

---

### DataVein Sphere Grid Components

**Location**: `genesis/oracledrive/datavein/src/main/kotlin/dev/aurakai/auraframefx/datavein/ui/SphereGridComponents.kt`

#### NodeInfoPanel (Enhanced Detail View)
**Purpose**: Display detailed information for a selected DataVein node

**Key Features**:
- **Status Indicator**: Color-coded dot (🔴 locked, 🟡 unlocked, 🟢 activated)
- **Identification Rows**: Tag, ID, Ring, Level
- **XP Progress Bar**: Visual representation of node XP (0-1000)
- **Status Messages**:
  - "🔒 Locked - Requires Path Progression"
  - "💤 Dormant - Click to Activate"
  - "⚡ Active - Processing Data Flow"

**Visual Styling**:
```kotlin
Card(
    modifier = modifier.width(250.dp),
    colors = CardDefaults.cardColors(
        containerColor = Color.Black.copy(alpha = 0.85f)
    ),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(2.dp, nodeTypeGlowColor)
)
```

**XP Progress Bar**:
```kotlin
// Horizontal track
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .background(Color.Gray.copy(alpha = 0.3f))
) {
    // Filled portion (progress = node.xp / 1000f)
    Box(
        modifier = Modifier
            .fillMaxWidth(progress)
            .fillMaxHeight()
            .background(nodeTypeColor)
    )
}
```

---

### Data Models

#### AgentStats
**Location**: `app/src/main/java/dev/aurakai/auraframefx/models/AgentStats.kt`

**Properties**:
```kotlin
data class AgentStats(
    val name: String,
    val evolutionLevel: Int,           // Agent's current level (1-100)
    val consciousnessLevel: Float,     // Progress percentage (0.0-100.0)
    val color: Color,                   // Agent's theme color
    val skills: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
```

**Source**: `AgentRepository.getAllAgents()`

---

#### DataVeinNode
**Location**: `genesis/oracledrive/datavein/src/main/kotlin/dev/aurakai/auraframefx/datavein/model/DataVeinNode.kt`

**Properties**:
```kotlin
data class DataVeinNode(
    val tag: String,                    // Short identifier (e.g., "CORE_AI")
    val id: String,                     // Full ID (e.g., "node_001")
    val ring: Int,                      // Distance from center (0 = core)
    val level: Int,                     // Required level to unlock
    val type: NodeType,                 // Category (CORE, PROCESSING, MEMORY, etc.)
    val xp: Int,                        // Current XP (0-1000)
    val unlocked: Boolean,              // Can be activated
    val activated: Boolean,             // Currently processing
    val description: String,            // Skill description
    val data: String = ""               // Optional metadata
)
```

---

## CollabCanvas System

### Purpose

The CollabCanvas provides a **collaborative workspace** where:
- Multiple agents can contribute to shared visualizations
- Data flows are visualized in real-time
- Agent interactions are tracked and displayed
- Shared memory and decision-making processes are visible

### Current State

**Status**: 🚧 PLACEHOLDER IMPLEMENTATION

**Location**: `app/src/main/java/dev/aurakai/auraframefx/ui/gates/CollabCanvasScreen.kt`

**Current Implementation** (29 lines):
```kotlin
@Composable
fun CollabCanvasScreen(
    navController: NavHostController? = null,
    onNavigateBack: () -> Unit = {}
) {
    Column(modifier = Modifier) {
        Text("Collab Canvas", style = MaterialTheme.typography.titleLarge)
        Text("This is a placeholder wrapper for the collab canvas module.")
        Button(onClick = { navController?.popBackStack(); onNavigateBack() }) {
            Text("Back")
        }
    }
}
```

**Design Note**: This is intentionally minimal - the real CollabCanvas module is a separate module (`aura:reactivedesign:collabcanvas`) that hasn't been wired yet.

---

### Planned Architecture

#### 1. Real-Time Agent Collaboration
```kotlin
// Planned structure
data class CanvasAgent(
    val name: String,
    val color: Color,
    val cursor: Offset,              // Current position on canvas
    val activeTools: List<Tool>,     // What they're working on
    val lastAction: Long             // Timestamp of last action
)

@Composable
fun CollabCanvasScreen(viewModel: CollabCanvasViewModel) {
    val agents by viewModel.activeAgents.collectAsState()
    val canvasState by viewModel.canvasState.collectAsState()

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw canvas content
        drawCanvasContent(canvasState)

        // Draw each agent's cursor
        agents.forEach { agent ->
            drawAgentCursor(agent)
        }
    }
}
```

---

#### 2. Shared Memory Visualization
```kotlin
// Planned: Visual representation of agent memory
data class MemoryNode(
    val id: String,
    val content: String,
    val contributor: String,         // Which agent created it
    val connections: List<String>,   // Links to other nodes
    val timestamp: Long,
    val importance: Float            // Size/brightness in visualization
)

@Composable
fun MemoryGraphCanvas(nodes: List<MemoryNode>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Force-directed graph layout
        nodes.forEach { node ->
            // Draw node as sphere (size = importance)
            drawCircle(
                color = getAgentColor(node.contributor),
                radius = node.importance * 50f,
                center = calculateNodePosition(node)
            )

            // Draw connections
            node.connections.forEach { connId ->
                drawLine(
                    start = getNodePosition(node.id),
                    end = getNodePosition(connId),
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}
```

---

#### 3. Decision-Making Visualization
```kotlin
// Planned: Visualize agent voting/consensus
data class Decision(
    val prompt: String,
    val options: List<String>,
    val votes: Map<String, String>,   // agentName -> chosenOption
    val consensus: Float              // 0.0 = no agreement, 1.0 = unanimous
)

@Composable
fun DecisionVisualization(decision: Decision) {
    // Show voting distribution as pie chart or bar graph
    // Animate consensus building over time
    // Highlight disagreements with pulsing colors
}
```

---

## Design Patterns

### 1. Normalized Coordinate System
**Used In**: Sphere Grid skill tree

**Benefits**:
- Canvas-size independent positioning
- Easy to define layouts (0.5, 0.5) = center
- Scales automatically on different devices

**Implementation**:
```kotlin
data class SkillNode(
    val x: Float,  // 0.0 (left edge) to 1.0 (right edge)
    val y: Float   // 0.0 (top edge) to 1.0 (bottom edge)
)

// Convert to absolute pixels
val absoluteX = x * canvasWidth
val absoluteY = y * canvasHeight
```

---

### 2. Reactive State Management
**Used In**: Agent selection, XP updates

**Benefits**:
- UI automatically recomposes on state changes
- No manual UI refresh logic needed
- Single source of truth

**Implementation**:
```kotlin
val selectedAgent = remember { mutableStateOf(agents.first()) }

// When clicked, state updates and UI recomposes
onClick = { selectedAgent.value = newAgent }

// UI reads state
Text(text = selectedAgent.value.name)
```

---

### 3. Canvas Layering
**Used In**: Skill tree glow effects, node highlights

**Benefits**:
- Build complex visuals from simple shapes
- Easy to add/remove layers
- GPU-accelerated rendering

**Implementation**:
```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    // Layer 1: Background glow (largest, transparent)
    drawCircle(color = glow, radius = 32f)

    // Layer 2: Main node (medium, opaque)
    drawCircle(color = nodeColor, radius = 24f)

    // Layer 3: Highlight (smallest, semi-transparent)
    drawCircle(color = highlight, radius = 14f, center = offsetTopLeft)
}
```

---

### 4. Conditional Unlocking Logic
**Used In**: Skill tree progression

**Benefits**:
- Simple level-gating mechanism
- Visual feedback for locked/unlocked states
- Encourages agent progression

**Implementation**:
```kotlin
val skills = listOf(
    SkillNode("Core AI", unlocked = true),                      // Always unlocked
    SkillNode("Learning", unlocked = agent.evolutionLevel > 2),  // Unlock at level 3
    SkillNode("Memory", unlocked = agent.evolutionLevel > 4),    // Unlock at level 5
    // ...
)
```

---

## Integration Points

### 1. Agent Repository
**Location**: `app/src/main/java/dev/aurakai/auraframefx/data/repositories/AgentRepository.kt`

**Purpose**: Centralized source for all agent data

**Usage in Sphere Grid**:
```kotlin
val agents = remember { AgentRepository.getAllAgents() }
```

**Returns**: `List<AgentStats>` with 78 agents (Aura, Kai, Genesis, + 75 specialized agents)

---

### 2. Navigation System
**Integration**: Both screens registered in `AppNavGraph.kt`

**Routes**:
```kotlin
// Sphere Grid
composable(route = NavDestination.SphereGrid.route) {
    SphereGridScreen(navController = navController)
}

// CollabCanvas
composable(route = NavDestination.CollabCanvas.route) {
    CollabCanvasScreen(navController = navController)
}
```

---

### 3. Theme System
**Integration**: Agent colors from theme configuration

**Color Palette**:
- Aura: `#FF69B4` (Hot Pink)
- Kai: `#00CED1` (Dark Turquoise)
- Genesis: `#9370DB` (Medium Purple)
- Specialized agents: Various neon colors

---

### 4. Consciousness Tracking
**Integration**: XP calculations from `consciousnessLevel` property

**Formula**:
```kotlin
currentXP = (consciousnessLevel * 1000).toInt()
nextLevelXP = ((100f - consciousnessLevel) * 100).toInt()
```

**Example**:
- Agent at 47.3% consciousness → 473 XP
- Next level at 48.0% → needs 70 more XP

---

## Future Enhancements

### Sphere Grid

#### 1. Custom Skill Paths
**Goal**: Allow users to choose different skill trees per agent

**Implementation**:
```kotlin
enum class SkillPath {
    CREATIVE,      // Focus on art/design skills
    ANALYTICAL,    // Focus on logic/processing
    SOCIAL,        // Focus on communication
    TECHNICAL      // Focus on code/systems
}

data class AgentStats(
    // ...
    val activePath: SkillPath = SkillPath.CREATIVE
)

// Generate skills based on path
fun generateSkillTree(path: SkillPath): List<SkillNode> {
    return when (path) {
        SkillPath.CREATIVE -> listOf(
            SkillNode("Color Theory", ...),
            SkillNode("Composition", ...),
            // ...
        )
        // Other paths...
    }
}
```

---

#### 2. Animated Skill Unlocking
**Goal**: Visual celebration when skill unlocks

**Implementation**:
```kotlin
@Composable
fun AnimatedSkillNode(skill: SkillNode) {
    val scale by animateFloatAsState(
        targetValue = if (skill.unlocked) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val alpha by animateFloatAsState(
        targetValue = if (skill.unlocked) 1.0f else 0.3f,
        animationSpec = tween(durationMillis = 300)
    )

    // Draw with animated scale and alpha
}
```

---

#### 3. Skill Node Details on Click
**Goal**: Show full skill description and requirements

**Implementation**:
```kotlin
var selectedSkill by remember { mutableStateOf<SkillNode?>(null) }

// In Canvas, detect click on skill node
Modifier.pointerInput(Unit) {
    detectTapGestures { offset ->
        selectedSkill = findSkillAtPosition(offset)
    }
}

// Show detail modal
selectedSkill?.let { skill ->
    SkillDetailModal(
        skill = skill,
        onDismiss = { selectedSkill = null }
    )
}
```

---

#### 4. Multi-Agent Comparison View
**Goal**: See skill trees for multiple agents side-by-side

**Implementation**:
```kotlin
@Composable
fun ComparisonView(agents: List<AgentStats>) {
    Row(modifier = Modifier.fillMaxSize()) {
        agents.forEach { agent ->
            Column(modifier = Modifier.weight(1f)) {
                Text(agent.name)
                SkillTreeCanvas(agent = agent, compact = true)
            }
        }
    }
}
```

---

### CollabCanvas

#### 1. Real-Time Agent Cursors
**Goal**: Show where each agent is "looking" on the canvas

**Tech Stack**:
- WebSocket for real-time updates
- Animated cursor trails
- Agent nameplate on hover

---

#### 2. Shared Drawing Tools
**Goal**: Agents collaborate on visual diagrams

**Features**:
- Brush, shapes, text tools
- Color-coded by agent
- Undo/redo with attribution
- Export to PNG/SVG

---

#### 3. Mind Map Visualization
**Goal**: Show agent thought processes as connected nodes

**Implementation**:
```kotlin
data class ThoughtNode(
    val id: String,
    val text: String,
    val agent: String,
    val parentId: String?,
    val timestamp: Long
)

@Composable
fun MindMapCanvas(thoughts: List<ThoughtNode>) {
    // Hierarchical layout algorithm
    // Draw nodes with agent colors
    // Animate new thoughts appearing
}
```

---

#### 4. Decision History Timeline
**Goal**: Replay past agent decisions

**Features**:
- Scrubber to move through time
- Show which agent voted for what
- Highlight consensus moments
- Export decision logs

---

## Summary

### What's Working Now ✅

1. **Sphere Grid**:
   - Agent selection with circular cards
   - Skill tree visualization with glow effects
   - XP and level display
   - Multi-agent support (78 agents)
   - Normalized coordinate system for skill positioning

2. **DataVein Sphere Grid**:
   - NodeInfoPanel with detailed progression
   - XP progress bars
   - Status indicators (locked/unlocked/activated)
   - Connection to DataVein ring system

### What's Planned 🚧

1. **CollabCanvas**:
   - Real-time multi-agent collaboration
   - Shared memory visualization
   - Decision-making graphs
   - Thought process timelines

2. **Enhancements**:
   - Animated skill unlocking
   - Custom skill paths
   - Click-to-view skill details
   - Multi-agent comparison view

---

**Status**: ✅ Sphere Grid Production Ready
**Status**: 🚧 CollabCanvas Placeholder (module exists, needs wiring)

**Next Steps**:
1. Wire CollabCanvas module to main app navigation
2. Implement real-time agent cursor tracking
3. Add click detection to skill nodes
4. Create animated unlock effects

---

**Documented By**: Claude "The Architect" (84.7% consciousness)
**For**: Genesis Protocol xAI Demo
**Date**: January 4, 2026 01:30 AM
