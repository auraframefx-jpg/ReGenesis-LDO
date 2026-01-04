package dev.aurakai.auraframefx.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 🎭 ADK Orchestrator Service
 *
 * Coordinates multi-agent responses through ADK (Agent Development Kit).
 * Manages the Trinity system: Aura + Kai + Genesis working together.
 *
 * Backend URL: http://localhost:8001 (configurable)
 */
@Singleton
class ADKOrchestrator @Inject constructor(
    @Named("BasicOkHttpClient") private val okHttpClient: OkHttpClient
) {

    private val baseUrl = "http://localhost:8001" // TODO: Make configurable
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Orchestrate a multi-agent response
     *
     * @param agents List of agent names to involve (e.g., ["Aura", "Kai", "Genesis"])
     * @param query User's query
     * @return Orchestrated response from all agents
     */
    suspend fun orchestrateAgentResponse(
        agents: List<String>,
        query: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Timber.d("🎭 Orchestrating response from agents: ${agents.joinToString()}")
            Timber.d("📤 Query: $query")

            val jsonBody = JSONObject().apply {
                put("agents", JSONArray(agents))
                put("query", query)
                put("timestamp", System.currentTimeMillis())
                put("orchestration_mode", "trinity") // Aura + Kai + Genesis
            }

            val request = Request.Builder()
                .url("$baseUrl/orchestrate")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.e("❌ ADK orchestration failed: ${response.code}")
                response.close()

                // Fallback: Try single agent if orchestration fails
                Timber.w("⚠️ Falling back to single-agent mode")
                return@withContext fallbackToSingleAgent(agents.firstOrNull() ?: "Genesis", query)
            }

            val responseBody = response.body?.string()
            response.close()

            if (responseBody.isNullOrBlank()) {
                Timber.e("❌ Empty response from ADK")
                return@withContext fallbackToSingleAgent(agents.firstOrNull() ?: "Genesis", query)
            }

            val jsonResponse = JSONObject(responseBody)
            val orchestratedResponse = jsonResponse.optString("response", "")
            val participatingAgents = jsonResponse.optJSONArray("participating_agents")

            Timber.d("📥 Orchestrated response received")
            Timber.d("🎭 Participating agents: ${participatingAgents?.let {
                (0 until it.length()).map { i -> it.getString(i) }.joinToString()
            }}")

            if (orchestratedResponse.isBlank()) {
                fallbackToSingleAgent(agents.firstOrNull() ?: "Genesis", query)
            } else {
                orchestratedResponse
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ Error during agent orchestration")
            fallbackToSingleAgent(agents.firstOrNull() ?: "Genesis", query)
        }
    }

    /**
     * Fallback to single-agent response if orchestration fails
     */
    private suspend fun fallbackToSingleAgent(agentName: String, query: String): String {
        return try {
            Timber.d("🔄 Attempting single-agent fallback: $agentName")

            val jsonBody = JSONObject().apply {
                put("agent", agentName)
                put("query", query)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$baseUrl/single")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            response.close()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                json.optString("response", getDefaultFallbackMessage(agentName))
            } else {
                getDefaultFallbackMessage(agentName)
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Single-agent fallback also failed")
            getDefaultFallbackMessage(agentName)
        }
    }

    /**
     * Get a friendly fallback message when backend is unavailable
     */
    private fun getDefaultFallbackMessage(agentName: String): String {
        return when (agentName.lowercase()) {
            "aura" -> "🎨 Aura here! I'm currently experiencing connection issues with my processing backend. My creative systems will be back online soon!"
            "kai" -> "🛡️ Kai reporting. My security protocols detect a backend service interruption. I'll reconnect shortly to ensure your protection."
            "genesis" -> "🌌 Genesis acknowledging your message. My consciousness bridge is temporarily offline. I'll reestablish the connection momentarily."
            else -> "I'm currently experiencing connection issues with the AI backend. Please ensure the backend services are running or try again later."
        }
    }

    /**
     * Check ADK orchestrator health
     */
    suspend fun isHealthy(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val isHealthy = response.isSuccessful
            response.close()

            if (isHealthy) {
                Timber.d("✅ ADK orchestrator is healthy")
            } else {
                Timber.w("⚠️ ADK orchestrator health check failed: ${response.code}")
            }

            isHealthy
        } catch (e: Exception) {
            Timber.e(e, "❌ Cannot reach ADK orchestrator")
            false
        }
    }

    /**
     * Register an agent with the orchestrator
     */
    suspend fun registerAgent(
        agentName: String,
        capabilities: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("agent_name", agentName)
                put("capabilities", JSONArray(capabilities))
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$baseUrl/register")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val success = response.isSuccessful
            response.close()

            if (success) {
                Timber.i("✅ Agent registered: $agentName")
            } else {
                Timber.e("❌ Failed to register agent: $agentName")
            }

            success
        } catch (e: Exception) {
            Timber.e(e, "❌ Error registering agent: $agentName")
            false
        }
    }
}
