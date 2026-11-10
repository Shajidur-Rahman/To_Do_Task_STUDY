package com.chronos.hsc.data.service

import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.store.AiRouletteRecommendation
import com.chronos.hsc.data.store.CognitiveLoadCategory
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.data.store.LastSpunRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object GeminiRouletteService {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a")

    fun getCognitiveCategory(slice: SliceWithParentChain): CognitiveLoadCategory {
        val customLink = CustomSubjectAndAiStore.getCustomSubjectForChain(slice.chain.chainId)
        if (customLink != null) {
            return customLink.cognitiveCategory
        }
        return when (slice.chain.subject) {
            HscSubject.HIGHER_MATH -> CognitiveLoadCategory.HEAVY_ANALYTICAL
            HscSubject.PHYSICS -> {
                if (slice.slice.phaseTag.contains("Theory", ignoreCase = true) ||
                    slice.slice.phaseTag.contains("Lecture", ignoreCase = true)
                ) {
                    CognitiveLoadCategory.BALANCED_CONCEPTUAL
                } else {
                    CognitiveLoadCategory.HEAVY_ANALYTICAL
                }
            }
            HscSubject.CHEMISTRY -> {
                if (slice.slice.phaseTag.contains("Theory", ignoreCase = true) ||
                    slice.slice.phaseTag.contains("Lecture", ignoreCase = true)
                ) {
                    CognitiveLoadCategory.BALANCED_CONCEPTUAL
                } else {
                    CognitiveLoadCategory.HEAVY_ANALYTICAL
                }
            }
            HscSubject.BIOLOGY -> CognitiveLoadCategory.SOFT_MEMORIZATION
            HscSubject.BANGLA -> CognitiveLoadCategory.SOFT_MEMORIZATION
            HscSubject.ENGLISH -> CognitiveLoadCategory.SOFT_MEMORIZATION
            HscSubject.ICT -> {
                if (slice.slice.phaseTag.contains("Programming", ignoreCase = true) ||
                    slice.slice.phaseTag.contains("Question", ignoreCase = true)
                ) {
                    CognitiveLoadCategory.BALANCED_CONCEPTUAL
                } else {
                    CognitiveLoadCategory.SOFT_MEMORIZATION
                }
            }
        }
    }

    suspend fun analyzeAndSpinLiveRoulette(
        candidateSlices: List<SliceWithParentChain>,
        allTodayCompletedSlices: List<SliceWithParentChain> = emptyList(),
        batteryPercent: Int = 100,
        batteryModeLabel: String = "HIGH_FOCUS",
        pinnedExam: ExamCountdownEntity? = null,
        weeklySlicesByDay: Map<Long, List<SliceWithParentChain>> = emptyMap()
    ): AiRouletteRecommendation = withContext(Dispatchers.IO) {
        if (candidateSlices.isEmpty()) {
            return@withContext AiRouletteRecommendation(
                selectedSliceId = "",
                cognitiveTransitionTag = "NO PENDING TASKS",
                behaviorAnalysisNote = "All tasks on this date are already completed!",
                whyThisTaskNow = "You have conquered all study slices for this day.",
                nextRecommendedAfterThis = "Take a well-deserved rest or review upcoming Ebbinghaus Echoes.",
                isOfflineFallback = true
            )
        }

        val apiKey = CustomSubjectAndAiStore.getApiKey().trim()
        val preferredModel = CustomSubjectAndAiStore.getSelectedModel().trim()
        val rejectedSlices = CustomSubjectAndAiStore.getRejectedSlicesSession()
        val lastSpun = CustomSubjectAndAiStore.getLastSpunRecord()

        // 1. Behavioral Override Analysis
        val mostRecentCompleted = allTodayCompletedSlices
            .filter { it.slice.isCompleted }
            .maxByOrNull { it.slice.completedAtTimestamp ?: 0L }

        val behavioralNoteStringBuilder = StringBuilder()
        var isUserOverrideDetected = false

        if (mostRecentCompleted != null && lastSpun != null) {
            val completedTime = mostRecentCompleted.slice.completedAtTimestamp ?: 0L
            val minsAgo = if (completedTime > 0) {
                ((System.currentTimeMillis() - completedTime) / 60000L).coerceAtLeast(0)
            } else 0

            val recentCategory = getCognitiveCategory(mostRecentCompleted)

            if (lastSpun.sliceId != mostRecentCompleted.slice.sliceId && completedTime >= (lastSpun.timestamp - 60000L)) {
                isUserOverrideDetected = true
                behavioralNoteStringBuilder.append(
                    "USER OVERRIDE DETECTED: You previously spun [${lastSpun.title}] (${lastSpun.cognitiveCategory.label}), " +
                    "but chose to complete [${mostRecentCompleted.slice.displayTitle}] (${recentCategory.label}) $minsAgo mins ago instead. "
                )
            } else if (lastSpun.sliceId == mostRecentCompleted.slice.sliceId) {
                behavioralNoteStringBuilder.append(
                    "DIRECT FOLLOW-THROUGH: You spun and successfully completed [${mostRecentCompleted.slice.displayTitle}] (${recentCategory.label}) $minsAgo mins ago! "
                )
            }
        }

        // 2. Build Candidate JSON for Gemini
        val candidateArray = JSONArray()
        for (candidate in candidateSlices) {
            val cat = getCognitiveCategory(candidate)
            val custom = CustomSubjectAndAiStore.getCustomSubjectForChain(candidate.chain.chainId)
            val subName = custom?.customSubjectName ?: candidate.chain.subject.fullName
            candidateArray.put(
                JSONObject().apply {
                    put("sliceId", candidate.slice.sliceId)
                    put("shortBadge", candidate.slice.shortBadge)
                    put("displayTitle", candidate.slice.displayTitle)
                    put("subject", subName)
                    put("cognitiveWeight", cat.name)
                    put("cognitiveLabel", cat.label)
                    put("phaseTag", candidate.slice.phaseTag)
                    put("problemsProgress", "${candidate.slice.problemsCompleted}/${candidate.slice.problemsTarget}")
                    put("effectiveLagDays", candidate.slice.effectiveLagDays)
                    put("isLagging", candidate.slice.isLagging)
                    put("isRejectedInSession", rejectedSlices.contains(candidate.slice.sliceId))
                }
            )
        }

        // 3. Build Completed Today JSON
        val completedArray = JSONArray()
        for (completed in allTodayCompletedSlices.take(8)) {
            val cat = getCognitiveCategory(completed)
            val custom = CustomSubjectAndAiStore.getCustomSubjectForChain(completed.chain.chainId)
            val subName = custom?.customSubjectName ?: completed.chain.subject.fullName
            val timeStr = completed.slice.completedAtTimestamp?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FORMATTER)
            } ?: "Today"
            completedArray.put(
                JSONObject().apply {
                    put("displayTitle", completed.slice.displayTitle)
                    put("subject", subName)
                    put("cognitiveWeight", cat.name)
                    put("completedAtTime", timeStr)
                }
            )
        }

        // 4. Construct Prompt
        val prompt = """
            You are the Chronos HSC AI Cognitive Roulette Engine — an elite neuro-productivity study engine designed for HSC exam candidates in Bangladesh.
            Your job is to analyze the student's live study telemetry and select the single optimal task slice from candidateSlices to beat procrastination and balance brain fatigue.

            CURRENT CONTEXT:
            - Current Local Time: ${LocalTime.now().format(TIME_FORMATTER)}
            - Mental Battery Mode: $batteryModeLabel ($batteryPercent%)
            - Urgency: ${pinnedExam?.let { val daysLeft = (it.targetDateEpochDay - LocalDate.now().toEpochDay()).coerceAtLeast(0); "$daysLeft Days Left for ${it.examTitle}" } ?: "No specific countdown"}
            - Behavioral Log Note: ${if (behavioralNoteStringBuilder.isNotEmpty()) behavioralNoteStringBuilder.toString() else "Starting a fresh study session."}
            - Rejected Slices This Session: ${rejectedSlices.joinToString(", ")}

            RECENTLY COMPLETED SLICES TODAY (Chronological):
            $completedArray

            CANDIDATE INCOMPLETE SLICES FOR TODAY:
            $candidateArray

            NEURO-PSYCHOLOGY RULES:
            1. COGNITIVE ALTERNATION: If the student just finished a HEAVY_ANALYTICAL calculation block (Math, Physics, Chem math), DO NOT immediately stack another heavy math task unless their battery is 100% and urgency demands it. Reward their prefrontal cortex by alternating to SOFT_MEMORIZATION (Bangla, English, ICT, Biology) or BALANCED_CONCEPTUAL.
            2. USER OVERRIDE AWARENESS: If the student resisted a previously spun task and completed something else instead, acknowledge their self-regulation! Bridge them with moderate momentum or challenge them now that their focus has warmed up.
            3. AVOID REJECTED: Do NOT select any slice that has isRejectedInSession == true if other valid candidate slices exist.
            4. ZOMBIE MODE (15%): If battery is 15% Zombie, strictly prioritize SOFT_MEMORIZATION or Theory/Light Review slices.
            5. LAG PRIORITY: Prioritize slices with effectiveLagDays > 0 to clear study backlog.

            STRICT OUTPUT FORMAT:
            You must return a valid JSON object only with NO markdown fences, matching this schema:
            {
              "selectedSliceId": "exact sliceId from candidateSlices",
              "cognitiveTransitionTag": "e.g. SOFT (Bangla) ➔ HEAVY (Chemistry) | Brain Channel Switch",
              "behaviorAnalysisNote": "Brief 1-sentence observation of what they just did or skipped",
              "whyThisTaskNow": "1-2 punchy sentences explaining why this exact task is optimal for their brain right now",
              "nextRecommendedAfterThis": "Brief teaser of what to tackle after this 25m sprint"
            }
        """.trimIndent()

        // 5. Query Gemini with model fallback chain
        val modelsToTry = listOf(preferredModel, CustomSubjectAndAiStore.MODEL_GEMINI_3_5_FLASH, CustomSubjectAndAiStore.MODEL_GEMINI_3_8_FLASH)
            .distinct()

        for (model in modelsToTry) {
            val result = queryGeminiModel(model, apiKey, prompt, candidateSlices)
            if (result != null) {
                CustomSubjectAndAiStore.saveLastRecommendation(result)
                return@withContext result
            }
        }

        // 6. Local Heuristic Fallback
        val fallback = runSmartCognitiveAlternatingHeuristic(
            candidateSlices = candidateSlices,
            allTodayCompletedSlices = allTodayCompletedSlices,
            batteryPercent = batteryPercent,
            rejectedSlices = rejectedSlices,
            overrideNote = behavioralNoteStringBuilder.toString()
        )
        CustomSubjectAndAiStore.saveLastRecommendation(fallback)
        fallback
    }

    private fun queryGeminiModel(
        model: String,
        apiKey: String,
        prompt: String,
        candidateSlices: List<SliceWithParentChain>
    ): AiRouletteRecommendation? {
        if (apiKey.isBlank()) return null
        var connection: HttpURLConnection? = null
        return try {
            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(endpointUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 14000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
                setRequestProperty("User-Agent", "ChronosHSC-AI-Engine")
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(
                        JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            }
                            put("parts", parts)
                        }
                    )
                }
                put("contents", contents)
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.35)
                    }
                )
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                parseGeminiResponse(responseText, candidateSlices)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseGeminiResponse(
        jsonString: String,
        candidateSlices: List<SliceWithParentChain>
    ): AiRouletteRecommendation? {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.getJSONArray("candidates")
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() == 0) return null
            val rawText = parts.getJSONObject(0).getString("text")

            val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(cleanJson)

            val sliceId = parsed.getString("selectedSliceId")
            val matchingCandidate = candidateSlices.find { it.slice.sliceId == sliceId } ?: candidateSlices.first()

            AiRouletteRecommendation(
                selectedSliceId = matchingCandidate.slice.sliceId,
                cognitiveTransitionTag = parsed.optString("cognitiveTransitionTag", "🧠 AI COGNITIVE BALANCE LOCK"),
                behaviorAnalysisNote = parsed.optString("behaviorAnalysisNote", "Telemetry analyzed your recent study pattern."),
                whyThisTaskNow = parsed.optString("whyThisTaskNow", "Selected to maintain rhythm and avoid cognitive fatigue."),
                nextRecommendedAfterThis = parsed.optString("nextRecommendedAfterThis", "Ready to cascade to your next syllabus slice."),
                isOfflineFallback = false
            )
        } catch (_: Exception) {
            null
        }
    }

    fun runSmartCognitiveAlternatingHeuristic(
        candidateSlices: List<SliceWithParentChain>,
        allTodayCompletedSlices: List<SliceWithParentChain>,
        batteryPercent: Int,
        rejectedSlices: Set<String>,
        overrideNote: String
    ): AiRouletteRecommendation {
        val nonRejected = candidateSlices.filter { !rejectedSlices.contains(it.slice.sliceId) }
        val pool = if (nonRejected.isNotEmpty()) nonRejected else candidateSlices

        val mostRecentCompleted = allTodayCompletedSlices
            .filter { it.slice.isCompleted }
            .maxByOrNull { it.slice.completedAtTimestamp ?: 0L }

        val lastCat = mostRecentCompleted?.let { getCognitiveCategory(it) }

        // Determine target cognitive category
        val targetCategory = when {
            batteryPercent <= 20 -> CognitiveLoadCategory.SOFT_MEMORIZATION
            lastCat == CognitiveLoadCategory.HEAVY_ANALYTICAL -> CognitiveLoadCategory.SOFT_MEMORIZATION
            lastCat == CognitiveLoadCategory.SOFT_MEMORIZATION -> CognitiveLoadCategory.HEAVY_ANALYTICAL
            else -> CognitiveLoadCategory.BALANCED_CONCEPTUAL
        }

        // Rank candidates: matching targetCategory first, then lagging slices, then highest XP
        val bestCandidate = pool.filter { getCognitiveCategory(it) == targetCategory }
            .maxByOrNull { it.slice.effectiveLagDays * 10 + it.slice.xpMultiplier }
            ?: pool.maxByOrNull { it.slice.effectiveLagDays * 10 + it.slice.xpMultiplier }
            ?: pool.first()

        val bestCat = getCognitiveCategory(bestCandidate)
        val fromLabel = lastCat?.name ?: "START"
        val toLabel = bestCat.name
        val custom = CustomSubjectAndAiStore.getCustomSubjectForChain(bestCandidate.chain.chainId)
        val subName = custom?.customSubjectName ?: bestCandidate.chain.subject.fullName

        val behaviorText = if (overrideNote.isNotBlank()) {
            overrideNote.trim()
        } else if (mostRecentCompleted != null) {
            "Noticed you finished [${mostRecentCompleted.slice.displayTitle}]. Alternating cognitive channels."
        } else {
            "Starting fresh session. Selecting optimal cognitive load for $batteryPercent% battery."
        }

        return AiRouletteRecommendation(
            selectedSliceId = bestCandidate.slice.sliceId,
            cognitiveTransitionTag = "⚡ $fromLabel ➔ $toLabel | Brain Channel Switch",
            behaviorAnalysisNote = behaviorText,
            whyThisTaskNow = "Targeted $subName (${bestCat.label}) to keep your neural momentum high while letting fatigued circuits recover.",
            nextRecommendedAfterThis = "After this 25m sprint, we will rotate to a contrasting conceptual block.",
            isOfflineFallback = true
        )
    }
}
