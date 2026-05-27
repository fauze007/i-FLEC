package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// --- Data Models for UI ---

data class WordFeedback(
    val word: String,
    val status: String, // "correct" or "correction"
    val reason: String? = null
)

data class GrammarAnalysisResult(
    val originalText: String,
    val improvedText: String,
    val words: List<WordFeedback>,
    val explanation: String
)

object GeminiService {
    private const val TAG = "GeminiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a user sentence to Gemini to analyze for financial and English communication clarity.
     * Includes a robust fallback mechanism in case the API key is invalid or missing.
     */
    suspend fun analyzeSentence(
        sentence: String,
        scenarioContext: String
    ): GrammarAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER", ignoreCase = true)) {
            Log.w(TAG, "API Key is missing or default. Using local simulated analytical feedback.")
            return@withContext simulateAnalysis(sentence, scenarioContext)
        }

        val prompt = """
            You are a expert English communication tuner and financial literacy coach specializing in the Malaysian context (using RM currency, Malaysian business situations, and Gen-Z/young adult culture).
            
            The user is participating in this scenario: "$scenarioContext"
            They said: "$sentence"
            
            Perform a word-by-word syntactic grammar check and financial check on their sentence.
            We want:
            1. An improved, highly professional version of their sentence in natural, correct English using appropriate financial terms (e.g., compounding, interest, collateral, budgeting, EPF/SOCSO, liquidity, cash flow).
            2. A word-by-word status categorization. Identify which words are completely correct ("correct") versus which words look incorrect, mispronounced, or part of poor grammatical phrasing ("correction").
            3. A short, highly practical tip explaining the English corrections or the financial literacy concept involved. Show empathy for Malaysian colloquial speech (Manglish slangs like 'lah', 'makan', 'bunga', 'pinjam') but coach them on standard professional equivalents.
            
            You MUST return ONLY a JSON block, containing no other text. Use this JSON format exactly:
            {
              "improvedText": "a polished, natural, professional English sentence",
              "analysis": [
                {"word": "WordA", "status": "correct"},
                {"word": "WordB", "status": "correction", "reason": "Explanation why this specific word/phrase should be changed"}
              ],
              "explanation": "Inline friendly tips pointing out Malaysian context, Manglish corrections, and financial term definitions."
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                Log.d(TAG, "Raw Response: $bodyString")

                val rootJson = JSONObject(bodyString)
                val candidates = rootJson.getJSONArray("candidates")
                val firstPart = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                
                val rawContent = firstPart.getString("text")
                return@withContext parseResult(sentence, rawContent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "API call failed, falling back", e)
            return@withContext simulateAnalysis(sentence, scenarioContext)
        }
    }

    private fun parseResult(originalText: String, rawText: String): GrammarAnalysisResult {
        try {
            val trimmed = extractJsonBlock(rawText)
            val json = JSONObject(trimmed)

            val improvedText = json.optString("improvedText", originalText)
            val explanation = json.optString("explanation", "Excellent effort! Keep talking to accumulate Ringgit coins.")
            
            val array = json.optJSONArray("analysis")
            val wordsList = mutableListOf<WordFeedback>()
            
            if (array != null && array.length() > 0) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val w = obj.optString("word", "")
                    val s = obj.optString("status", "correct")
                    val r = obj.optString("reason", "")
                    if (w.isNotEmpty()) {
                        wordsList.add(WordFeedback(w, s, if (r.isEmpty()) null else r))
                    }
                }
            } else {
                // Parse word by word from original text if analysis array is empty/malformed
                val originalWords = originalText.split(Regex("\\s+"))
                for (ow in originalWords) {
                    wordsList.add(WordFeedback(ow.replace(Regex("[^a-zA-Z]"), ""), "correct"))
                }
            }

            return GrammarAnalysisResult(originalText, improvedText, wordsList, explanation)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse json", e)
            return simulateAnalysis(originalText, "Error recovery mode")
        }
    }

    private fun extractJsonBlock(rawResponse: String): String {
        val startIndex = rawResponse.indexOf("{")
        val endIndex = rawResponse.lastIndexOf("}")
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            rawResponse.substring(startIndex, endIndex + 1)
        } else {
            rawResponse
        }
    }

    /**
     * Local intelligence simulator so that the app works perfectly offline / when no API key is set!
     */
    private fun simulateAnalysis(original: String, context: String): GrammarAnalysisResult {
        val cleanOriginal = original.trim()
        val originalWords = cleanOriginal.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val wordsList = mutableListOf<WordFeedback>()

        // Generate synthetic highlights based on local grammar rules
        var improved = cleanOriginal
        var customExplanation = "Good pitch effort! Let's practice standard financial terms in English."

        if (context.contains("Budget", ignoreCase = true) || context.contains("Salary", ignoreCase = true) || context.contains("Mamak", ignoreCase = true)) {
            val containsBunga = cleanOriginal.contains("bunga", ignoreCase = true)
            val containsLah = cleanOriginal.contains("lah", ignoreCase = true)
            val containsPinjam = cleanOriginal.contains("pinjam", ignoreCase = true)

            for (w in originalWords) {
                val cleanWord = w.replace(Regex("[^a-zA-Z]"), "")
                when {
                    cleanWord.equals("bunga", ignoreCase = true) -> {
                        wordsList.add(WordFeedback(w, "correction", "In English finance, 'bunga' stands for 'interest rate' or 'dividend'."))
                    }
                    cleanWord.equals("lah", ignoreCase = true) -> {
                        wordsList.add(WordFeedback(w, "correction", "Avoid informal particles like 'lah' in formal pitches, business interviews or loan proposals."))
                    }
                    cleanWord.equals("pinjam", ignoreCase = true) -> {
                        wordsList.add(WordFeedback(w, "correction", "Instead of 'pinjam', use 'borrow' (liability side) or 'loan' (financing asset)."))
                    }
                    else -> {
                        wordsList.add(WordFeedback(w, "correct"))
                    }
                }
            }

            if (containsBunga || containsLah || containsPinjam) {
                improved = cleanOriginal
                    .replace("bunga", "interest rate", ignoreCase = true)
                    .replace("lah", "", ignoreCase = true)
                    .replace("pinjam", "borrow", ignoreCase = true)
                    .trim()
                customExplanation = "We corrected Manglish structures. In finance, terms matter! Remember to use 'interest rate' instead of bank 'bunga', and keep your speech professional by omitting 'lah' in proposals."
            } else {
                // If it is regular English, let's just make it sound awesome
                improved = "$cleanOriginal professionally, keeping in mind my monthly budgeting allocation and liquidity needs."
                customExplanation = "We added cash-flow terminology. In Malaysian budgeting, securing liquid resources (your RM savings in touch & go or bank) protects against emergency rent bills in places like KL!"
                for (i in originalWords.indices) {
                    val w = originalWords[i]
                    if (i % 5 == 1) {
                        wordsList.add(WordFeedback(w, "correction", "Can be rephrased for better commercial impact."))
                    } else {
                        wordsList.add(WordFeedback(w, "correct"))
                    }
                }
            }
        } else {
            // General filler word analysis
            for (i in originalWords.indices) {
                val w = originalWords[i]
                val cleanWord = w.replace(Regex("[^a-zA-Z]"), "")
                if (cleanWord.equals("uh", ignoreCase = true) || cleanWord.equals("um", ignoreCase = true)) {
                    wordsList.add(WordFeedback(w, "correction", "Filler sounds lower your communication confidence. Pause instead of saying '$w'."))
                } else if (cleanWord.equals("lah", ignoreCase = true)) {
                    wordsList.add(WordFeedback(w, "correction", "Omitting 'lah' increases formal interview scores."))
                } else {
                    wordsList.add(WordFeedback(w, "correct"))
                }
            }
            improved = cleanOriginal.replace(Regex("(?i)\\b(uh|um|lah)\\b"), "").replace(Regex("\\s+"), " ").trim()
            customExplanation = "Great job! Removing fillers like 'uh' or 'um' dramatically stabilizes your English fluency scores. Your business logic is sound."
        }

        return GrammarAnalysisResult(
            originalText = cleanOriginal,
            improvedText = if (improved.isEmpty()) "Excellent financial pitch." else improved,
            words = wordsList,
            explanation = customExplanation
        )
    }

    /**
     * Highly authentic Malaysian Personal Finance Advisor simulator.
     * Keeps the conversation interactive and guidance-focused when in offline or key-missing mode.
     */
    suspend fun generateAdvisorResponse(
        chatHistory: List<Map<String, Any>>,
        goalContext: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER", ignoreCase = true)) {
            Log.w(TAG, "API Key is missing or default. Using local simulated advisor response.")
            return@withContext simulateAdvisorReply(chatHistory, goalContext)
        }

        val historyStr = StringBuilder()
        for (msg in chatHistory) {
            val sender = if (msg["sender"] == "user") "User" else "Advisor (Encik Harris)"
            val text = msg["text"]?.toString() ?: ""
            historyStr.append("$sender: $text\n\n")
        }

        val prompt = """
            You are Encik Harris, an expert, licensed Malaysian Financial Planner (CFP/IFP) representing MyPF or a reputable Malaysian financial advisory.
            You are helpful, encouraging, and write in highly polished, grammatically correct professional English.
            
            The user has initiated a conversation about their personal finance goal: "$goalContext"
            
            Here is the conversation history so far:
            $historyStr
            
            Based on the history, write the next response from Encik Harris.
            Follow these rules STRICTLY:
            1. Respond in clear, professional English. Do not use heavy Manglish slang yourself, but acknowledge and understand if the user uses Malaysian terms.
            2. Incorporate specific, highly accurate Malaysian financial context and products when relevant:
               - EPF/KWSP (specifically Akaun 1, Akaun 2, or Akaun 3/Fleksibel, or voluntary Caruman Sukarela up to RM100,000 p.a.).
               - ASNB fixed-price funds (such as ASB for Bumiputeras or ASM for all Malaysians, compounding dividend yields).
               - Private Retirement Schemes (PRS) offering RM3,000 tax relief.
               - Touch 'n Go Go+, KDI Save, Rize, or specific local high yield accounts for liquid cash reserves.
               - First home assistance: SJKP (Skim Jaminan Kredit Perumahan) for first-time buyers with gig salaries, or Skim Rumah Pertamaku (SRP).
               - Renting vs Buying in expensive zones like Mont Kiara, Bangsar, or Subang Jaya.
               - Buying Perodua Myvi or Proton Saga using hire purchase loans and the 20/4/10 rule.
            3. Guide the conversation step-by-step. Do NOT dump a massive financial plan in a single turn. Instead, provide 2-4 sentences of empathetic feedback or explanation, and ask EXACTLY ONE clear, guiding question to continue the role-play (e.g., asking about monthly income, saving preferences, risk appetite).
            4. Keep the response concise, friendly, and engaging.
            
            Return ONLY the raw response from Encik Harris. Do not wrap in markdown json block or intro/outros.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                val rootJson = JSONObject(bodyString)
                val candidates = rootJson.getJSONArray("candidates")
                val reply = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                return@withContext reply.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "API call failed for advisor reply, using simulator fallback", e)
            return@withContext simulateAdvisorReply(chatHistory, goalContext)
        }
    }

    private fun simulateAdvisorReply(
        chatHistory: List<Map<String, Any>>,
        goalContext: String
    ): String {
        // Count how many user messages exist in client history
        val userMsgs = chatHistory.filter { it["sender"] == "user" }
        val lastUserMsg = userMsgs.lastOrNull()?.get("text")?.toString()?.lowercase() ?: ""

        if (userMsgs.isEmpty()) {
            return "Hello! I am Encik Harris, your licensed Malaysian financial advisor. I specialize in helping young adults map out their financial life goals. Let's explore your goal: $goalContext. What is your current estimated monthly income, and how much can you save each month?"
        }

        // Check if discussing house, investment, etc.
        val lowerGoal = goalContext.lowercase()
        val step = userMsgs.size

        if (step == 1) {
            return when {
                lowerGoal.contains("house") || lowerGoal.contains("home") || lowerGoal.contains("property") -> {
                    "That is a wonderful goal! Planning for a home in Malaysia (RM300k - RM500k) is a big step. For first-time homebuyers with gig or non-fixed income, SJKP (Skim Jaminan Kredit Perumahan) is excellent because it guarantees up to 100% financing. To start our plan, may I ask, what is your current monthly gross salary and how much do you have saved in your EPF Akaun 2 or Akaun 3?"
                }
                lowerGoal.contains("asb") || lowerGoal.contains("invest") || lowerGoal.contains("wealth") || lowerGoal.contains("prs") -> {
                    "Investing early in Malaysia is the absolute key to compounding wealth. ASB fixed-price funds offer consistent tax-free dividends around 5.5% with high security. For retirement, Private Retirement Schemes (PRS) offer up to RM3,000 tax relief! To help customize this, what is your current monthly income and what level of investment risk are you comfortable with?"
                }
                lowerGoal.contains("car") || lowerGoal.contains("purchase") || lowerGoal.contains("myvi") -> {
                    "Buying a car like a Perodua Myvi or Proton Saga is exciting, but monthly commitments add up quickly when including insurance/takaful, fuel, and service. I recommend the 20/4/10 rule: a 20% downpayment, maximum 4-year loan tenure, and capping total expenses under 10% of monthly salary. What is your monthly take-home pay, and do you have any extra downpayment saved?"
                }
                lowerGoal.contains("epf") || lowerGoal.contains("retirement") || lowerGoal.contains("kwsp") -> {
                    "EPF/KWSP is a rock-solid retirement tool, historically yielding 5% to 6% compounding returns. Doing voluntary self-contribution (Caruman Sukarela) on the i-Akaun app up to RM100k a year directly increases your principal. Tell me: how much of your current monthly budget would you be comfortable contributing, and do you intend to use it for an emergency buffer or retirement?"
                }
                else -> {
                    "That is an excellent goal to prioritize. Managing your budget is key to achieving this financial freedom in Malaysia. To make this actionable, let's explore if you can use liquid cash generators like Touch 'n Go Go+ or KDI Save for emergency funds to support this goal. Tell me, how much monthly income do you generate, and how much can you comfortably set aside each month for this specific goal?"
                }
            }
        }

        // For subsequent turns from user
        return when {
            lastUserMsg.contains("rm") || lastUserMsg.matches(Regex(".*\\d+.*")) -> {
                "That budget range is very standard and workable! For instance, if you earn RM3,500, a take-home net salary of RM3,115 (after EPF and SOCSO cuts) leaves you with about RM500 for savings. If we park your savings in high-yield liquid products like Touch 'n Go Go+ (earning around 3.4% p.a.) or ASB, we build consistency. Can you share if you have any high-interest debts like credit cards or PTPTN loans that we should clear first?"
            }
            lastUserMsg.contains("ptptn") || lastUserMsg.contains("card") || lastUserMsg.contains("debt") || lastUserMsg.contains("loan") -> {
                "Clearing debt first is extremely smart. PTPTN is lowest impact at only 1% interest, whereas credit cards charge a heavy 15% to 18% p.a. compound interest. I advise paying off credit cards first using the avalanche method while keeping a small RM1,000 emergency fund in Go+. Do you have any credit card debt outstanding, or is it mostly low-interest PTPTN?"
            }
            lastUserMsg.contains("asb") || lastUserMsg.contains("asm") || lastUserMsg.contains("invest") || lastUserMsg.contains("save") -> {
                "Excellent! Putting those savings in ASB or ASM is incredibly smart because they pay compound dividends annually. If you deposit RM400 monthly in ASB, in 5 years you will have saved over RM24,000, with around RM3,000 purely from dividends! Should we automate your ASNB deposits every month directly on the myASNB app, or do you prefer to invest manually during salary day?"
            }
            lastUserMsg.contains("downpayment") || lastUserMsg.contains("epf") || lastUserMsg.contains("kwsp") || lastUserMsg.contains("house") -> {
                "I see! Utilizing EPF Akaun 2 or Akaun 3 for downpayment withdrawals is very common and can jumpstart your ownership without wiping out your cash savings. For the remainder, we can combine with SJKP. To progress, would you prefer an Islamic home financing structure (with a fixed profit rate ceilings) or a standard conventional floating-rate mortgage?"
            }
            else -> {
                "You've clearly given this a lot of thought! Setting up structured monthly automations removes the mental friction of saving. To fine-tune our plan, what do you think is your biggest daily hurdle—is it controlling discretionary impulse online-shopping spending, or simply finding a higher-paying job?"
            }
        }
    }

    /**
     * Unified, high-intelligence response generator for Voice Conversations.
     * Instructs Gemini to speak naturally as the role-played mentor (e.g., Uncle Raju, Sarah Chen, Encik Harris).
     * Avoids markdown formatting so that Text-To-Speech sounds perfectly natural.
     */
    suspend fun generateMentorVoiceResponse(
        chatHistory: List<Map<String, Any>>,
        scenarioName: String,
        mentorName: String,
        financialFocus: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER", ignoreCase = true)) {
            Log.w(TAG, "API Key is missing. Using local voice fallback.")
            return@withContext simulateVoiceReply(chatHistory, mentorName, financialFocus)
        }

        val historyStr = StringBuilder()
        for (msg in chatHistory) {
            val sender = if (msg["sender"] == "user") {
                val hasResult = msg["result"] != null
                if (hasResult) "Student" else "Student"
            } else {
                "Mentor ($mentorName)"
            }
            val text = msg["text"]?.toString() ?: ""
            historyStr.append("$sender: $text\n\n")
        }

        val prompt = """
            You are $mentorName, the user's business mentor or financial advisor role-playing the scenario "$scenarioName".
            The student is currently on a voice call with you.
            Your financial focus in this conversation is: "$financialFocus".
            
            Here is the conversation history:
            $historyStr
            
            As $mentorName, respond to the student's last message.
            CRITICAL RULES FOR VOICE CONVERSATIONS:
            1. Keep your output short, engaging, and professional (1 to 3 sentences max).
            2. Speak in warm, conversational, natural English. You can use mild local references (like KL, Ringgit coins, local business culture) to make it highly authentic.
            3. STRICTLY AVOID any markdown syntax, including asterisks (**), italics (*), bullet lists (-), or brackets. The response will be read aloud by an Android Text-To-Speech engine, so write it exactly as it should be spoken! No weird punctuation characters.
            4. End your turn by giving feedback or asking exactly ONE clear business or financial question to keep the voice call interactive.
            
            Respond only with your conversational verbal response. Do not add any intros or metadata.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error: ${response.code}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty body")
                val rootJson = JSONObject(bodyString)
                val reply = rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                return@withContext reply.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "API call failed for voice response, using simulator fallback", e)
            return@withContext simulateVoiceReply(chatHistory, mentorName, financialFocus)
        }
    }

    private fun simulateVoiceReply(
        chatHistory: List<Map<String, Any>>,
        mentorName: String,
        financialFocus: String
    ): String {
        val userMsgs = chatHistory.filter { it["sender"] == "user" }
        val lastText = userMsgs.lastOrNull()?.get("text")?.toString()?.lowercase() ?: ""

        if (mentorName.contains("Harris", ignoreCase = true)) {
            if (userMsgs.isEmpty()) {
                return "Hello! I am Encik Harris. Let's check how we can reach your target financial goal. What is your current monthly salary and target savings amount?"
            }
            return when {
                lastText.contains("save") || lastText.contains("rm") || lastText.contains("salary") -> {
                    "That is a very healthy monthly goal. If we accumulate that regularly in safe high-yielding vehicles like Touch n Go Go Plus or ASB, we can reach it very quickly. Do you currently have any high-interest debts, or is your budget free to invest?"
                }
                lastText.contains("debt") || lastText.contains("ptptn") || lastText.contains("card") -> {
                    "Understood. Financial security starts with clearing compound debts, starting with highest interest cards. Renting in prime spots like Bangsar can wait. Do you have a fixed monthly installment target in mind?"
                }
                else -> {
                    "That sounds very promising! Keeping a close eye on your monthly cash flow is the secret to compound wealth. What do you see as your biggest challenge in keeping consistent savings every month?"
                }
            }
        } else if (mentorName.contains("Raju", ignoreCase = true)) {
            if (userMsgs.isEmpty()) {
                return "Welcome to CIMB. I am Uncle Raju. Tell me about your business idea and how you plan to manage this finance loan."
            }
            return when {
                lastText.contains("collateral") || lastText.contains("rm") || lastText.contains("debt") || lastText.contains("profit") -> {
                    "A high debt service cover ratio shows your business has enough liquidity to pay installments. Your expansion logic looks very solid! What is your estimated monthly revenue and margin from this new outlet?"
                }
                else -> {
                    "Yes, expanding looks attractive, but managing the interest rate fluctuations is crucial. Can you specify your monthly repayment budget limits?"
                }
            }
        } else if (mentorName.contains("Sarah", ignoreCase = true) || mentorName.contains("Chen", ignoreCase = true)) {
            if (userMsgs.isEmpty()) {
                return "Hello, I am Sarah Chen. Thanks for calling. Let's discuss your salary expectations and benefits views."
            }
            return when {
                lastText.contains("gross") || lastText.contains("epf") || lastText.contains("salary") || lastText.contains("net") -> {
                    "A high gross pay with strong compulsory EPF contributions guarantees robust financial retirement security. I think your skill explanation is excellent. When would you be ready to join our KL tech team?"
                }
                else -> {
                    "A net pay after EPF and SOSCO cuts must meet your living standards in expensive centers like Kuala Lumpur. What are your key considerations for this job?"
                }
            }
        } else {
            if (userMsgs.isEmpty()) {
                return "Aiyoo, I am Uncle Samy. Let's discuss your cash inventory terms."
            }
            return when {
                lastText.contains("credit") || lastText.contains("cash") || lastText.contains("terms") -> {
                    "Okay, a Net-30 credit term makes your cash flow safe but increases my liability risk! Can you do a shorter term first, or pay some upfront?"
                }
                else -> {
                    "Negotiating credit in wholesale operations requires strong liquid trust. Let's start with a smaller stock batch. Is that agreeable?"
                }
            }
        }
    }
}
