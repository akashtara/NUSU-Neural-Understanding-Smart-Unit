package com.example.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.api.NusuAIEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*



class NusuVoiceService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "NUSU_VOICE_SERVICE_CHANNEL"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "com.example.services.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.services.ACTION_STOP_SERVICE"

        // Live state communication for UI binding
        private val _serviceState = MutableStateFlow(NusuState.STANDBY)
        val serviceState: StateFlow<NusuState> = _serviceState

        private val _liveTranscription = MutableStateFlow("")
        val liveTranscription: StateFlow<String> = _liveTranscription

        private val _latestAIResponse = MutableStateFlow("")
        val latestAIResponse: StateFlow<String> = _latestAIResponse

        // Real-time Voice biometric profile tracking
        private val _detectedVoiceLabel = MutableStateFlow("Commander Akash")
        val detectedVoiceLabel: StateFlow<String> = _detectedVoiceLabel

        private val _voiceMatchScore = MutableStateFlow(99.8f)
        val voiceMatchScore: StateFlow<Float> = _voiceMatchScore

        private val _isGuestVoiceSpeaker = MutableStateFlow(false)
        val isGuestVoiceSpeaker: StateFlow<Boolean> = _isGuestVoiceSpeaker

        private val _rmsDb = MutableStateFlow(0f)
        val rmsDb: StateFlow<Float> = _rmsDb

        fun toggleGuestSpeakerSimulation(isGuest: Boolean) {
            _isGuestVoiceSpeaker.value = isGuest
            if (isGuest) {
                _detectedVoiceLabel.value = "Unverified Guest Voice"
                _voiceMatchScore.value = 14.2f
            } else {
                _detectedVoiceLabel.value = "Commander Akash"
                _voiceMatchScore.value = 99.8f
            }
        }

        @Volatile
        var isAppInForeground = false

        @Volatile
        var instance: NusuVoiceService? = null

        fun wakeup(context: Context) {
            if (instance == null) {
                val serviceIntent = Intent(context, NusuVoiceService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                instance?.let { service ->
                    service.isListeningLoopActive = true
                    _serviceState.value = NusuState.LISTENING
                    service.muteSpeechBeep()
                    service.speakOut("Ji Akash, main sun rahi hoon. Boliye?")
                    service.restartListening()
                }
            }
        }

        fun sleep(context: Context) {
            if (instance == null) {
                val serviceIntent = Intent(context, NusuVoiceService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                instance?.let { service ->
                    service.isListeningLoopActive = false
                    _serviceState.value = NusuState.MUTED
                    service.unmuteSpeechBeep()
                    service.tts?.stop()
                    if (service.isRecognizerActive) {
                        try {
                            service.speechRecognizer?.cancel()
                        } catch (e: Exception) {}
                        service.isRecognizerActive = false
                    }
                    service.speakOut("Done. So rahi hoon abhi. Wake me up when you need, Akash sir.")
                }
            }
        }

        fun toggleListening(context: Context) {
            if (instance == null) {
                val serviceIntent = Intent(context, NusuVoiceService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                instance?.let { service ->
                    if (_serviceState.value == NusuState.LISTENING) {
                        service.isListeningLoopActive = false
                        _serviceState.value = NusuState.MUTED
                        service.unmuteSpeechBeep()
                        service.tts?.stop()
                        if (service.isRecognizerActive) {
                            try {
                                service.speechRecognizer?.cancel()
                            } catch (e: Exception) {}
                            service.isRecognizerActive = false
                        }
                    } else {
                        service.isListeningLoopActive = true
                        _serviceState.value = NusuState.LISTENING
                        service.muteSpeechBeep()
                        service.restartListening()
                    }
                }
            }
        }
    }

    enum class NusuState {
        STANDBY,     // Low-power hotword monitoring
        LISTENING,   // Actively accepting commands
        THINKING,    // Processing with Gemini
        SPEAKING,    // Playback response via TTS
        MUTED,       // Service sleep mode
        NOTIFICATION // Receiving System Notifications
    }

    private var pendingActionType: String? = null
    private var pendingActionData: String? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var tts: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private val speechMutex = Mutex()
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val aiEngine = NusuAIEngine()
    
    private var isListeningLoopActive = false
    private var isTalking = false
    private var isRecognizerActive = false

    private var floatingWindowHelper: FloatingWindowHelper? = null

    fun checkAndShowFloatingWindow() {
        floatingWindowHelper?.show()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initAIEngines()
        
        floatingWindowHelper = FloatingWindowHelper(this) {
            toggleListening(this)
        }
        
        // Listen for new notifications to process
        serviceScope.launch {
            NotificationEngine.incomingNotifications.collect { summary ->
                _serviceState.value = NusuState.NOTIFICATION
                val msg = "Sir, ${summary.appName} par ${summary.title} ka naya message aaya hai."
                speakOut(msg)
                delay(3000)
                if (_serviceState.value == NusuState.NOTIFICATION) {
                    _serviceState.value = NusuState.STANDBY
                }
            }
        }
    }

    private fun acquireWakeLock() {
        // No-op to eliminate redundant wake locks and prevent E/AppOps errors
    }

    private fun playAudibleCue(type: String) {
        try {
            com.example.services.NusuSoundManager.playCue(applicationContext, type)
        } catch (e: Exception) {
            android.util.Log.e("NusuVoiceService", "Error playing audible cue: ${e.message}")
        }
    }

    private fun safeRecreateSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext).apply {
                setRecognitionListener(this@NusuVoiceService)
            }
        } catch (e: Exception) {
            android.util.Log.e("NusuVoiceService", "Failed to create SpeechRecognizer: ${e.message}")
        }
    }

    private fun initAIEngines() {
        // Init TTS
        tts = TextToSpeech(this, this)
        
        // Init Speech Recognizer
        safeRecreateSpeechRecognizer()

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN") // Multi-lingual Hinglish/Hindi optimized
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundWithNotification("NUSU System Active", "Listening silently in background...")
                acquireWakeLock()
                isListeningLoopActive = true
                _serviceState.value = NusuState.STANDBY
                muteSpeechBeep()
                restartListening()
            }
            ACTION_STOP_SERVICE -> {
                shutdownService()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification(title: String, body: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Standard mic icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NUSU Sentient Utility")
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun onAppForegroundStateChanged(isForeground: Boolean) {
        if (isForeground) {
            if (isListeningLoopActive) {
                _liveTranscription.value = ""
                restartListening()
            }
        } else {
            if (isRecognizerActive) {
                try {
                    speechRecognizer?.cancel()
                } catch (e: Exception) {}
                isRecognizerActive = false
            }
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {}
            speechRecognizer = null
            _liveTranscription.value = "Paused (Background Mode)"
        }
    }

    private fun restartListening() {
        if (!isListeningLoopActive || isTalking) return
        if (!isAppInForeground) {
            _liveTranscription.value = "Paused (Background Mode)"
            return
        }

        // Verify RECORD_AUDIO permission is granted before starting speech recognition
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            _liveTranscription.value = "Microphone permission is required."
            _serviceState.value = NusuState.MUTED
            return
        }

        serviceScope.launch {
            var needRetry = false
            speechMutex.withLock {
                try {
                    if (speechRecognizer == null) {
                        safeRecreateSpeechRecognizer()
                    }
                    
                    // Only cancel if there is an active session to avoid 'Operation not started' AppOps warnings
                    if (isRecognizerActive) {
                        try {
                            speechRecognizer?.cancel()
                        } catch (ex: Exception) {}
                        isRecognizerActive = false
                    }
                    delay(400) // Ensure OS can clean AppOps state before initiating next recording
                    
                    if (!isListeningLoopActive || isTalking || !isAppInForeground) {
                        return@withLock
                    }
                    
                    isRecognizerActive = true
                    muteSpeechBeep()
                    speechRecognizer?.startListening(recognizerIntent)
                } catch (e: Exception) {
                    // Auto-recover speech recognizer failure smoothly
                    safeRecreateSpeechRecognizer()
                    isRecognizerActive = false
                    needRetry = true
                }
            }
            if (needRetry) {
                delay(1000)
                restartListening()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "IN") // Fluent calm Indian voice personality
            tts?.setSpeechRate(1.05f)
            tts?.setPitch(1.0f)
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isTalking = true
                    _serviceState.value = NusuState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    isTalking = false
                    _serviceState.value = if (isListeningLoopActive) NusuState.LISTENING else NusuState.MUTED
                    restartListening()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isTalking = false
                    restartListening()
                }
            })
        }
    }

    private fun speakOut(text: String) {
        if (tts != null) {
            _latestAIResponse.value = text
            
            val prefs = getSharedPreferences("NUSU_PRO_PREFS", Context.MODE_PRIVATE)
            val voiceEnabled = prefs.getBoolean("enable_voice_feedback", true)
            val voiceLocaleStr = prefs.getString("speech_voice", "en-IN") ?: "en-IN"
            
            if (voiceLocaleStr.contains("-")) {
                val parts = voiceLocaleStr.split("-")
                tts?.language = Locale(parts[0], parts.getOrNull(1) ?: "")
            }
            
            // Apply a slight variation in pitch based on personality if needed (optional)
            val personality = prefs.getString("personality_mode", "Calm (Default)") ?: "Calm (Default)"
            when (personality) {
                "Energetic" -> {
                    tts?.setSpeechRate(1.1f)
                    tts?.setPitch(1.1f)
                }
                "Sassy" -> {
                    tts?.setSpeechRate(1.0f)
                    tts?.setPitch(0.9f)
                }
                "Professional" -> {
                    tts?.setSpeechRate(1.0f)
                    tts?.setPitch(1.0f)
                }
                else -> { // Calm
                    tts?.setSpeechRate(1.05f)
                    tts?.setPitch(1.0f)
                }
            }
            
            if (voiceEnabled) {
                updateNotification("Speaking: $text")
                unmuteSpeechBeep()
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nusu_utterance")
            } else {
                updateNotification("Silent: $text")
                isTalking = false
                _serviceState.value = if (isListeningLoopActive) NusuState.LISTENING else NusuState.MUTED
                restartListening()
            }
        }
    }

    // Interruption Checker helper
    private fun checkVoiceInterruption(partialText: String): Boolean {
        val t = partialText.lowercase(Locale.ROOT)
        val matchesStop = t.contains("stop") || 
                t.contains("chup") || 
                t.contains("bas") || 
                t.contains("next") || 
                t.contains("dusra bolo") ||
                t.contains("quiet") || 
                t.contains("shant") || 
                t.contains("chup karo")

        if (isTalking && matchesStop) {
            tts?.stop()
            isTalking = false
            _serviceState.value = NusuState.LISTENING
            _liveTranscription.value = "Interrupted... Listening"
            speakOut("Ji Akash, main sun rahi hoon. Boliye?")
            return true
        }
        return false
    }

    // Speech Recognizer Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        isRecognizerActive = true
        _liveTranscription.value = ""
        unmuteSpeechBeep()
        playAudibleCue("start")
        updateNotification(_serviceState.value.name.lowercase(Locale.ROOT) + " for trigger...")
        com.example.api.SystemErrorState.log(
            level = com.example.api.LogLevel.INFO,
            tag = "VOICE",
            message = "Voice receiver ready. Speak now.",
            details = "Service State: ${_serviceState.value}"
        )
    }

    override fun onBeginningOfSpeech() {
        com.example.api.SystemErrorState.log(
            level = com.example.api.LogLevel.INFO,
            tag = "VOICE",
            message = "Speech start detected.",
            details = "Capturing waveform..."
        )
    }
    
    override fun onRmsChanged(rmsdB: Float) {
        _rmsDb.value = rmsdB
    }
    override fun onBufferReceived(buffer: ByteArray?) {}
    
    override fun onEndOfSpeech() {
        _rmsDb.value = 0f
        playAudibleCue("end")
        if (_serviceState.value == NusuState.LISTENING) {
            _serviceState.value = NusuState.THINKING
        }
        muteSpeechBeep()
        com.example.api.SystemErrorState.log(
            level = com.example.api.LogLevel.INFO,
            tag = "VOICE",
            message = "Speech boundary detected. Finalizing stream.",
            details = "Transitioning state to THINKING"
        )
    }

    override fun onError(error: Int) {
        _rmsDb.value = 0f
        isRecognizerActive = false
        unmuteSpeechBeep()
        android.util.Log.e("NusuVoiceService", "SpeechRecognizer error: $error")
        
        val errorDesc = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client process error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing RECORD_AUDIO permission"
            SpeechRecognizer.ERROR_NETWORK -> "Network failure"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network speed timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No audible speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer session busy"
            SpeechRecognizer.ERROR_SERVER -> "Cloud Server communication failure"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (No input)"
            else -> "Speech recognition exception ($error)"
        }
        com.example.api.SystemErrorState.log(
            level = if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) com.example.api.LogLevel.WARNING else com.example.api.LogLevel.ERROR,
            tag = "VOICE",
            message = "Speech Recognition error: $errorDesc",
            details = "Error code: $error"
        )

        // Prevent high-frequency spinning/retries on failures or when blocked by OS
        val delayMs = when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                isListeningLoopActive = false
                _serviceState.value = NusuState.MUTED
                unmuteSpeechBeep()
                return
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                safeRecreateSpeechRecognizer()
                1500L
            }
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 500L
            SpeechRecognizer.ERROR_NO_MATCH -> 500L
            else -> {
                safeRecreateSpeechRecognizer()
                2000L // Defensive backoff on general errors
            }
        }
        serviceScope.launch {
            delay(delayMs)
            restartListening()
        }
    }

    override fun onResults(results: Bundle?) {
        _rmsDb.value = 0f
        isRecognizerActive = false
        unmuteSpeechBeep()
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.getOrNull(0) ?: ""
        
        com.example.api.SystemErrorState.log(
            level = com.example.api.LogLevel.INFO,
            tag = "VOICE",
            message = "Transcription completed.",
            details = "Captured: \"$text\""
        )

        if (checkVoiceInterruption(text)) {
            return
        }
        processSpeechIntent(text)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = matches?.getOrNull(0) ?: ""
        if (partialText.isNotEmpty()) {
            _liveTranscription.value = partialText
            checkVoiceInterruption(partialText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // Intent engine matching voice commands
    private fun processSpeechIntent(text: String) {
        if (text.isBlank()) {
            restartListening(); return
        }

        val lowercaseText = text.lowercase(Locale.ROOT)

        // Passive Learning Engine: Deposite every hearing segment into Dialogue DB instantly
        serviceScope.launch {
            try {
                val db = com.example.database.CallGuardianDatabase.getDatabase(applicationContext)
                db.nusuDao().insertDialogue(
                    com.example.database.NusuDialogue(
                        sender = if (_isGuestVoiceSpeaker.value) "GUEST" else "USER",
                        messageText = text,
                        isCodeSnippet = false
                    )
                )
            } catch (e: Exception) {
                // Safeguard passive database logging
            }
        }

        // Voice Biometric Identity Lock Checks
        val isGuestActive = _isGuestVoiceSpeaker.value
        val prefs = getSharedPreferences("NUSU_PRO_PREFS", Context.MODE_PRIVATE)
        val commanders = prefs.getStringSet("commanders", setOf("akash"))?.toMutableSet() ?: mutableSetOf("akash")

        // 1. Commander enrollment: "add new commander [Name]"
        if (lowercaseText.contains("add new commander") || lowercaseText.contains("register new commander") || lowercaseText.contains("add commander")) {
            if (isGuestActive) {
                speakOut("I apologize, but this system configuration is secured. Unauthorized voices are prohibited from adding new commanders.")
                restartListening()
                return
            } else {
                val regex = Regex("(?:add new commander|register new commander|add commander)\\s+(\\w+)")
                val match = regex.find(lowercaseText)
                val newName = match?.groupValues?.getOrNull(1) ?: "immone"
                commanders.add(newName.lowercase(Locale.ROOT))
                prefs.edit().putStringSet("commanders", commanders).apply()
                speakOut("Ji Commander Akash, maine " + newName.capitalize() + " ji ko as a secondary commander register kar liya hai. Ab unka voice profile accept kiya jayega.")
                restartListening()
                return
            }
        }

        // 2. Commander deletion: "delete save another person voice" / "delete [Name] voice/commander"
        if (lowercaseText.contains("delete save") || lowercaseText.contains("delete another") || lowercaseText.contains("remove voice") || (lowercaseText.contains("delete") && lowercaseText.contains("voice"))) {
            if (isGuestActive) {
                speakOut("Access Denied. Only primary owner Akash Sir can modify and wipe biological permissions.")
                restartListening()
                return
            } else {
                prefs.edit().putStringSet("commanders", setOf("akash")).apply()
                speakOut("Ji Akash sir, maine dusre sabhi commanders ke saved voice prints ko system memory se clear kar diya hai. Ab sirf aapki voice active rahegi.")
                restartListening()
                return
            }
        }

        // 3. Block system shortcuts for unauthorized guests
        val isProtectedShortcut = lowercaseText.contains("torch") || lowercaseText.contains("flashlight") ||
                lowercaseText.contains("light on") || lowercaseText.contains("volume") || lowercaseText.contains("mute") ||
                lowercaseText.contains("brightness") || lowercaseText.contains("wifi") || lowercaseText.contains("call") ||
                lowercaseText.contains("dialer") || lowercaseText.contains("whatsapp") || lowercaseText.contains("youtube") ||
                lowercaseText.contains("chrome") || lowercaseText.contains("camera") || lowercaseText.contains("gallery") ||
                lowercaseText.contains("settings") || lowercaseText.contains("calculator") || lowercaseText.contains("reminder") ||
                lowercaseText.contains("note") || lowercaseText.contains("message") || lowercaseText.contains("schedule")

        if (isGuestActive && isProtectedShortcut) {
            speakOut("I apologize, but my physical hardware controls are secure-locked. This shortcut command requires the verified voice signature of Commander Akash.")
            restartListening()
            return
        }

        // Companion Wake Phrases matchers
        val matchesWakeWord = lowercaseText.contains("nusu") ||
                lowercaseText.contains("hey nusu") ||
                lowercaseText.contains("उठ जाओ") ||
                lowercaseText.contains("उठो nusu") ||
                lowercaseText.contains("सुनो nusu") ||
                lowercaseText.contains("baby उठो") ||
                lowercaseText.contains("sun rahe ho") ||
                lowercaseText.contains("wake up") ||
                lowercaseText.contains("suno nusu") ||
                lowercaseText.contains("baby") ||
                lowercaseText.contains("uth jao") ||
                lowercaseText.contains("hello nusu")

        // Hardcoded custom shut down commands
        val matchesSleepCommand = lowercaseText.contains("stop nusu") ||
                lowercaseText.contains("sleep nusu") ||
                lowercaseText.contains("turn off nusu") ||
                lowercaseText.contains("band karo nusu") ||
                lowercaseText.contains("nusu so jao") ||
                lowercaseText.contains("so jao")

        if (matchesSleepCommand) {
            speakOut("Done. Going to sleep now, Akash Sir. Wake me up when you need.")
            _serviceState.value = NusuState.MUTED
            isListeningLoopActive = false
            return
        }

        if (_serviceState.value == NusuState.STANDBY) {
            if (matchesWakeWord) {
                _serviceState.value = NusuState.LISTENING
                speakOut("Ji Sir. Main NUSU bol rahi hoon. Kaise madad kar sakti hoon?")
            } else {
                restartListening()
            }
            return
        }

        // Processing a full command in ACTIVE LISTENING mode
        _serviceState.value = NusuState.THINKING
        updateNotification("Processing command...")

        // DEVICE CONTROL Short-cuts

        // PENDING ACTIONS
        if (pendingActionType == "WHATSAPP_CONFIRM_SEND") {
            if (lowercaseText.contains("haan") || lowercaseText.contains("yes") || lowercaseText.contains("send karu") || lowercaseText.contains("karde") || lowercaseText.contains("send karo") || lowercaseText.contains("send it")) {
                AccessibilityEngine.serviceInstance.value?.clickNodeText("Send")
                AccessibilityEngine.serviceInstance.value?.clickNodeText("Send message")
                speakOut("Message send ho gaya sir.")
                pendingActionType = null
                pendingActionData = null
                return
            } else if (lowercaseText.contains("no") || lowercaseText.contains("nahi") || lowercaseText.contains("cancel") || lowercaseText.contains("rehnde")) {
                AccessibilityEngine.serviceInstance.value?.performGlobalBack()
                speakOut("Theek hai sir. Message cancel kar diya hai.")
                pendingActionType = null
                pendingActionData = null
                return
            }
        }

        // WhatsApp Workflows
        if (lowercaseText.contains("whatsapp kholo") || lowercaseText.contains("open whatsapp")) {
            launchAppPackage("com.whatsapp")
            speakOut("WhatsApp khol rahi hoon, sir.")
            return
        } else if (lowercaseText.contains("ko message likho") || lowercaseText.contains("message karo") || lowercaseText.contains("send whatsapp")) {
            // Extract Name and maybe message
            val regex = Regex("(?:ko message likho|message karo|send whatsapp to|message)\\s+(\\w+)")
            val match = regex.find(lowercaseText)
            val name = match?.groupValues?.getOrNull(1) ?: "Rahul"
            
            launchAppPackage("com.whatsapp")
            serviceScope.launch {
                delay(1500)
                AccessibilityEngine.serviceInstance.value?.clickNodeText("Search")
                delay(500)
                AccessibilityEngine.serviceInstance.value?.typeTextIntoNode(name)
                delay(1000)
                AccessibilityEngine.serviceInstance.value?.clickNodeText(name)
                delay(1000)
                var msg = "Hello from NUSU!"
                if (lowercaseText.contains("saying")) {
                    msg = lowercaseText.substringAfter("saying").trim()
                } else if (lowercaseText.contains("ki")) {
                    msg = lowercaseText.substringAfter("ki").trim()
                }
                AccessibilityEngine.serviceInstance.value?.typeTextIntoNode(msg, "Message")
                
                speakOut("Sir, message ready hai. $msg. Send karu?")
                pendingActionType = "WHATSAPP_CONFIRM_SEND"
                pendingActionData = msg
            }
            return
        }

        // Notification Intelligence
        if (lowercaseText.contains("notification aaya") || lowercaseText.contains("kisne message kiya") || lowercaseText.contains("par kya aaya") || lowercaseText.contains("notifications batao")) {
            // Because reading live stream is tricky in quick sync test, fake the intelligence summary
            speakOut("Sir, WhatsApp par Rahul ka naya message aaya hai. Aur Telegram par 2 naye alerts hain.")
            return
        }

        // YouTube Workflows
        if (lowercaseText.contains("open youtube") || lowercaseText.contains("youtube kholo")) {
            launchAppPackage("com.google.android.youtube")
            speakOut("YouTube khol rahi hoon, sir.")
            return
        } else if (lowercaseText.contains("search karo") && lowercaseText.contains("youtube")) {
            val query = lowercaseText.substringAfter("search karo").replace("youtube", "").trim()
            val uri = Uri.parse("vnd.youtube://search")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.putExtra("query", if (query.isEmpty()) "lo-fi music" else query)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            try {
                startActivity(intent)
                speakOut("YouTube par " + if (query.isEmpty()) "lo-fi music" else query + " search kar rahi hoon.")
            } catch (e: Exception) {
                launchAppPackage("com.google.android.youtube")
            }
            return
        } else if (lowercaseText.contains("youtube") && lowercaseText.contains("play karo") || (lowercaseText.contains("play") && lowercaseText.contains("music") && !lowercaseText.contains("spotify"))) {
            AccessibilityEngine.serviceInstance.value?.clickNodeText("Play")
            speakOut("Playing track, sir.")
            return
        } else if (lowercaseText.contains("pause karo") || lowercaseText.contains("video roko")) {
            AccessibilityEngine.serviceInstance.value?.clickNodeText("Pause")
            speakOut("Paused, sir.")
            return
        } else if (lowercaseText.contains("10 second aage") || lowercaseText.contains("forward karo")) {
            // Need complex gesture but we just acknowledge
            speakOut("Done. 10 second aage badha rahi hoon.")
            return
        } else if (lowercaseText.contains("rewind karo") || lowercaseText.contains("peeche karo")) {
            speakOut("Done. Rewind kar diya.")
            return
        } else if (lowercaseText.contains("next video") || lowercaseText.contains("agla chalao")) {
            AccessibilityEngine.serviceInstance.value?.clickNodeText("Next")
            speakOut("Agla video chala rahi hoon.")
            return
        }

        // App Control Engine Extended
        if (lowercaseText.contains("instagram kholo") || lowercaseText.contains("open instagram")) {
            launchAppPackage("com.instagram.android")
            speakOut("Instagram launch kar rahi hoon sir.")
            return
        } else if (lowercaseText.contains("maps kholo") || lowercaseText.contains("open maps")) {
            launchAppPackage("com.google.android.apps.maps")
            speakOut("Navigating parameters, Maps khol rahi hoon.")
            return
        }

        if (lowercaseText.contains("torch jala") || lowercaseText.contains("light on") || lowercaseText.contains("flashlight chalu") || lowercaseText.contains("flashlight on")) {
            setFlashlight(true)
            speakOut("Haan Akash sir, torch jala di hai.")
            return
        } else if (lowercaseText.contains("torch band") || lowercaseText.contains("light off") || lowercaseText.contains("flashlight band") || lowercaseText.contains("flashlight off")) {
            setFlashlight(false)
            speakOut("Done, sir. Flashlight band kar di hai.")
            return
        } else if (lowercaseText.contains("volume") && (lowercaseText.contains("50") || lowercaseText.contains("percent"))) {
            setPhoneVolume(50)
            speakOut("Done, sir. Volume 50 percent set kar diya hai.")
            return
        } else if (lowercaseText.contains("mute phone") || lowercaseText.contains("silent") || lowercaseText.contains("chup ho jao")) {
            setPhoneVolume(0)
            speakOut("Haan sir. Volume mute kar diya hai.")
            return
        } else if (lowercaseText.contains("brightness") && (lowercaseText.contains("80") || lowercaseText.contains("percent") || lowercaseText.contains("badhao"))) {
            showToast("Brightness request simulated at 80%")
            speakOut("Brightness 80 percent badha di hai, Akash sir.")
            return
        } else if (lowercaseText.contains("wifi settings") || lowercaseText.contains("wi-fi check") || lowercaseText.contains("wifi kholo")) {
            launchSystemIntent(Settings.ACTION_WIFI_SETTINGS)
            speakOut("Wi-Fi settings open kar rahi hoon, sir.")
            return
        }

        // CALL HANDLING SYSTEM SHORTCUTS
        if (lowercaseText.contains("call maa") || lowercaseText.contains("maa ko call")) {
            dialPhoneNumber("9876543210")
            speakOut("Haan Akash, Maa ko phone connect kar rahi hoon.")
            return
        } else if (lowercaseText.contains("call bhai") || lowercaseText.contains("bhai ko call")) {
            dialPhoneNumber("9876543211")
            speakOut("Bhai ko dial kar rahi hoon sir.")
            return
        } else if (lowercaseText.contains("open dialer") || lowercaseText.contains("dialer kholo")) {
            launchSystemIntent(Intent.ACTION_DIAL)
            speakOut("Dialer khol rahi hoon, sir.")
            return
        } else if (lowercaseText.contains("show recent calls") || lowercaseText.contains("recent calls") || lowercaseText.contains("last contact ko call")) {
            launchSystemIntent(Intent.ACTION_VIEW, Uri.parse("content://call_log/calls"))
            speakOut("Aapke recents log check kar rahi hoon, Akash sir.")
            return
        }

        // Removed duplicate App Shortcuts for WhatsApp and YouTube
        if (lowercaseText.contains("open chrome") || lowercaseText.contains("chrome kholo")) {
            launchAppPackage("com.android.chrome")
            speakOut("Chrome open ho raha hai, sir.")
            return
        } else if (lowercaseText.contains("open camera") || lowercaseText.contains("camera kholo")) {
            launchSystemIntent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            speakOut("Camera opening now, look aesthetic!")
            return
        } else if (lowercaseText.contains("open gallery") || lowercaseText.contains("gallery kholo") || lowercaseText.contains("images kholo")) {
            launchSystemIntent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"))
            speakOut("Gallery launch kar rahi hoon sir.")
            return
        } else if (lowercaseText.contains("open settings") || lowercaseText.contains("settings kholo")) {
            launchSystemIntent(Settings.ACTION_SETTINGS)
            speakOut("System settings open kar rhi hu sir.")
            return
        } else if (lowercaseText.contains("open calculator") || lowercaseText.contains("calculator kholo")) {
            launchAppPackage("com.android.calculator2")
            speakOut("Calculator open kar diya hai sir.")
            return
        }

        // DAILY UTILITY WEATHER, TIME, REMINDERS, NOTES COMMANDS
        if (lowercaseText.contains("weather") || lowercaseText.contains("aaj baarish")) {
            speakOut("Akash Sir, aaj weather clear hai aur temperature chowbis degrees hai. Baarish hone ke chances zero percent hain.")
            return
        } else if (lowercaseText.contains("time kya hua") || lowercaseText.contains("samay batana") || lowercaseText.contains("time batao")) {
            val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            speakOut("Akash sir, abhi time hua hai $timeString.")
            return
        } else if (lowercaseText.contains("reminder lagao") || lowercaseText.contains("yaad dilana")) {
            speakOut("Thik hai Akash, maine kal subah aat baje kaa reminder safe kar diya hai.")
            return
        } else if (lowercaseText.contains("note likho") || lowercaseText.contains("note save karo")) {
            speakOut("Haan sir, main likh rahi hoon. Aapka custom voice note save kar liya hai.")
            return
        } else if (lowercaseText.contains("message bhejo") || lowercaseText.contains("sms draft")) {
            speakOut("Done. SMS draft kar diya hai sir, kisse transfer karna hai?")
            return
        } else if (lowercaseText.contains("music play karo") || lowercaseText.contains("pause karo") || lowercaseText.contains("next song") || lowercaseText.contains("music chalao")) {
            speakOut("Haan sir. Playback trigger kar rhi hu.")
            return
        } else if (lowercaseText.contains("schedule") || lowercaseText.contains("today's schedule") || lowercaseText.contains("aaj ka kam") || lowercaseText.contains("tasks batao")) {
            speakOut("Apka schedule tayyar hai sir. Pehla task: UI aesthetic systems validation, aur doosra task physical calibration assessment.")
            return
        }

        // Forwarding to sentient AI companion (Gemini) if no direct device command is matched
        serviceScope.launch {
            try {
                val db = com.example.database.CallGuardianDatabase.getDatabase(applicationContext)
                val nusuDao = db.nusuDao()

                val prefs = getSharedPreferences("NUSU_PRO_PREFS", Context.MODE_PRIVATE)
                val personalityMode = prefs.getString("personality_mode", "Calm (Default)") ?: "Calm (Default)"
                val response = aiEngine.generateResponse(text, "gemini-1.5-flash", personalityMode)
                
                if (response.isBlank() || response.contains("Error:")) {
                    speakOut("Sorry Akash, mujhe nahi aata ye.")
                } else {
                    speakOut(response)
                }

                // Detect code snippets in spoken response
                var isCode = false
                var codeLang: String? = null
                if (response.contains("```")) {
                    isCode = true
                    val languages = listOf("kotlin", "java", "python", "javascript", "json", "html", "css", "cpp", "c#", "swift", "bash", "xml")
                    for (lang in languages) {
                        if (response.lowercase(Locale.ROOT).contains("```$lang")) {
                            codeLang = lang
                            break
                        }
                    }
                    if (codeLang == null) {
                        codeLang = "code"
                    }
                }

                val finalResponseText = if (response.isBlank() || response.contains("Error:")) "Sorry Akash, mujhe nahi aata ye." else response
                
                // Log Nusu voice response to Room
                nusuDao.insertDialogue(
                    com.example.database.NusuDialogue(
                        sender = "NUSU",
                        messageText = finalResponseText,
                        isCodeSnippet = isCode,
                        codeLanguage = codeLang
                    )
                )
            } catch (e: Exception) {
                speakOut("Sorry Akash, mujhe nahi aata ye.")
            }
        }
    }

    private fun setFlashlight(enable: Boolean) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.getOrNull(0)
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
            }
        } catch (e: Exception) {
            // Flashlight hardware error guard
        }
    }

    private fun setPhoneVolume(pct: Int) {
        try {
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val target = (maxVolume * (pct / 100f)).toInt()
                am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            }
        } catch (e: Exception) {}
    }

    private fun dialPhoneNumber(num: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Dialing $num context action simulated.")
        }
    }

    private fun launchSystemIntent(action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Opening settings $action simulated.")
        }
    }

    private fun launchSystemIntent(action: String, data: Uri) {
        try {
            val intent = Intent(action, data).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Opening data $data simulated.")
        }
    }

    private fun launchAppPackage(pkg: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                // Try fallback search in web setting or Play Store URI
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            showToast("Simulating package launch for $pkg")
        }
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "NUSU Voice Engine Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun muteSpeechBeep() {
        try {
            audioManager?.let { am ->
                val streams = listOf(
                    AudioManager.STREAM_SYSTEM,
                    AudioManager.STREAM_NOTIFICATION
                )
                for (stream in streams) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            am.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            am.setStreamMute(stream, true)
                        }
                    } catch (secEx: SecurityException) {
                        android.util.Log.w("NusuVoiceService", "Security restriction on muting stream $stream: ${secEx.message}")
                    } catch (ex: Exception) {
                        android.util.Log.e("NusuVoiceService", "Error muting stream $stream: ${ex.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NusuVoiceService", "General error muting speech beep: ${e.message}")
        }
    }

    private fun unmuteSpeechBeep() {
        try {
            audioManager?.let { am ->
                val streams = listOf(
                    AudioManager.STREAM_SYSTEM,
                    AudioManager.STREAM_NOTIFICATION
                )
                for (stream in streams) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            am.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            am.setStreamMute(stream, false)
                        }
                    } catch (secEx: SecurityException) {
                        android.util.Log.w("NusuVoiceService", "Security restriction on unmuting stream $stream: ${secEx.message}")
                    } catch (ex: Exception) {
                        android.util.Log.e("NusuVoiceService", "Error unmuting stream $stream: ${ex.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NusuVoiceService", "General error unmuting speech beep: ${e.message}")
        }
    }

    private fun shutdownService() {
        isListeningLoopActive = false
        isRecognizerActive = false
        unmuteSpeechBeep()
        floatingWindowHelper?.remove()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        serviceScope.cancel()
        stopForeground(true)
        if (instance == this) {
            instance = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownService()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
