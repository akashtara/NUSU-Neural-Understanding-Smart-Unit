package com.example.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.NusuAIEngine
import com.example.api.SystemErrorState
import com.example.api.LogLevel
import com.example.database.CallGuardianDatabase
import com.example.database.NusuDialogue
import com.example.database.NusuRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class NusuViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CallGuardianDatabase.getDatabase(application)
    private val repository = NusuRepository(database.nusuDao())
    private val aiEngine = NusuAIEngine()
    
    private val sharedPrefs = application.getSharedPreferences("NUSU_PRO_PREFS", Context.MODE_PRIVATE)
    
    private val _selectedModel = MutableStateFlow(
        sharedPrefs.getString("selected_core_model", "gemini-1.5-flash") ?: "gemini-1.5-flash"
    )
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isVoiceFeedbackEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("enable_voice_feedback", true)
    )
    val isVoiceFeedbackEnabled: StateFlow<Boolean> = _isVoiceFeedbackEnabled.asStateFlow()

    private val _personalityMode = MutableStateFlow(
        sharedPrefs.getString("personality_mode", "Calm (Default)") ?: "Calm (Default)"
    )
    val personalityMode: StateFlow<String> = _personalityMode.asStateFlow()

    private val _speechVoice = MutableStateFlow(
        sharedPrefs.getString("speech_voice", "en-IN") ?: "en-IN"
    )
    val speechVoice: StateFlow<String> = _speechVoice.asStateFlow()

    private val _isTouchOverlayEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("dev_touch_overlay_enabled", false)
    )
    val isTouchOverlayEnabled: StateFlow<Boolean> = _isTouchOverlayEnabled.asStateFlow()

    private val _touchSensitivity = MutableStateFlow(
        sharedPrefs.getFloat("dev_touch_sensitivity", 10f)
    )
    val touchSensitivity: StateFlow<Float> = _touchSensitivity.asStateFlow()

    fun selectModel(model: String) {
        _selectedModel.value = model
        sharedPrefs.edit().putString("selected_core_model", model).apply()
    }

    fun setVoiceFeedbackEnabled(enabled: Boolean) {
        _isVoiceFeedbackEnabled.value = enabled
        sharedPrefs.edit().putBoolean("enable_voice_feedback", enabled).apply()
        com.example.services.BackupManager.performBackup(getApplication())
    }

    fun setPersonalityMode(mode: String) {
        _personalityMode.value = mode
        sharedPrefs.edit().putString("personality_mode", mode).apply()
        com.example.services.BackupManager.performBackup(getApplication())
    }

    fun setSpeechVoice(voiceLocale: String) {
        _speechVoice.value = voiceLocale
        sharedPrefs.edit().putString("speech_voice", voiceLocale).apply()
        com.example.services.BackupManager.performBackup(getApplication())
    }

    fun setTouchOverlayEnabled(enabled: Boolean) {
        _isTouchOverlayEnabled.value = enabled
        sharedPrefs.edit().putBoolean("dev_touch_overlay_enabled", enabled).apply()
        com.example.services.BackupManager.performBackup(getApplication())
        SystemErrorState.log(
            level = LogLevel.INFO,
            tag = "DEV_SETTINGS",
            message = "System touch overlay toggled: $enabled",
            details = "Configuration changed. System backup completed."
        )
    }

    fun setTouchSensitivity(sensitivity: Float) {
        _touchSensitivity.value = sensitivity
        sharedPrefs.edit().putFloat("dev_touch_sensitivity", sensitivity).apply()
        com.example.services.BackupManager.performBackup(getApplication())
        SystemErrorState.log(
            level = LogLevel.INFO,
            tag = "DEV_SETTINGS",
            message = "Touch slop threshold sensitivity offset set to: $sensitivity",
            details = "Threshold updated. System backup completed."
        )
    }

    fun performSystemBackup() {
        com.example.services.BackupManager.performBackup(getApplication())
        SystemErrorState.log(
            level = LogLevel.INFO,
            tag = "BACKUP_SYSTEM",
            message = "Critical code and database state snapshot generated.",
            details = "Successful system-wide backup completed."
        )
    }

    val allDialogues: StateFlow<List<NusuDialogue>> = repository.allDialogues
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCodeSnippets: StateFlow<List<NusuDialogue>> = repository.savedCodeSnippets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMoodEntries: StateFlow<List<com.example.database.MoodEntry>> = repository.allMoodEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertMood(moodTitle: String, productivity: Int, notes: String) {
        viewModelScope.launch {
            repository.insertMoodEntry(com.example.database.MoodEntry(
                moodTitle = moodTitle,
                productivityLevel = productivity,
                privateNotes = notes
            ))
        }
    }

    init {
        // Pre-seed greeting message if database history empty
        viewModelScope.launch {
            val dbList = repository.getAllDialoguesDirect()
            if (dbList.isEmpty()) {
                repository.insertDialogue(
                    NusuDialogue(
                        sender = "NUSU",
                        messageText = "Hello Akash. I am NUSU. Your companion link is fully synced. Ask me about coding, notes, or assistant shortcuts!",
                        isCodeSnippet = false
                    )
                )
            }
        }
    }

    fun sendMessage(text: String, onComplete: () -> Unit = {}) {
        if (text.isBlank()) return
        SystemErrorState.log(
            level = LogLevel.INFO,
            tag = "CORE_CHAT",
            message = "Initiating prompt dispatch pipeline.",
            details = "Input length: ${text.length} chars | Query: \"$text\""
        )
        viewModelScope.launch {
            // Save User message
            repository.insertDialogue(
                NusuDialogue(
                    sender = "USER",
                    messageText = text,
                    isCodeSnippet = false
                )
            )

            // Generate AI response
            try {
                val response = aiEngine.generateResponse(text, _selectedModel.value, _personalityMode.value)
                
                // Detect code snippet
                val (isCode, lang) = detectCodeBlocks(response)
                
                repository.insertDialogue(
                    NusuDialogue(
                        sender = "NUSU",
                        messageText = response,
                        isCodeSnippet = isCode,
                        codeLanguage = lang
                    )
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown pipe exception"
                SystemErrorState.log(
                    level = LogLevel.ERROR,
                    tag = "CORE_CHAT",
                    message = "Prompt dispatch pipeline failed.",
                    details = errorMsg
                )
                repository.insertDialogue(
                    NusuDialogue(
                        sender = "NUSU",
                        messageText = "A connectivity issue occurred: ${e.message}",
                        isCodeSnippet = false
                    )
                )
            } finally {
                repository.updatePermanentSentinel(getApplication())
                onComplete()
            }
        }
    }

    fun clearAllHistory() {
        SystemErrorState.log(
            level = LogLevel.WARNING,
            tag = "DATABASE",
            message = "Operator request: clear database dialogue history.",
            details = "Performing atomic transaction delete across dialogue tables."
        )
        viewModelScope.launch {
            repository.clearHistory()
            // Add initial welcome back (initial neutral state)
            repository.insertDialogue(
                NusuDialogue(
                    sender = "NUSU",
                    messageText = "Hello Akash. I am NUSU. Your companion link is fully synced. Ask me about coding, notes, or assistant shortcuts!",
                    isCodeSnippet = false
                )
            )
            repository.updatePermanentSentinel(getApplication())
        }
    }

    fun deleteDialogue(id: Int) {
        viewModelScope.launch {
            repository.deleteDialogueById(id)
            repository.updatePermanentSentinel(getApplication())
        }
    }

    fun insertCustomCodeSnippet(code: String, language: String) {
        viewModelScope.launch {
            repository.insertDialogue(
                NusuDialogue(
                    sender = "NUSU",
                    messageText = code,
                    isCodeSnippet = true,
                    codeLanguage = language
                )
            )
            repository.updatePermanentSentinel(getApplication())
        }
    }

    suspend fun exportBackup(): String? {
        repository.updatePermanentSentinel(getApplication())
        return repository.exportCodeAndDialogBackup(getApplication())
    }

    suspend fun restoreBackup(jsonString: String): Boolean {
        val success = repository.restoreCodeAndDialogBackup(getApplication(), jsonString)
        if (success) {
            repository.updatePermanentSentinel(getApplication())
        }
        return success
    }

    suspend fun recoverFromPermanentSentinel(): Boolean {
        val success = repository.restoreFromPermanentSentinel(getApplication())
        return success
    }

    private fun detectCodeBlocks(text: String): Pair<Boolean, String?> {
        val containsBackticks = text.contains("```")
        if (containsBackticks) {
            val languages = listOf("kotlin", "java", "python", "javascript", "json", "html", "css", "cpp", "c#", "swift", "bash", "xml")
            for (lang in languages) {
                if (text.lowercase(Locale.ROOT).contains("```$lang")) {
                    return true to lang
                }
            }
            return true to "code"
        }
        return false to null
    }
}
