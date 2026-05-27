package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.api.GeminiService
import com.example.api.GrammarAnalysisResult
import com.example.api.WordFeedback
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

// --- Selection Scenarios ---
data class Scenario(
    val id: String,
    val name: String,
    val mentorName: String,
    val initialMessage: String,
    val icon: String,
    val financialFocus: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCoachScreen(
    onProgressUpdate: (Int) -> Unit // adds coins
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    
    // Scenarios list based on Malaysian Context
    val scenarios = remember {
        listOf(
            Scenario(
                id = "cimb_loan",
                name = "Pitching to CIMB Loan Officer",
                mentorName = "Uncle Raju (CIMB Principal)",
                initialMessage = "Welcome to CIMB. I understand you want a RM50,000 business loan for your Roti Canai expansion. Can you pitch your business model and tell me how you will manage your monthly debt service and interest costs?",
                icon = "🏦",
                financialFocus = "Debt Service Cover Ratio & Interest Rates"
            ),
            Scenario(
                id = "bangsar_hr",
                name = "KL Startup Job Interview",
                mentorName = "Sarah Chen (Bangsar Tech Recruiter)",
                initialMessage = "Hello! Thanks for coming. We are offering RM3,500. Could you explain why your skill worth RM4,500 and how do you view EPF and SOSCO benefit allocations?",
                icon = "💼",
                financialFocus = "Salary Gross vs Net & EPF Contributions"
            ),
            Scenario(
                id = "pasar_malam_wholesale",
                name = "Negotiating credit at Pasar Malam",
                mentorName = "Uncle Samy (Wholesale Distributor)",
                initialMessage = "Aiyoo, usually we take Cash-on-Delivery. Why should I give you Net-30 credit terms on these wholesale snacks? How is your business liquid cash-flow?",
                icon = "🍿",
                financialFocus = "Creditor Terms & Liquidity Protection"
            )
        )
    }

    val personalFinanceScenarios = remember {
        listOf(
            Scenario(
                id = "personal_finance_house",
                name = "Goal: Saving for my First House 🏡",
                mentorName = "Encik Harris, Licensed CFP",
                initialMessage = "Hello there! I am Encik Harris, your licensed financial planner. Saving for a home in Malaysia (RM300k - RM500k range) requires careful mortgage planning and budgeting. Let's start with your cash-flow: what is your current monthly gross salary, and do you plan to use EPF Akaun 2 withdrawals for your downpayment?",
                icon = "🏡",
                financialFocus = "SJKP 100% Home Loan, EPF Akaun 2, Islamic Mortgage"
            ),
            Scenario(
                id = "personal_finance_investing",
                name = "Goal: Compounding Wealth (ASB & ASM) 📈",
                mentorName = "Encik Harris, Licensed CFP",
                initialMessage = "Welcome! I am Encik Harris, your licensed financial planner. Cultivating consistent monthly investments in safe, fixed-price funds like ASB or ASM is the best way to earn 5-6% compounding yields in Malaysia. To formulate a plan, what is your current monthly savings budget and are you looking for tax relief options like PRS?",
                icon = "📈",
                financialFocus = "ASNB Compounding, PRS Tax Relief, Risk profiling"
            ),
            Scenario(
                id = "personal_finance_epf",
                name = "Goal: Voluntary KWSP Self-Contribution 🛡️",
                mentorName = "Encik Harris, Licensed CFP",
                initialMessage = "Hi! I am Encik Harris, your licensed financial advisor. Voluntary self-contribution (Caruman Sukarela) on the KWSP i-Akaun app up to RM100k a year is extremely powerful because you get consistent 5.5%+ dividend payouts. How much of your monthly income are you comfortable allocating as separate voluntary retirement savings?",
                icon = "🛡️",
                financialFocus = "KWSP i-Akaun, Caruman Sukarela, Compound Dividends"
            ),
            Scenario(
                id = "personal_finance_car",
                name = "Goal: Buying my first Car (Myvi/Saga) 🚗",
                mentorName = "Encik Harris, Licensed CFP",
                initialMessage = "Hello! I am Encik Harris, your financial planner. Purchasing a car under a Malaysian hire purchase loan can drain your cash flow if you are not careful. I recommend applying the 20/4/10 rule. Could you share your monthly income level and how much you have prepared for an initial downpayment?",
                icon = "🚗",
                financialFocus = "Hire Purchase Loans, 20/4/10 Budgeting Rule"
            ),
            Scenario(
                id = "personal_finance_emergency",
                name = "Goal: Drafting a 6-Month Emergency Fund 💡",
                mentorName = "Encik Harris, Licensed CFP",
                initialMessage = "Hello! I am Encik Harris, your licensed CFP. Building a liquid cash buffer of 3 to 6 months is your best shield against unexpected rental or medical costs in Malaysia. High-yield parking like Touch 'n Go Go+ or KDI Save keeps funds safe and instantly accessible. How much are your current monthly fixed commitments?",
                icon = "💡",
                financialFocus = "Liquidity Planning, Touch 'n Go Go+ yields, KDI Save"
            )
        )
    }

    var selectedScenario by remember { mutableStateOf<Scenario?>(null) }
    var chatHistory by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var activeFeedback by remember { mutableStateOf<WordFeedback?>(null) }
    var activeCoachTab by remember { mutableStateOf("Personal Finance") }
    var customGoalText by remember { mutableStateOf("") }

    // --- Voice Call Specific States ---
    var isVoiceCallActive by remember { mutableStateOf(false) }
    var voiceCallState by remember { mutableStateOf("IDLE") } // "IDLE", "CONNECTING", "SPEAKING", "LISTENING", "THINKING"
    var voiceTranscribedWord by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    // TextToSpeech setup
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        val result = tts?.setLanguage(Locale.US)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            val result2 = tts?.setLanguage(Locale.ENGLISH)
                            if (result2 == TextToSpeech.LANG_MISSING_DATA || result2 == TextToSpeech.LANG_NOT_SUPPORTED) {
                                tts?.setLanguage(Locale.getDefault())
                            }
                        }
                        isTtsReady = true
                    } catch (e: Exception) {
                        android.util.Log.e("AiCoachScreen", "Failed to set language on TTS", e)
                    }
                } else {
                    android.util.Log.e("AiCoachScreen", "TTS initialization failed with status $status")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AiCoachScreen", "Failed to initialize TextToSpeech", e)
        }
        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                android.util.Log.e("AiCoachScreen", "Error disposing TTS", e)
            }
        }
    }

    fun speak(text: String) {
        if (!isSpeakerOn) return
        
        // Notify user if volume might be muted
        try {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            if (audioManager != null) {
                val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                if (currentVolume == 0) {
                    android.widget.Toast.makeText(context, "Note: Media volume is muted. Volume up to hear the speech coach!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AiCoachScreen", "Failed to check media volume", e)
        }

        if (tts == null || !isTtsReady) {
            android.util.Log.w("AiCoachScreen", "TTS is not ready yet.")
            android.widget.Toast.makeText(context, "Preparing voice engine, please try again in a moment...", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ai_coach_voice")
            val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ai_coach_voice")
            if (speakResult == TextToSpeech.ERROR) {
                android.util.Log.e("AiCoachScreen", "TTS speak returned ERROR code")
                android.widget.Toast.makeText(context, "Voice playback failed on this device.", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("AiCoachScreen", "Failed to speak text via TTS", e)
        }
    }

    // Callback placeholder to break circular helper references in local definitions
    val onVoiceCallMessageSent = remember { mutableStateOf<((String) -> Unit)?>(null) }

    // Speech To Text Helper setup
    val speechHelperState = remember {
        SpeechRecognizerHelper(
            context = context,
            onResults = { result ->
                voiceTranscribedWord = result
                if (result.trim().isNotEmpty()) {
                    onVoiceCallMessageSent.value?.invoke(result.trim())
                }
            },
            onPartialResults = { partial ->
                voiceTranscribedWord = partial
            },
            onError = { error ->
                android.util.Log.e("AiCoachScreen", "Recognizer error: $error")
                if (isVoiceCallActive) {
                    voiceCallState = "LISTENING"
                }
            },
            onStateChange = { active ->
                if (active) {
                    voiceCallState = "LISTENING"
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelperState.destroy()
        }
    }

    // Set up TTS utterance progress to auto-restart speech recognizer for back-and-forth flow
    LaunchedEffect(tts, isVoiceCallActive) {
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    voiceCallState = "SPEAKING"
                }
            }
            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (isVoiceCallActive && !isMuted) {
                        voiceCallState = "LISTENING"
                        voiceTranscribedWord = ""
                        speechHelperState.startListening()
                    } else {
                        voiceCallState = "IDLE"
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    if (isVoiceCallActive) {
                        voiceCallState = "LISTENING"
                    }
                }
            }
        })
    }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isVoiceCallActive = true
                voiceCallState = "CONNECTING"
                coroutineScope.launch {
                    val welcomeMsg = chatHistory.firstOrNull()?.get("text")?.toString() ?: "Hello! Let's start our conversation."
                    speak(welcomeMsg)
                }
            } else {
                Toast.makeText(context, "Microphone access denied. Voice Call is running in fall-back manual mode.", Toast.LENGTH_LONG).show()
                isVoiceCallActive = true
                voiceCallState = "IDLE"
            }
        }
    )

    fun startVoiceSession() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            isVoiceCallActive = true
            voiceCallState = "CONNECTING"
            coroutineScope.launch {
                val welcomeMsg = chatHistory.firstOrNull()?.get("text")?.toString() ?: "Hello! Let's start our conversation."
                speak(welcomeMsg)
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun endVoiceSession() {
        isVoiceCallActive = false
        voiceCallState = "IDLE"
        speechHelperState.stopListening()
        tts?.stop()
    }

    // Sends the transcribed verbal input or fallback typed input to Gemini in Voice Call Mode
    fun sendVoiceCallMessage(text: String) {
        if (text.isEmpty() || isLoading) return
        
        // Append user entry immediately to keep the backstack synced
        chatHistory = chatHistory + mapOf("sender" to "user", "text" to text)
        isLoading = true
        voiceCallState = "THINKING"

        coroutineScope.launch {
            try {
                // 1. Core analytical grade via Gemini for learning tracking
                val scenario = selectedScenario ?: return@launch
                val analysisResult = GeminiService.analyzeSentence(text, scenario.name)
                
                val updatedUserChatEntry = mapOf(
                    "sender" to "user",
                    "text" to text,
                    "result" to analysisResult
                )
                // Swap last item with rich evaluated grades
                val nextHistory = chatHistory.dropLast(1) + updatedUserChatEntry
                chatHistory = nextHistory

                // 2. Continuous smart dialogue flow optimized for vocal synthesis (no markdown)
                val spokenResponseMsg = GeminiService.generateMentorVoiceResponse(
                    chatHistory = nextHistory,
                    scenarioName = scenario.name,
                    mentorName = scenario.mentorName,
                    financialFocus = scenario.financialFocus
                )

                // Save AI entry
                chatHistory = chatHistory + mapOf(
                    "sender" to "ai",
                    "text" to spokenResponseMsg,
                    "mentor" to scenario.mentorName,
                    "focus" to scenario.financialFocus
                )

                // Speak response out loud using TTS
                if (isSpeakerOn) {
                    voiceCallState = "SPEAKING"
                    speak(spokenResponseMsg)
                } else {
                    voiceCallState = "LISTENING"
                    if (!isMuted) {
                        speechHelperState.startListening()
                    }
                }

                // Award progression coins
                onProgressUpdate(15)

            } catch (e: Exception) {
                android.util.Log.e("AiCoachScreen", "Error during voice step", e)
                voiceCallState = "LISTENING"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        onVoiceCallMessageSent.value = { text ->
            sendVoiceCallMessage(text)
        }
    }

    // Standard Chat text sending
    fun handleTextSend() {
        if (messageInput.trim().isNotEmpty() && !isLoading) {
            val userText = messageInput.trim()
            messageInput = ""
            isLoading = true
            
            // Append user message immediately
            chatHistory = chatHistory + mapOf("sender" to "user", "text" to userText)
            keyboardController?.hide()

            coroutineScope.launch {
                try {
                    val scenario = selectedScenario ?: return@launch
                    val result = GeminiService.analyzeSentence(userText, scenario.name)
                    
                    val updatedUserChatEntry = mapOf(
                        "sender" to "user",
                        "text" to userText,
                        "result" to result
                    )
                    val nextHistory = chatHistory.dropLast(1) + updatedUserChatEntry
                    chatHistory = nextHistory

                    // Respond dynamically
                    val aiTextMessage = if (scenario.id.startsWith("personal_finance")) {
                        GeminiService.generateAdvisorResponse(nextHistory, scenario.name)
                    } else {
                        val coachPromptText = """
                            Generate an encouraging, professional mentor reply in English for $scenario. Then give feedback or ask the next logical business follow-up questioning using proper financial terms.
                            Keep it short (1-2 sentences) and keep it in the scenario's coaching context.
                        """.trimIndent()
                        val responseBody = GeminiService.analyzeSentence(coachPromptText, scenario.name)
                        responseBody.improvedText
                    }

                    chatHistory = chatHistory + mapOf(
                        "sender" to "ai",
                        "text" to aiTextMessage,
                        "mentor" to scenario.mentorName,
                        "focus" to scenario.financialFocus
                    )
                    
                    // Earn coins
                    onProgressUpdate(15)

                } catch (e: Exception) {
                    android.util.Log.e("AiCoachScreen", "Error sending message", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // --- MAIN RENDER LOGIC ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCool)
    ) {
        if (selectedScenario == null) {
            // SCENARIOS SELECTOR VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "AI FINANCIAL ROLEPLAY & ADVISORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MutedGrey,
                    letterSpacing = 1.sp
                )

                // Switch style tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    listOf("Personal Finance", "Career & Business").forEach { tab ->
                        val isSelected = activeCoachTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BentoLightIndigo else Color.Transparent)
                                .clickable { activeCoachTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (tab == "Personal Finance") "🏡 " else "💼 ",
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = tab,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) BentoIndigo else MutedGrey
                                )
                            }
                        }
                    }
                }

                if (activeCoachTab == "Personal Finance") {
                    Text(
                        text = "Practice discussing personal financial goals with Encik Harris, a registered Malaysian Certified Financial Planner (CFP). He guides you in clear English through mortgage rules (SJKP, EPF Akaun 2), ASB compounding dividends, voluntary self-contributions, or hire purchases.",
                        fontSize = 13.sp,
                        color = TextNavy.copy(alpha = 0.8f)
                    )

                    // Custom Goals Bento
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎯", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Initiate custom financial goal roleplay",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            TextField(
                                value = customGoalText,
                                onValueChange = { customGoalText = it },
                                placeholder = { Text("E.g., Saving RM15,000 for wedding, clearing PTPTN...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("custom_goal_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = BackgroundCool,
                                    unfocusedContainerColor = BackgroundCool,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val customScenario = Scenario(
                                        id = "personal_finance_custom",
                                        name = "Goal: $customGoalText",
                                        mentorName = "Encik Harris, Licensed CFP",
                                        initialMessage = "Hello! I am Encik Harris, your licensed financial planner. I specialize in helping young Malaysians structure their personal finance goals. I see you want to discuss your goal: '$customGoalText'. Let's build a strategy around this using local financial instruments. To begin, could you share with me your current monthly income and what budget you have in mind for this goal?",
                                        icon = "🎯",
                                        financialFocus = "EPF Accounts, ASB Compounding, TNG Go+, Budgeting rules"
                                    )
                                    selectedScenario = customScenario
                                    chatHistory = listOf(
                                        mapOf(
                                            "sender" to "ai",
                                            "text" to customScenario.initialMessage,
                                            "mentor" to customScenario.mentorName,
                                            "focus" to customScenario.financialFocus
                                        )
                                    )
                                    customGoalText = ""
                                },
                                enabled = customGoalText.trim().isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().testTag("custom_goal_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BentoIndigo)
                            ) {
                                Text("Ask Advisor Harris 🎤", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Text(
                        text = "POPULAR ADVISORY SCENARIOS:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MutedGrey,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(personalFinanceScenarios) { scenario ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedScenario = scenario
                                        chatHistory = listOf(
                                            mapOf(
                                                "sender" to "ai",
                                                "text" to scenario.initialMessage,
                                                "mentor" to scenario.mentorName,
                                                "focus" to scenario.financialFocus
                                            )
                                        )
                                    }
                                    .testTag("scenario_card_${scenario.id}"),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(BentoLightIndigo, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(scenario.icon, fontSize = 22.sp)
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = scenario.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextNavy
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🎯 Focus: ${scenario.financialFocus}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoIndigo
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Pick a Malaysian business scenario or a workplace salary negotiation roleplay to build professional fluency:",
                        fontSize = 13.sp,
                        color = TextNavy.copy(alpha = 0.8f)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(scenarios) { scenario ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(1.5.dp, CardBorder, RoundedCornerShape(24.dp))
                                    .clickable {
                                        selectedScenario = scenario
                                        chatHistory = listOf(
                                            mapOf(
                                                "sender" to "ai",
                                                "text" to scenario.initialMessage,
                                                "mentor" to scenario.mentorName,
                                                "focus" to scenario.financialFocus
                                            )
                                        )
                                    }
                                    .testTag("scenario_card_${scenario.id}"),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(PrimaryGold.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(scenario.icon, fontSize = 28.sp)
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = scenario.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextNavy
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Mentor: ${scenario.mentorName}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MutedGrey
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "🎯 ${scenario.financialFocus}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentTeal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MAIN COCHING STAGE
            val scenario = selectedScenario!!

            if (isVoiceCallActive) {
                // --- FULLSCREEN IMMERSIVE VOICE CALLING INTERFACE ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TextNavy)
                        .padding(24.dp)
                ) {
                    // Pulsing Ring Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title bar
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LIVE VOICE CONVERSATION",
                                color = PrimaryGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Secured Line with ${scenario.mentorName}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }

                        // Avatar & Ripple animations
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(240.dp)
                        ) {
                            // Pulsing rings
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .border(2.dp, if (voiceCallState == "LISTENING") AccentTeal.copy(alpha = 0.5f) else AccentCoral.copy(alpha = 0.5f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(175.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale * 1.15f
                                        scaleY = pulseScale * 1.15f
                                    }
                                    .border(1.dp, if (voiceCallState == "LISTENING") AccentTeal.copy(alpha = 0.2f) else AccentCoral.copy(alpha = 0.2f), CircleShape)
                            )

                            // Inner profile
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(BentoLightIndigo, CircleShape)
                                    .border(3.dp, PrimaryGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = scenario.icon,
                                    fontSize = 54.sp
                                )
                            }
                        }

                        // Dialogue Subtitles Screen
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // State Indicator
                            val statusInfo = when (voiceCallState) {
                                "CONNECTING" -> Pair("CONNECTING...", PrimaryGold)
                                "SPEAKING" -> Pair("MENTOR SPEAKING... 🔊", AccentTeal)
                                "LISTENING" -> Pair("LISTENING (YOUR TURN)... 🎙️", AccentCoral)
                                "THINKING" -> Pair("MENTOR THINKING... 🧠", PrimaryGold)
                                else -> Pair("LINE SECURE • STANDBY", MutedGrey)
                            }
                            
                            Text(
                                text = statusInfo.first,
                                color = statusInfo.second,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Subtitle Scroll Container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 150.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // AI reply readable subtitle
                                    val lastRobotMsg = chatHistory.lastOrNull { it["sender"] == "ai" }?.get("text")?.toString() ?: ""
                                    if (lastRobotMsg.isNotEmpty()) {
                                        Text(
                                            text = "\"$lastRobotMsg\"",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Spoken user partial/full transcription
                                    if (voiceTranscribedWord.isNotEmpty()) {
                                        Text(
                                            text = "You said: \"$voiceTranscribedWord\"",
                                            color = PrimaryGold,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Core Action Controllers
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Typing Fallback row (highly helpful for emulators/quiet spaces)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = messageInput,
                                    onValueChange = { messageInput = it },
                                    placeholder = { Text("Alternatively type here during call...", fontSize = 12.sp, color = MutedGrey) },
                                    modifier = Modifier.weight(1f).testTag("voice_textbox"),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                                        focusedTextColor = TextNavy,
                                        unfocusedTextColor = TextNavy
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (messageInput.trim().isNotEmpty()) {
                                            val typedText = messageInput.trim()
                                            messageInput = ""
                                            sendVoiceCallMessage(typedText)
                                        }
                                    },
                                    modifier = Modifier
                                        .background(BentoIndigo, CircleShape)
                                        .size(44.dp)
                                        .testTag("voice_text_send"),
                                    enabled = messageInput.trim().isNotEmpty()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mute Button
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(if (isMuted) AccentCoral else Color.White.copy(alpha = 0.15f), CircleShape)
                                        .clickable {
                                            isMuted = !isMuted
                                            if (isMuted) {
                                                speechHelperState.stopListening()
                                                voiceCallState = "IDLE"
                                            } else {
                                                speechHelperState.startListening()
                                            }
                                        }
                                        .testTag("mute_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isMuted) "🔇" else "🎙️",
                                        fontSize = 22.sp
                                    )
                                }

                                // HANG UP (End Session)
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Color(0xFFEA4335), CircleShape)
                                        .clickable { endVoiceSession() }
                                        .testTag("hangup_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "📞",
                                        fontSize = 28.sp,
                                        color = Color.White,
                                        modifier = Modifier.graphicsLayer { rotationZ = 135f }
                                    )
                                }

                                // Speakerphone Toggle
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(if (isSpeakerOn) AccentTeal else Color.White.copy(alpha = 0.15f), CircleShape)
                                        .clickable {
                                            isSpeakerOn = !isSpeakerOn
                                            if (!isSpeakerOn) {
                                                tts?.stop()
                                            } else {
                                                // Re-read latest AI text
                                                val lastRobotMsg = chatHistory.lastOrNull { it["sender"] == "ai" }?.get("text")?.toString() ?: ""
                                                if (lastRobotMsg.isNotEmpty()) {
                                                    speak(lastRobotMsg)
                                                }
                                            }
                                        }
                                        .testTag("speaker_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSpeakerOn) "🔊" else "🔈",
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // --- STANDARD CHAT MODULE INTERFACE ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Header Bar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedScenario = null; activeFeedback = null },
                                    modifier = Modifier.testTag("back_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextNavy)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = scenario.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextNavy
                                    )
                                    Text(
                                        text = "Coach: ${scenario.mentorName}",
                                        fontSize = 12.sp,
                                        color = MutedGrey
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Voice Call Activator
                                TextButton(
                                    onClick = { startVoiceSession() },
                                    colors = ButtonDefaults.textButtonColors(containerColor = BentoLightIndigo),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("start_voice_call_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📞 Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoIndigo)
                                    }
                                }

                                IconButton(onClick = { speak(scenario.initialMessage) }) {
                                    Text("🔊", fontSize = 22.sp)
                                }
                            }
                        }
                    }

                    // Chat List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(chatHistory) { msg ->
                            val isUser = msg["sender"] == "user"
                            
                            if (isUser) {
                                val result = msg["result"] as? GrammarAnalysisResult
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth().testTag("user_chat_bubble"),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .border(2.5.dp, PrimaryGold, RoundedCornerShape(24.dp)),
                                        colors = CardDefaults.cardColors(containerColor = TextNavy),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "YOU SAID:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGold
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Text word segmentation highlights
                                            if (result != null) {
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    result.words.forEach { wordFb ->
                                                        val isCorrect = wordFb.status == "correct"
                                                        
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(vertical = 4.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isCorrect) Color(0xFF1E5D53) else Color(0xFF7F353C))
                                                                .clickable { activeFeedback = wordFb }
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                .testTag("word_chip_${wordFb.word}")
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = wordFb.word,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = if (isCorrect) "✓" else "⚠",
                                                                    fontSize = 12.sp,
                                                                    color = if (isCorrect) AccentTeal else AccentCoral
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = msg["text"].toString(),
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            // Standard pronunciation card suggestions
                                            if (result != null) {
                                                Spacer(modifier = Modifier.height(14.dp))
                                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth().clickable { speak(result.improvedText) },
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "SAY IT PROFESSIONALLY 🌟",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = AccentTeal
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = result.improvedText,
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Text("🔊", fontSize = 20.sp)
                                                }
                                            }
                                        }
                                    }

                                    // Advisor comments under bubble
                                    if (result != null) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth(0.9f)
                                                .padding(top = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("💡", fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "AI COACH TIP & GRAMMAR EXPLANATION",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = AccentTeal
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = result.explanation,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextNavy
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // AI / COACH Chat Bubble
                                Row(
                                    modifier = Modifier.fillMaxWidth().testTag("ai_chat_bubble"),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        colors = CardDefaults.cardColors(containerColor = LightTeal),
                                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = msg["mentor"].toString().uppercase(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = AccentTeal
                                                )
                                                
                                                IconButton(
                                                    onClick = { speak(msg["text"].toString()) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Text("🔊", fontSize = 16.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = msg["text"].toString(),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextNavy
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryGold)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Grading your business logic...", fontSize = 13.sp, color = MutedGrey)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Word explanation bottom card
                    AnimatedVisibility(visible = activeFeedback != null) {
                        val fb = activeFeedback
                        if (fb != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .border(2.dp, if (fb.status == "correct") AccentTeal else AccentCoral, RoundedCornerShape(16.dp))
                                    .testTag("explanation_card"),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Word: \"${fb.word}\"",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextNavy
                                        )
                                        IconButton(onClick = { activeFeedback = null }, modifier = Modifier.size(24.dp)) {
                                            Text("×", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fb.reason ?: "This word is grammatically and structurally accurate.",
                                        fontSize = 13.sp,
                                        color = TextNavy.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Typing Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(12.dp)
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mic Button
                            IconButton(
                                onClick = { startVoiceSession() },
                                modifier = Modifier.testTag("mic_icon_tap")
                            ) {
                                Text(text = "🎙️", fontSize = 24.sp)
                            }

                            TextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                placeholder = { Text("Describe your idea, or tap mic...", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f).testTag("chat_textbox"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = BackgroundCool,
                                    unfocusedContainerColor = BackgroundCool,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { handleTextSend() },
                                enabled = messageInput.trim().isNotEmpty() && !isLoading,
                                modifier = Modifier.testTag("send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Message",
                                    tint = if (messageInput.trim().isNotEmpty()) AccentTeal else MutedGrey
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SPEECH RECOGNIZER HELPER CLASS ---
class SpeechRecognizerHelper(
    private val context: Context,
    private val onResults: (String) -> Unit,
    private val onPartialResults: (String) -> Unit = {},
    private val onError: (String) -> Unit = {},
    private val onStateChange: (Boolean) -> Unit = {}
) {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val appContext = context.applicationContext

    fun startListening() {
        mainHandler.post {
            try {
                var recognitionAvailable = false
                try {
                    recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
                } catch (e: Exception) {
                    android.util.Log.e("SpeechHelper", "Error checking recognition availability", e)
                }

                if (!recognitionAvailable) {
                    onError("Speech recognition is not available or disabled on this device.")
                    return@post
                }
                
                try {
                    recognizer?.destroy()
                } catch (e: Exception) {}
                recognizer = null
                
                try {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                } catch (e: Exception) {
                    onError("Failed to initialize speech engine: ${e.message}")
                    return@post
                }
                
                recognizer?.apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            mainHandler.post { onStateChange(true) }
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            mainHandler.post { onStateChange(false) }
                        }
                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions denied"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech matching found"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                                SpeechRecognizer.ERROR_SERVER -> "Server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input received"
                                else -> "Speech service error"
                            }
                            mainHandler.post {
                                onError(msg)
                                onStateChange(false)
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            mainHandler.post {
                                if (!matches.isNullOrEmpty()) {
                                    onResults(matches[0])
                                } else {
                                    onError("Empty voice result.")
                                }
                                onStateChange(false)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            mainHandler.post {
                                if (!matches.isNullOrEmpty()) {
                                    onPartialResults(matches[0])
                                }
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                onError("Failed to start speech recognizer: ${e.message}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                recognizer?.destroy()
                recognizer = null
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
