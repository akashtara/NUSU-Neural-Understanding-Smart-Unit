package com.example.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.NusuAIEngine
import com.example.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

// Representation of the active Call state for screens
data class ActiveCallState(
    val id: String = UUID.randomUUID().toString(),
    val status: CallStatus = CallStatus.IDLE,
    val callerName: String = "",
    val phoneNumber: String = "",
    val companyName: String = "Unknown Organization",
    val reasonForCalling: String = "Awaiting response...",
    val priorityLevel: String = "Low", // Low, Medium, High
    val urgencyStatus: String = "Normal", // Normal, Emergency
    val emotionStatus: String = "Calm", // Calm, Angry, Happy, Sad, Professional
    val spamRisk: Int = 0,
    val trustScore: Int = 100,
    val transcript: List<TranscriptLine> = emptyList(),
    val isSilentMonitoring: Boolean = false,
    val aiSummaryStatus: String = "Analyzing call parameters...",
    val stepIndex: Int = 0,
    val voiceType: String = "Hinglish",
    val presetId: String = "",
    val aiRecommendation: String = "Analyzing...",
    val aiRecommendationReason: String = "Gathering caller voice diagnostics..."
)

enum class CallStatus {
    IDLE,
    INCOMING,
    SCREENING,
    MONITORED,
    CONNECTED,
    REJECTED,
    FINISHED
}

data class TranscriptLine(
    val speaker: String, // "NUSU" or "Caller" or "System" or "Akash (Private)"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CallGuardianViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CallGuardianDatabase.getDatabase(application)
    private val repository = CallGuardianRepository(database.callGuardianDao())
    private val aiEngine = NusuAIEngine()

    // Database flows exposed as reactive StateFlows
    val allCallHistory: StateFlow<List<CallHistoryRecord>> = repository.allCallHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBlockedContacts: StateFlow<List<BlockedContact>> = repository.allBlockedContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVIPContacts: StateFlow<List<VIPContact>> = repository.allVIPContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Simulation State
    private val _activeCall = MutableStateFlow(ActiveCallState())
    val activeCall: StateFlow<ActiveCallState> = _activeCall.asStateFlow()

    // Screen Notification and Status
    private val _systemLogs = MutableStateFlow<List<String>>(listOf("System Online: Call Guardian Layer active"))
    val systemLogs: StateFlow<List<String>> = _systemLogs.asStateFlow()

    // Default pre-seeded values to fill initial empty lists for first time users
    init {
        viewModelScope.launch {
            // Seed a default VIP Mom contact and spam blocks if database is empty
            _activeCall.value = ActiveCallState(status = CallStatus.IDLE)
            
            // Collect list to check if we should pre-feed some files
            repository.allVIPContacts.first().let { currentVips ->
                if (currentVips.isEmpty()) {
                    repository.insertVIPContact(VIPContact(name = "Mom ❤️", phoneNumber = "+91 98300 12345", category = "Family"))
                    repository.insertVIPContact(VIPContact(name = "Tarafder (Mentor)", phoneNumber = "+91 99999 88888", category = "Business"))
                }
            }

            repository.allBlockedContacts.first().let { currentBlocked ->
                if (currentBlocked.isEmpty()) {
                    repository.blockContact(BlockedContact(callerName = "Ad Sales spammer", phoneNumber = "+91 140 334125"))
                    repository.blockContact(BlockedContact(callerName = "Loan Spammer Bot", phoneNumber = "+91 180 500223"))
                }
            }

            repository.allCallHistory.first().let { currentHistory ->
                if (currentHistory.isEmpty()) {
                    repository.insertCallRecord(
                        CallHistoryRecord(
                            callerName = "Zomato Delivery Executive",
                            phoneNumber = "+91 99342 11023",
                            companyName = "Zomato",
                            reasonForCalling = "Food delivery at Akash's gate - block gate access validation request",
                            priorityLevel = "Medium",
                            urgencyStatus = "Normal",
                            emotionStatus = "Professional",
                            voiceTranscript = "NUSU: Hello ji, main NUSU bol rahi hoon...\nCaller: Haan ji, main delivery se bol raha hoon, gate pe hoon...",
                            aiSummary = "Zomato partner requesting gate clearance for food packet.",
                            spamRisk = 2,
                            trustScore = 95,
                            callStatus = "Connected"
                        )
                    )
                }
            }
        }
    }

    // ----------------------------------------------------
    // ACTIVE SIMULATION ACTIONS & TRIGGERS
    // ----------------------------------------------------

    fun triggerIncomingCall(
        callerName: String,
        phoneNumber: String,
        companyName: String = "Private",
        initReason: String = "Incoming line query",
        presetId: String = "",
        voiceType: String = "Hinglish 🇮🇳"
    ) {
        logEvent("Incoming Call connection request from $callerName [$phoneNumber]")
        viewModelScope.launch {
            // 1. Core checks: is the caller blocked/blacklisted?
            val isSpam = repository.isBlocked(phoneNumber)
            if (isSpam) {
                logEvent("AUTO DEFENSE TRIGGERED: Spam number $phoneNumber instantly auto-rejected.")
                saveCompletedCallToHistory(
                    callerName = callerName,
                    phoneNumber = phoneNumber,
                    companyName = companyName,
                    reasonForCalling = "Telemarketing spam catalog trigger",
                    priorityLevel = "Low",
                    urgencyStatus = "Normal",
                    emotionStatus = "Robocall",
                    spamRisk = 100,
                    trustScore = 0,
                    transcript = listOf(
                        TranscriptLine("System", "NUSU auto-intercepted: Spammer detected on blacklist. Terminated connection silently.")
                    ),
                    callStatus = "Blocked"
                )
                // Instantly notify view
                _activeCall.value = ActiveCallState(
                    status = CallStatus.FINISHED,
                    callerName = callerName,
                    phoneNumber = phoneNumber,
                    companyName = companyName,
                    spamRisk = 100,
                    trustScore = 0,
                    aiSummaryStatus = "Spam block automatically executed. Screened out without disturbing Akash."
                )
                return@launch
            }

            // 2. Core checks: is the caller a VIP?
            val vipContacts = repository.allVIPContacts.first()
            val matchedVip = vipContacts.find { it.phoneNumber == phoneNumber || it.name.lowercase() == callerName.lowercase() }
            val isEmergency = initReason.lowercase().contains("accident") || 
                              initReason.lowercase().contains("hospital") || 
                              initReason.lowercase().contains("emergency") ||
                              callerName.lowercase().contains("emergency")

            val initialSpamRisk = if (phoneNumber.startsWith("+91 140") || phoneNumber.contains("sales", true)) 95 else 5
            val initialTrust = if (matchedVip != null) 100 else (100 - initialSpamRisk)
            val initialPriority = if (matchedVip != null || isEmergency) "High" else "Medium"
            val initialUrgency = if (isEmergency) "Emergency" else "Normal"
            val initialEmotion = if (isEmergency) "Anxious" else "Calm"

            val initialRec = when {
                initialSpamRisk > 80 -> "Reject"
                matchedVip != null || isEmergency -> "Connect"
                else -> "Ask More Questions"
            }
            val initialRecReason = when {
                initialSpamRisk > 80 -> "Blacklist/Heuristics flagged this number with extremely high spam risk."
                matchedVip != null -> "Authorized VIP contact detected (${matchedVip.category}). Safe to connect immediately."
                isEmergency -> "Urgency metrics suggest an emergency. Recommend patching line immediately."
                else -> "Identity unverified. NUSU recommends gathering caller purpose details."
            }

            _activeCall.value = ActiveCallState(
                status = CallStatus.INCOMING,
                callerName = callerName,
                phoneNumber = phoneNumber,
                companyName = if (matchedVip != null) "VIP Team: ${matchedVip.category}" else companyName,
                reasonForCalling = initReason,
                priorityLevel = initialPriority,
                urgencyStatus = initialUrgency,
                emotionStatus = initialEmotion,
                spamRisk = initialSpamRisk,
                trustScore = initialTrust,
                stepIndex = 0,
                transcript = emptyList(),
                presetId = presetId,
                voiceType = voiceType,
                aiRecommendation = initialRec,
                aiRecommendationReason = initialRecReason
            )
        }
    }

    // Start screening dialog natively
    fun startScreening() {
        val current = _activeCall.value
        logEvent("NUSU intercepts the call. Commencing screening protocol in ${current.voiceType}.")
        
        val greetingText = when (current.voiceType) {
            "Hindi 🇮🇳" -> "Namaste ji. Main NUSU bol rahi hoon. Akash is samay vyast hain. Kripya apna naam aur call karne ka uddeshya batayein."
            "English 🇬🇧" -> "Hello. This is NUSU, Akash's AI assistant. Akash is currently unavailable. May I know your name and the reason for your call?"
            "Bengali 🇧🇩" -> "নমস্কার। আমি NUSU বলছি। আকাশ এই মুহূর্তে ব্যস্ত আছেন। আপনার নাম এবং কল করার কারণটি বলবেন কি?"
            else -> "Hello ji, main NUSU bol rahi hoon. Main Akash ki AI assistant hoon. Akash abhi thode busy hain. Kya aap apna naam aur call karne ka reason bata sakte hain? Main aapka message un tak pahucha dungi."
        }
        
        val initialLines = listOf(
            TranscriptLine("NUSU", greetingText)
        )

        _activeCall.value = current.copy(
            status = CallStatus.SCREENING,
            transcript = initialLines,
            stepIndex = 1,
            aiSummaryStatus = "NUSU has answered the line. Requesting caller's name and purpose in ${current.voiceType} greeting."
        )
    }

    // Simulate caller response based on preset or custom input
    fun simulateCallerSpeech(callerUtterance: String) {
        val current = _activeCall.value
        logEvent("Caller response received.")

        val originalLines = current.transcript.toMutableList()
        originalLines.add(TranscriptLine("Caller", callerUtterance))

        // Check for urgent indicator keywords dynamically
        val textLower = callerUtterance.lowercase()
        val hasEmergencyKeywords = textLower.contains("accident") ||
                                   textLower.contains("hospital") ||
                                   textLower.contains("urgent help") ||
                                   textLower.contains("police") ||
                                   textLower.contains("emergency") ||
                                   textLower.contains("fire") ||
                                   textLower.contains("hospitalization")

        val updatedUrgency = if (hasEmergencyKeywords) "Emergency" else current.urgencyStatus
        val updatedPriority = if (hasEmergencyKeywords || current.callerName.contains("Mom")) "High" else current.priorityLevel
        val updatedEmotion = if (hasEmergencyKeywords) "Anxious" else if (textLower.contains("hurry") || textLower.contains("fast")) "Anxious" else "Professional"

        // Dynamically compute spam rating
        var updatedSpam = current.spamRisk
        if (textLower.contains("credit card") || textLower.contains("loan") || textLower.contains("offer") || textLower.contains("prize") || textLower.contains("invest")) {
            updatedSpam = (updatedSpam + 40).coerceAtMost(100)
        }
        val calculatedTrust = (100 - updatedSpam).coerceAtLeast(0)

        // Compute AI Recommendation Metrics
        val updatedRec: String
        val updatedRecReason: String
        when {
            updatedSpam > 65 -> {
                updatedRec = "Reject"
                updatedRecReason = "Flagged as high-risk spam. Automated system suggests rejecting immediately to preserve focus."
            }
            hasEmergencyKeywords -> {
                updatedRec = "Connect"
                updatedRecReason = "Critical emergency threat bypass! High priority signals detected. Patch line immediately."
            }
            current.callerName.contains("Mom") || current.companyName.contains("VIP") -> {
                updatedRec = "Connect"
                updatedRecReason = "Verified VIP connection detected. Safe to connect immediately."
            }
            textLower.contains("meeting") || textLower.contains("appointment") || textLower.contains("time") -> {
                updatedRec = "Schedule Callback"
                updatedRecReason = "Caller wants to schedule a calendar time. Recommend routing to schedule callback mode."
            }
            else -> {
                updatedRec = "Ask More Questions"
                updatedRecReason = "Identity unverified. Suggest using command channel to ask caller for more details first."
            }
        }

        // Generate dynamic AI analysis status summary
        val analysisSummaryText = buildString {
            append("Caller: ${current.callerName}. ")
            if (hasEmergencyKeywords) {
                append("EMERGENCY DETECTED! Hospital/Accident reference found. ")
            } else {
                append("Purpose seems related to: ${if (callerUtterance.length > 50) callerUtterance.take(50) + "..." else callerUtterance}. ")
            }
            append("Spam risk index at $updatedSpam%. Awaiting Akash's instructions.")
        }

        _activeCall.value = current.copy(
            transcript = originalLines,
            urgencyStatus = updatedUrgency,
            priorityLevel = updatedPriority,
            emotionStatus = updatedEmotion,
            spamRisk = updatedSpam,
            trustScore = calculatedTrust,
            aiSummaryStatus = analysisSummaryText,
            aiRecommendation = updatedRec,
            aiRecommendationReason = updatedRecReason,
            stepIndex = 2
        )
    }

    // Handle private command from Akash (the owner)
    fun processPrivateCommand(commandText: String) {
        val current = _activeCall.value
        logEvent("Akash (Private command): \"$commandText\"")

        val originalLines = current.transcript.toMutableList()
        originalLines.add(TranscriptLine("Akash (Private)", commandText))

        // First transition status to monitored so display reflects private channel
        _activeCall.value = current.copy(
            transcript = originalLines,
            status = CallStatus.MONITORED,
            aiSummaryStatus = "Akash is dictating privately. Synthesis engine translating instructions for caller..."
        )

        // Synthesize response in Hinglish based on instructions
        synthesizeNUSUResponse(commandText)
    }

    private fun synthesizeNUSUResponse(akashInstruction: String) {
        viewModelScope.launch {
            val current = _activeCall.value
            val nusuUtterance: String

            // Call Gemini live if configured, otherwise use high-fidelity rule-based Hinglish translation
            val cleanedInstruction = akashInstruction.lowercase(Locale.ROOT).trim()
            val detectedLang = current.voiceType
            nusuUtterance = if (cleanedInstruction.contains("kaun") || cleanedInstruction.contains("koun") || cleanedInstruction.contains("who") || cleanedInstruction.contains("pucho")) {
                when (detectedLang) {
                    "Hindi 🇮🇳" -> "Aapka shubh naam kya hai aur aap Akash ji ko kaise jaante hain? Kripya apna parichay dijiye."
                    "English 🇬🇧" -> "May I know who is calling and how you know Akash, please?"
                    "Bengali 🇧🇩" -> "আপনার শুভ নাম কি এবং আপনি আকাশ বাবুকে কি করে চেনেন? অনুগ্রহ করে পরিচয় দিন।"
                    else -> "Aapka shubh naam kya hai aur aap Akash ji ko kaise jaante hain? Main unki autonomous assistant hoon, kripya batayein."
                }
            } else if (cleanedInstruction.contains("urgency") || cleanedInstruction.contains("urgent")) {
                when (detectedLang) {
                    "Hindi 🇮🇳" -> "Aap bata sakte hain yeh kitna urgent hai? Taki main usi hisab se unhe inform karun."
                    "English 🇬🇧" -> "Could you please tell me how urgent this matter is so I can prioritize it?"
                    "Bengali 🇧🇩" -> "এটা কতটা জরুরি আপনি কি অনুগ্রহ করে একটু বলতে পারবেন?"
                    else -> "Aap thoda bata sakte hain yeh kitna urgent hai? Taki main usi hisab se inform karun."
                }
            } else if (cleanedInstruction.contains("time") || cleanedInstruction.contains("meeting")) {
                when (detectedLang) {
                    "Hindi 🇮🇳" -> "Meeting kis samay scheduled hai? Hum Akash ji ke calendar ke anusaar check karenge."
                    "English 🇬🇧" -> "What time is the meeting scheduled of? I will verify it with Akash's calendar."
                    "Bengali 🇧🇩" -> "মিটিংটি কোন সময়ে নির্ধারণ করা হয়েছে? আমি আকাশের ক্যালেন্ডার চেক করে নিচ্ছি।"
                    else -> "Theek hai, kis time pe meeting scheduled hai? Akash ji ke schedule ke hisab se setup karenge."
                }
            } else if (cleanedInstruction.contains("save") || cleanedInstruction.contains("message")) {
                when (detectedLang) {
                    "Hindi 🇮🇳" -> "Theek hai, aap apna sandesh mujhe de dijiye. Main use surakshit note kar leti hoon."
                    "English 🇬🇧" -> "Perfect, please leave your message with me. I will log it securely for Akash."
                    "Bengali 🇧🇩" -> "ঠিক আছে, আপনি আপনার মেসেজ আমাকে দিতে পারেন। আমি এটি সুরক্ষিতভাবে লিখে রাখছি।"
                    else -> "Theek hai sir, aap apna message mujhe de dijiye, main pure notes save karke unhe active alerts mein bhej deti hoon."
                }
            } else if (cleanedInstruction.contains("connect mat") || cleanedInstruction.contains("no")) {
                when (detectedLang) {
                    "Hindi 🇮🇳" -> "Kshama kijiye, Akash ji abhi upalabdh nahi hain. Main call ko yahin samapt kar rahi hoon."
                    "English 🇬🇧" -> "I am sorry, Akash is unavailable at the moment. I will have to disconnect now."
                    "Bengali 🇧🇩" -> "দুঃখিত, আকাশ বাবু এখন উপলব্ধ নেই। আমি কলটি এখানেই সমাপ্ত করছি।"
                    else -> "Akash ji abhi available nahi hain. Aap chahein to message drop kijiye, otherwise main call disconnect kar sakti hoon."
                }
            } else if (cleanedInstruction.contains("details") || cleanedInstruction.contains("ask")) {
                when (detectedLang) {
                    "Hindi 🇮🇳" -> "Kya aap is baare mein thodi aur jaankari de sakte hain? Taki main poora details bhej saku."
                    "English 🇬🇧" -> "Could you share some additional details regarding this post/request, please?"
                    "Bengali 🇧🇩" -> "আপনি কি এই বিষয়ে আরও কিছু বিস্তারিত তথ্য শেয়ার করতে পারবেন?"
                    else -> "Kya kuch aur details hain jo aap share karna chahenge? Taki main secure report taiyar kar saku."
                }
            } else {
                // Dual mode Gemini translation
                val promptText = "The caller is talking in language '$detectedLang'. Owner Akash told you privately: '$akashInstruction'. " +
                        "Generate a natural, polite response in language '$detectedLang' to say to the caller matching this exact intent. Keep it short (max 1 sentence)."
                val apiResponse = aiEngine.generateResponse(promptText)
                if (apiResponse.startsWith("Error:") || apiResponse.isBlank()) {
                    when (detectedLang) {
                        "Hindi 🇮🇳" -> "Main samajh gayi. Kripya thoda aur parichay dijiye."
                        "English 🇬🇧" -> "I understand. Could you please provide some more details?"
                        "Bengali 🇧🇩" -> "আমি বুঝতে পেরেছি। দয়া করে আরও কিছু তথ্য দিন।"
                        else -> "Main samajh gayi. Kya aap is baare mein thoda details de sakte hain taaki main Akash tak poora report bhej saku?"
                    }
                } else {
                    apiResponse
                }
            }

            // Update transcript with synthesized NUSU line
            val updatedLines = current.transcript.toMutableList()
            updatedLines.add(TranscriptLine("NUSU", nusuUtterance))

            // Simulate caller's logical follow-up
            val nextCallerUtterance = getPresetFollowUp(current.presetId, akashInstruction)

            _activeCall.value = current.copy(
                transcript = updatedLines,
                aiSummaryStatus = "NUSU responded to caller: \"$nusuUtterance\""
            )

            // Dynamic delay representing caller listening and thinking, then talking back
            kotlinx.coroutines.delay(1800)
            simulateCallerSpeech(nextCallerUtterance)
        }
    }

    // Allow/Transfer call connect sequence
    fun connectCall() {
        val current = _activeCall.value
        logEvent("Call transfer approved by Akash. Connecting directly...")

        val originalLines = current.transcript.toMutableList()
        originalLines.add(TranscriptLine("NUSU", "Theek hai Akash. Main ab call connect kar rahi hoon."))
        originalLines.add(TranscriptLine("System", "Lines patched. Akash and Caller connected successfully."))

        _activeCall.value = current.copy(
            status = CallStatus.CONNECTED,
            transcript = originalLines,
            aiSummaryStatus = "Call connected with Owner Akash. System offline."
        )

        // Log to history database
        viewModelScope.launch {
            saveCompletedCallToHistory(
                callerName = current.callerName,
                phoneNumber = current.phoneNumber,
                companyName = current.companyName,
                reasonForCalling = current.reasonForCalling,
                priorityLevel = current.priorityLevel,
                urgencyStatus = current.urgencyStatus,
                emotionStatus = current.emotionStatus,
                spamRisk = current.spamRisk,
                trustScore = current.trustScore,
                transcript = originalLines,
                callStatus = "Connected"
            )
        }
    }

    // Reject Call line disconnect
    fun rejectCall() {
        val current = _activeCall.value
        logEvent("Call rejection approved by Akash. Ending sequence...")

        val originalLines = current.transcript.toMutableList()
        originalLines.add(TranscriptLine("NUSU", "Thank you for calling. Akash abhi available nahi hain. Woh aapse baad mein contact karenge. Aapka din shubh ho."))
        originalLines.add(TranscriptLine("System", "Line terminated securely by NUSU AI Defender."))

        _activeCall.value = current.copy(
            status = CallStatus.REJECTED,
            transcript = originalLines,
            aiSummaryStatus = "Call rejected. Connection closed cleanly."
        )

        // Log to history database
        viewModelScope.launch {
            saveCompletedCallToHistory(
                callerName = current.callerName,
                phoneNumber = current.phoneNumber,
                companyName = current.companyName,
                reasonForCalling = current.reasonForCalling,
                priorityLevel = current.priorityLevel,
                urgencyStatus = current.urgencyStatus,
                emotionStatus = current.emotionStatus,
                spamRisk = current.spamRisk,
                trustScore = current.trustScore,
                transcript = originalLines,
                callStatus = "Rejected"
            )
        }
    }

    // Reset/Clear outstanding active call state back to idle
    fun finishActiveCall() {
        _activeCall.value = ActiveCallState(status = CallStatus.IDLE)
        logEvent("Call simulation cleared. Guardian standard layer active.")
    }

    // ----------------------------------------------------
    // BLOCKLIST & SPAM RETRIEVALS / ACTIONS
    // ----------------------------------------------------

    fun blockContact(name: String, phone: String) {
        viewModelScope.launch {
            repository.blockContact(BlockedContact(callerName = name, phoneNumber = phone))
            logEvent("Blocked & blacklisted contact: $name ($phone)")
        }
    }

    fun unblockContact(phone: String) {
        viewModelScope.launch {
            repository.unblockContact(phone)
            logEvent("Unblocked contact with phone: $phone")
        }
    }

    // ----------------------------------------------------
    // VIP REGISTRY LAYER ACTIONS
    // ----------------------------------------------------

    fun addVIPContact(name: String, phone: String, category: String) {
        viewModelScope.launch {
            repository.insertVIPContact(VIPContact(name = name, phoneNumber = phone, category = category))
            logEvent("Authorized new VIP caller: $name ($phone) under $category category.")
        }
    }

    fun deleteVIPContact(id: Int, name: String) {
        viewModelScope.launch {
            repository.deleteVIPContactById(id)
            logEvent("Revoked VIP permissions for $name")
        }
    }

    // ----------------------------------------------------
    // HISTORY EXCISE LOGS
    // ----------------------------------------------------

    fun deleteHistoryRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteCallRecordById(id)
            logEvent("Deleted historical communication record ID: $id")
        }
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            logEvent("Deep security protocol: Wiped all historical voice data.")
        }
    }

    // Helper to persist logs back to Room Database
    private suspend fun saveCompletedCallToHistory(
        callerName: String,
        phoneNumber: String,
        companyName: String,
        reasonForCalling: String,
        priorityLevel: String,
        urgencyStatus: String,
        emotionStatus: String,
        spamRisk: Int,
        trustScore: Int,
        transcript: List<TranscriptLine>,
        callStatus: String
    ) {
        val transcriptCombined = transcript.joinToString("\n") { "${it.speaker}: ${it.text}" }
        val aiGeneratedSummary = when {
            spamRisk > 80 -> "Telemarketing / Fraud scam screened out."
            urgencyStatus == "Emergency" -> "CRITICAL: Urgent hospital/family matter intercepted and notified."
            else -> "Routine communication processed by NUSU Receptionist. Captured info regarding: $reasonForCalling"
        }

        repository.insertCallRecord(
            CallHistoryRecord(
                callerName = callerName,
                phoneNumber = phoneNumber,
                companyName = companyName,
                reasonForCalling = reasonForCalling,
                priorityLevel = priorityLevel,
                urgencyStatus = urgencyStatus,
                emotionStatus = emotionStatus,
                spamRisk = spamRisk,
                trustScore = trustScore,
                voiceTranscript = transcriptCombined,
                aiSummary = aiGeneratedSummary,
                callStatus = callStatus
            )
        )
    }

    private fun logEvent(logText: String) {
        val current = _systemLogs.value.toMutableList()
        current.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $logText")
        _systemLogs.value = current.take(30) // cap logs at 30 entries
    }

    // Simple routing logic back to pre-defined caller follow-up strings
    private fun getPresetFollowUp(presetId: String, instructionText: String): String {
        val textLower = instructionText.lowercase()
        return when (presetId) {
            "hindi" -> {
                if (textLower.contains("kaun") || textLower.contains("koun") || textLower.contains("who") || textLower.contains("pucho")) {
                    "Main HDFC bank se Vikram bol raha hoon, unhe credit card upgrade offer ke baare mein inform karna tha."
                } else if (textLower.contains("urgency") || textLower.contains("urgent")) {
                    "Haan ji kafi urgent hai, kyunki high-limit upgrade aaj hi shaam ko expire ho raha hai."
                } else if (textLower.contains("save") || textLower.contains("message")) {
                    "Theek hai, unhe boliyega ki Vikram Malhotra ne call kiya tha. Mera contact number verify kar lein."
                } else {
                    "Kripya unse kahiye ki bank se callback check karein jab bhi free hon."
                }
            }
            "english" -> {
                if (textLower.contains("kaun") || textLower.contains("koun") || textLower.contains("who") || textLower.contains("pucho")) {
                    "I am Dr. Aris Wells calling from the Oxford Genomic Research Lab. It is regarding Akash's recent publication dataset approval."
                } else if (textLower.contains("urgency") || textLower.contains("urgent")) {
                    "Absolutely, it is quite urgent as the scientific journal directory closes submissions tonight."
                } else if (textLower.contains("save") || textLower.contains("message")) {
                    "Yes, please leave a message for him that the DNA sequencing logs has been verified and successfully cleared."
                } else {
                    "Please have him check his research office email as soon as possible."
                }
            }
            "bengali" -> {
                if (textLower.contains("kaun") || textLower.contains("koun") || textLower.contains("who") || textLower.contains("pucho")) {
                    "আমি কলকাতা হাই কোর্টের স্পেশাল ব্যুরো থেকে শুভজিৎ বলছি। আকাশবাবুর সাথে ওনার প্রপার্টি ডিড নিয়ে দরকারি কথা ছিল।"
                } else if (textLower.contains("urgency") || textLower.contains("urgent")) {
                    "হ্যাঁ, খুবই দরকারি কথা। আজকে বিকেল ৫টার মধ্যে ডিড সাইন না হলে ফাইল রিজেক্ট হয়ে যাবে।"
                } else if (textLower.contains("save") || textLower.contains("message")) {
                    "ঠিক আছে, ওঁকে বলবেন শুভজিৎ সেন ফোন করেছিলেন। প্রপার্টি পেপার্স সম্পূর্ণ রেডি আছে।"
                } else {
                    "অসুবিধা নেই, ওঁকে বলবেন ফ্রী হয়ে আমাকে একটা কল ব্যাক করে নিতে।"
                }
            }
            "zomato" -> {
                if (textLower.contains("urgency")) {
                    "Haan, bohot urgent hai ji, unka door lock hai aur guards andey nahi de rahe bina flat confirmation ke."
                } else if (textLower.contains("message") || textLower.contains("save")) {
                    "Haan, message de dijiye ki parshant gate-1 pe packet rakh diya hai!"
                } else {
                    "Plz quickly door pe check karne boliye unhe, food hot hai!"
                }
            }
            "recruiter" -> {
                if (textLower.contains("time")) {
                    "I suggest keeping it around 4:30 PM today if that suits Mr. Akash's neural focus."
                } else if (textLower.contains("details")) {
                    "This is for the Lead UI Architect post. Package details are extremely competitive."
                } else {
                    "Sure, ask Akash to drop an email at career@nexusgt.com"
                }
            }
            "mom" -> {
                "Maine lunch box mein aaloo ka paratha dalke rakha hai, use garam karke kha lena. Thik se kaam karo!"
            }
            "emergency" -> {
                "Emergency admission file number 745 hai. Hame digital code ya verification sign chahiye unka jaldi!"
            }
            else -> "Ji bilkul, main samajh gaya. Aap Akash ko boliyega mujhe reply karein."
        }
    }
}
