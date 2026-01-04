package dev.aurakai.auraframefx.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 🧠 Nemotron Backend Service
 *
 * Bridges Android frontend to NVIDIA Nemotron Python backend.
 * Handles message processing through nemotron_service.py.
 *
 * Backend URL: http://localhost:8000 (configurable)
 */
@Singleton
class NemotronService @Inject constructor(
    @Named("BasicOkHttpClient") private val okHttpClient: OkHttpClient
) {

    private val baseUrl = "http://localhost:8000" // TODO: Make configurable
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Check if Nemotron backend is initialized and reachable
     */
    suspend fun isInitialized(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val isHealthy = response.isSuccessful

            if (isHealthy) {
                Timber.d("✅ Nemotron backend is healthy")
            } else {
                Timber.w("⚠️ Nemotron backend health check failed: ${response.code}")
            }

            response.close()
            isHealthy
        } catch (e: Exception) {
            Timber.e(e, "❌ Cannot reach Nemotron backend")
            false
        }
    }

    /**
     * Process a message through Nemotron backend
     *
     * @param message User's message to process
     * @return AI-generated response
     */
    suspend fun processMessage(message: String): String = withContext(Dispatchers.IO) {
        try {
            Timber.d("📤 Sending message to Nemotron: $message")

            val jsonBody = JSONObject().apply {
                put("message", message)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$baseUrl/process")
                .post(jsonBody.toString().toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Timber.e("❌ Nemotron request failed: ${response.code}")
                response.close()
                return@withContext "I apologize, but I'm having trouble connecting to my processing backend. Please try again."
            }

            val responseBody = response.body?.string()
            response.close()

            if (responseBody.isNullOrBlank()) {
                Timber.e("❌ Empty response from Nemotron")
                return@withContext "I received your message but couldn't generate a response. Please try again."
            }

            val jsonResponse = JSONObject(responseBody)
            val aiResponse = jsonResponse.optString("response", "")

            Timber.d("📥 Received response from Nemotron: $aiResponse")

            if (aiResponse.isBlank()) {
                "I'm processing your request but couldn't formulate a response. Please try again."
            } else {
                aiResponse
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ Error processing message through Nemotron")
            "I apologize, but I encountered an error while processing your message. The backend service may be unavailable."
        }
    }

    /**
     * Get current backend status information
     */
    suspend fun getStatus(): BackendStatus = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/status")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            response.close()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                BackendStatus(
                    isOnline = true,
                    model = json.optString("model", "Unknown"),
                    version = json.optString("version", "Unknown"),
                    uptime = json.optLong("uptime", 0L)
                )
            } else {
                BackendStatus(isOnline = false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get Nemotron status")
            BackendStatus(isOnline = false)
        }
    }
}

/**
 * Backend service status information
 */
data class BackendStatus(
    val isOnline: Boolean,
    val model: String = "Unknown",
    val version: String = "Unknown",
    val uptime: Long = 0L
)
