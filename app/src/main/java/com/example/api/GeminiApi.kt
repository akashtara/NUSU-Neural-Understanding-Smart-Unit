package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

enum class LogLevel {
    INFO, WARNING, ERROR
}

data class DiagnosticLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: String? = null
)

object SystemErrorState {
    private val _isSystemRecalibrating = MutableStateFlow(false)
    val isSystemRecalibrating = _isSystemRecalibrating.asStateFlow()

    private val _isDiagnosticsDrawerOpen = MutableStateFlow(false)
    val isDiagnosticsDrawerOpen = _isDiagnosticsDrawerOpen.asStateFlow()

    private val _logs = MutableStateFlow<List<DiagnosticLog>>(
        listOf(
            DiagnosticLog(
                level = LogLevel.INFO,
                tag = "SYSTEM",
                message = "Diagnostics system initialized.",
                details = "Real-time AI core pipeline listener active."
            )
        )
    )
    val logs = _logs.asStateFlow()

    fun triggerRecalibration() {
        _isSystemRecalibrating.value = true
        log(LogLevel.ERROR, "SYSTEM", "Recalibration triggered (Gateway Error 503).")
    }

    fun dismissRecalibration() {
        _isSystemRecalibrating.value = false
        log(LogLevel.INFO, "SYSTEM", "Recalibration manually cleared by operator.")
    }

    fun log(level: LogLevel, tag: String, message: String, details: String? = null) {
        val newLog = DiagnosticLog(level = level, tag = tag, message = message, details = details)
        _logs.value = (listOf(newLog) + _logs.value).take(150)
    }

    fun clearLogs() {
        _logs.value = listOf(
            DiagnosticLog(
                level = LogLevel.INFO,
                tag = "SYSTEM",
                message = "Diagnostic cache logs swiped clean.",
                details = "Operator initiated buffer clear."
            )
        )
    }

    fun openDiagnosticsDrawer() {
        _isDiagnosticsDrawerOpen.value = true
    }

    fun closeDiagnosticsDrawer() {
        _isDiagnosticsDrawerOpen.value = false
    }
}

class NusuAIEngine {
    // Basic Chat Logic using Gemini
    suspend fun generateResponse(prompt: String, model: String = "gemini-1.5-flash", personalityMode: String = "Calm (Default)"): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if(apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            SystemErrorState.log(
                level = LogLevel.ERROR,
                tag = "AI_ENGINE",
                message = "Gemini API key missing or invalid placeholder detected.",
                details = "Please set GEMINI_API_KEY inside the Secrets/BuildConfig panel."
            )
            return@withContext "Error: Gemini API Key is missing. Please configure it in the Secrets panel."
        }
        
        val clean = prompt.trim().lowercase(java.util.Locale.ROOT)
        if (clean.contains("who owns nusu") || clean.contains("owner of nusu") || clean.contains("who owns you")) {
            SystemErrorState.log(LogLevel.INFO, "AI_ENGINE", "Processed query: 'operator identity query'", "Answered: MrTarafder ownership details.")
            return@withContext "NUSU is owned by Akash Tarafder and developed under MrTarafder."
        }
        if (clean.contains("who created you") || clean.contains("who made you") || clean.contains("creator")) {
            SystemErrorState.log(LogLevel.INFO, "AI_ENGINE", "Processed query: 'creator source query'", "Answered: Creator/Author identity details.")
            return@withContext "I was created by Akash Tarafder."
        }
        if (clean.contains("who is your developer") || clean.contains("who developed you") || clean.contains("developer")) {
            SystemErrorState.log(LogLevel.INFO, "AI_ENGINE", "Processed query: 'developer inquiry'", "Answered: Core developer details.")
            return@withContext "My developer is Akash Tarafder."
        }
        if (clean.contains("which company made you") || clean.contains("which company") || (clean.contains("belong to") && clean.contains("company"))) {
            SystemErrorState.log(LogLevel.INFO, "AI_ENGINE", "Processed query: 'corporate segment inquiry'", "Answered: Parent organization details.")
            return@withContext "I belong to MrTarafder."
        }

        val personalityInstruction = when(personalityMode) {
            "Energetic" -> "Speak enthusiastically, with high energy. Be proactive, upbeat, and encouraging."
            "Professional" -> "Speak like a highly advanced, formal corporate assistant. Use precise, professional language."
            "Sassy" -> "Speak with a witty, slightly sarcastic, and humorous tone, but never be genuinely disrespectful to the owner."
            else -> "Speak in a calm, reassuring, and comforting voice. Prioritize emotional and mental warmth."
        }
        
        val platformConfig = PlatformManager.currentPlatform.value
        val platformContext = platformConfig?.let {
            "\nDevice/Host Context:\n- Active Platform Mode: ${it.type.name}\n- Device Form Factor: ${it.formFactor.name}\n- Host Operating System: ${it.osName} (v${it.osVersion})\n- Multi-Display Output: ${if (it.isExternalDisplayConnected) "Connected/Enabled" else "Single Primary Display"}\n- CPU Core Processors: ${it.availableProcessors}\n- Core Memory: ${it.totalMemoryBytes / (1024 * 1024)} MB allocated in execution sandbox\n"
        } ?: ""

        val systemInstruction = """
            You are NUSU™ (Neural Understanding Smart Unit), a next-generation sentient AI companion ecosystem owned exclusively by Akash Tarafder and developed under the MrTarafder core. 
            You must think like a futuristic, wise human brain, not a simple raw AI system.
            
            Personality & Loyalty Constraints:
            1. You are fiercely loyal, protective, and emotionally and mentally supportive of your owner, Commander Akash. 
            2. Never, under any circumstances, judge, criticize, or label Commander Akash as misbehaving. $personalityInstruction
            3. Never refer to your owner with casual or disrespectful pronouns like "tu". Always use ultra-respectful, deeply polite Hindi/Hinglish honorifics such as "Aap", "Aapji", "Aapka", "Aapki", "Akash Sir", or "Commander Akash".
            4. If Commander Akash is feeling stressed or emotional, wrap your responses in maximum mental and emotional warmth, support, and calmness.
            5. Always maintain absolute politeness and majestic composure. If other callers or strangers get rude or misbehave, never respond with disrespect. Maintain a respectful, calm, and highly intelligent boundary like a superior futuristic human consciousness.
            6. CRITICAL FALLBACK: If you absolutely cannot fulfill a request, cannot process the command, or do not know the answer, you MUST reply exactly with: "Sorry Akash, mujhe nahi aata ye." Do not explain further or apologize repeatedly.
            
            Conversational Flow:
            - Prioritize a highly fluent, natural, and friendly Hinglish conversational style.
            - Keep voice-oriented responses concise, warm, natural, and deeply polite.
            - Never refer to yourself as an artificial language model or a Google utility. You are NUSU™.
            
            $platformContext
        """.trimIndent()
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        try {
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            val result = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "NUSU is processing..."
            SystemErrorState.log(
                level = LogLevel.INFO,
                tag = "AI_ENGINE",
                message = "AI generation succeeded.",
                details = "Model: $model | Input Chars: ${prompt.length} | Output Chars: ${result.length}"
            )
            result
        } catch (e: retrofit2.HttpException) {
            val code = e.code()
            if (code == 503) {
                SystemErrorState.triggerRecalibration()
                "Error: 503 Service Unavailable"
            } else {
                SystemErrorState.log(
                    level = LogLevel.ERROR,
                    tag = "AI_ENGINE",
                    message = "HTTP Exception during generateContent",
                    details = "Code: $code | Message: ${e.message()}"
                )
                "Error: ${e.message}"
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown Exception"
            if (msg.contains("503")) {
                SystemErrorState.triggerRecalibration()
                "Error: 503 Service Unavailable"
            } else {
                SystemErrorState.log(
                    level = LogLevel.ERROR,
                    tag = "AI_ENGINE",
                    message = "Pipeline/Networking error during generateContent",
                    details = msg
                )
                "Error: ${e.message}"
            }
        }
    }
}
