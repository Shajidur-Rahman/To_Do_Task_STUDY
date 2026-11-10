package com.chronos.hsc.data.service

import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.store.AiRouletteRecommendation
import com.chronos.hsc.data.store.BatchExamAiEvaluation
import com.chronos.hsc.data.store.BatchExamRecord
import com.chronos.hsc.data.store.BatchExamStore
import com.chronos.hsc.data.store.CognitiveLoadCategory
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.data.store.ExamFormatMode
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
import java.util.Locale
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

    /**
     * Evaluates the HSC syllabus difficulty, required study hours, and strategy for an upcoming
     * private batch or coaching exam using Gemini 3.8 Flash (with instant offline fallback).
     */
    suspend fun evaluatePrivateExamDifficulty(
        subjectName: String,
        paperName: String,
        chapterNumbersAndTitles: String,
        examFormatMode: ExamFormatMode,
        mcqCount: Int,
        cqCount: Int,
        daysLeft: Long
    ): BatchExamAiEvaluation = withContext(Dispatchers.IO) {
        val apiKey = CustomSubjectAndAiStore.getApiKey().trim()
        val preferredModel = CustomSubjectAndAiStore.getSelectedModel().trim()

        val prompt = """
            You are the Chronos HSC AI Exam Analysis Engine — specialized in Bangladesh HSC syllabus, coaching batch exams, and cognitive study load balancing.
            Evaluate the difficulty, required study preparation time, importance, and tactical strategy for this upcoming private / coaching exam:
            - Subject: $subjectName
            - Paper: $paperName
            - Chapters: $chapterNumbersAndTitles
            - Exam Format: ${examFormatMode.label}
            - Total MCQs: $mcqCount
            - Total CQs: $cqCount
            - Days Left Until Exam: $daysLeft

            STRICT REQUIREMENTS:
            1. difficultyScore: Integer 1 to 10 (e.g., Physics/Higher Math analytical chapters with high CQs = 8-10, Biology/Bangla memorization = 4-6).
            2. difficultyLabel: Punchy label with emoji (e.g. "🔥 Heavy Analytical & Formula Dense (9/10)" or "📖 Conceptual & Memory Intensive (6/10)").
            3. estimatedTotalStudyHours: Realistic total hours (Float) needed to master these exact chapters for this MCQ/CQ count (e.g. 6.5).
            4. importanceScore: Integer 1 to 100 based on question volume, chapter depth, and countdown urgency.
            5. prepStrategyAdvice: 1-2 practical, high-impact tactical advice sentences tailored to the exact MCQ vs CQ count (e.g. speed drills, calculator shortcuts, or C/D stem derivation practice).

            OUTPUT FORMAT: Return a valid JSON object only with NO markdown fences:
            {
              "difficultyScore": 9,
              "difficultyLabel": "🔥 Heavy Analytical & Formula Dense (9/10)",
              "estimatedTotalStudyHours": 6.5,
              "importanceScore": 94,
              "prepStrategyAdvice": "For $mcqCount MCQs in $chapterNumbersAndTitles, focus 60% on formula ratios & calculator tricks, and allocate remaining time to $cqCount CQ stem variations."
            }
        """.trimIndent()

        val modelsToTry = listOf(
            preferredModel,
            CustomSubjectAndAiStore.MODEL_GEMINI_3_8_FLASH,
            CustomSubjectAndAiStore.MODEL_GEMINI_3_5_FLASH
        ).distinct()

        for (model in modelsToTry) {
            val result = queryGeminiExamEvaluation(model, apiKey, prompt)
            if (result != null) {
                return@withContext result
            }
        }

        // Offline Heuristic Fallback
        computeLocalExamEvaluation(subjectName, paperName, chapterNumbersAndTitles, examFormatMode, mcqCount, cqCount, daysLeft)
    }

    private fun queryGeminiExamEvaluation(
        model: String,
        apiKey: String,
        prompt: String
    ): BatchExamAiEvaluation? {
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
                        put("temperature", 0.3)
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
                parseExamEvaluationResponse(responseText)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseExamEvaluationResponse(jsonString: String): BatchExamAiEvaluation? {
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

            BatchExamAiEvaluation(
                difficultyScore = parsed.optInt("difficultyScore", 6).coerceIn(1, 10),
                difficultyLabel = parsed.optString("difficultyLabel", "🔥 Analytical Load (6/10)"),
                estimatedTotalStudyHours = parsed.optDouble("estimatedTotalStudyHours", 4.5).toFloat().coerceIn(1.0f, 30.0f),
                importanceScore = parsed.optInt("importanceScore", 70).coerceIn(1, 100),
                prepStrategyAdvice = parsed.optString("prepStrategyAdvice", "Prioritize key formula drills and solve past board question stems.")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun computeLocalExamEvaluation(
        subjectName: String,
        paperName: String,
        chapterNumbersAndTitles: String,
        examFormatMode: ExamFormatMode,
        mcqCount: Int,
        cqCount: Int,
        daysLeft: Long
    ): BatchExamAiEvaluation {
        val isHeavy = subjectName.contains("Physics", ignoreCase = true) ||
                subjectName.contains("Math", ignoreCase = true) ||
                subjectName.contains("Chemistry", ignoreCase = true)
        val isSoft = subjectName.contains("Bangla", ignoreCase = true) ||
                subjectName.contains("English", ignoreCase = true) ||
                subjectName.contains("Biology", ignoreCase = true)

        val baseDiff = if (isHeavy) 8 else if (isSoft) 5 else 6
        val diffScore = (baseDiff + (if (cqCount >= 3) 1 else 0)).coerceIn(1, 10)
        val diffLabel = when {
            diffScore >= 8 -> "🔥 Heavy Analytical & Formula Dense ($diffScore/10)"
            diffScore >= 6 -> "⚖️ Balanced Analytical & Conceptual ($diffScore/10)"
            else -> "📖 Conceptual & Memory Intensive ($diffScore/10)"
        }

        val mcqHours = mcqCount * 0.05f
        val cqHours = cqCount * 0.75f
        val baseHours = if (isHeavy) 2.5f else 1.5f
        val rawHours = baseHours + mcqHours + cqHours
        val estimatedHours = (Math.round(rawHours * 10f) / 10f).coerceIn(1.5f, 25.0f)

        val urgencyBonus = if (daysLeft <= 1) 25 else if (daysLeft <= 3) 15 else 5
        val importance = (40 + (mcqCount * 0.4f) + (cqCount * 6f) + urgencyBonus).toInt().coerceIn(10, 100)

        val advice = when (examFormatMode) {
            ExamFormatMode.MCQ_ONLY -> "Master formula shortcuts and time yourself at ~45s per MCQ. Focus on Board & Top College question patterns."
            ExamFormatMode.CQ_ONLY -> "Practice writing full (c) Application & (d) Higher Ability steps cleanly. Emphasize standard chapter derivations."
            ExamFormatMode.MCQ_AND_CQ_BOTH -> "Split study time: 50% on speed drills for the $mcqCount MCQs and 50% writing out $cqCount CQ problem stems."
        }

        return BatchExamAiEvaluation(
            difficultyScore = diffScore,
            difficultyLabel = diffLabel,
            estimatedTotalStudyHours = estimatedHours,
            importanceScore = importance,
            prepStrategyAdvice = advice
        )
    }

    /**
     * Live Gemini 3.8 Flash Task Roulette with Coaching Batch Exam Priority Injection
     */
    suspend fun analyzeAndSpinLiveRoulette(
        candidateSlices: List<SliceWithParentChain>,
        allTodayCompletedSlices: List<SliceWithParentChain> = emptyList(),
        batteryPercent: Int = 100,
        batteryModeLabel: String = "HIGH_FOCUS",
        pinnedExam: ExamCountdownEntity? = null,
        weeklySlicesByDay: Map<Long, List<SliceWithParentChain>> = emptyMap(),
        upcomingBatchExams: List<BatchExamRecord> = BatchExamStore.getActiveUpcomingExams()
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

        if (mostRecentCompleted != null && lastSpun != null) {
            val completedTime = mostRecentCompleted.slice.completedAtTimestamp ?: 0L
            val minsAgo = if (completedTime > 0) {
                ((System.currentTimeMillis() - completedTime) / 60000L).coerceAtLeast(0)
            } else 0

            val recentCategory = getCognitiveCategory(mostRecentCompleted)

            if (lastSpun.sliceId != mostRecentCompleted.slice.sliceId && completedTime >= (lastSpun.timestamp - 60000L)) {
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

        // 2. Build Upcoming Private Exams Array for Context Injection
        val upcomingExamsArray = JSONArray()
        val todayEpoch = LocalDate.now().toEpochDay()
        for (exam in upcomingBatchExams.take(6)) {
            val matchingCandidates = candidateSlices.filter { exam.matchesSlice(it) }
            upcomingExamsArray.put(
                JSONObject().apply {
                    put("examId", exam.examId)
                    put("coachingName", exam.batchOrCoachingName)
                    put("subjectName", exam.subjectName)
                    put("paperName", exam.paperName)
                    put("chapters", exam.chapterNumbersAndTitles)
                    put("daysLeft", exam.daysLeft(todayEpoch))
                    put("examFormat", exam.examFormatMode.name)
                    put("mcqCount", exam.mcqCount)
                    put("cqCount", exam.cqCount)
                    put("aiDifficultyScore", exam.aiDifficultyScore)
                    put("aiEstimatedTotalStudyHours", exam.aiEstimatedTotalStudyHours.toDouble())
                    put("studyHoursLogged", exam.studyHoursLogged.toDouble())
                    put("remainingHoursNeeded", exam.remainingStudyHours.toDouble())
                    put("matchingCandidateSliceIds", JSONArray(matchingCandidates.map { it.slice.sliceId }))
                }
            )
        }

        // 3. Build Candidate JSON for Gemini
        val candidateArray = JSONArray()
        for (candidate in candidateSlices) {
            val cat = getCognitiveCategory(candidate)
            val custom = CustomSubjectAndAiStore.getCustomSubjectForChain(candidate.chain.chainId)
            val subName = custom?.customSubjectName ?: candidate.chain.subject.fullName
            val matchingExam = upcomingBatchExams.firstOrNull { it.matchesSlice(candidate) }

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
                    if (matchingExam != null) {
                        put("hasUpcomingExam", true)
                        put("examDaysLeft", matchingExam.daysLeft(todayEpoch))
                        put("examFormat", matchingExam.formatSummary)
                        put("examRemainingHours", matchingExam.remainingStudyHours)
                    } else {
                        put("hasUpcomingExam", false)
                    }
                }
            )
        }

        // 4. Build Completed Today JSON
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

        // 5. Construct Prompt with Strict Private Exam Priority
        val prompt = """
            You are the Chronos HSC AI Cognitive Roulette Engine — an elite neuro-productivity study engine designed for HSC exam candidates in Bangladesh.
            Your job is to analyze the student's live study telemetry and select the single optimal task slice from candidateSlices to beat procrastination and balance brain fatigue.

            CRITICAL COACHING EXAM RULE (HIGHEST PRIORITY):
            The student attends private batches that take frequent MCQ/CQ exams. Check `upcomingPrivateExams` FIRST.
            If any candidate slice on Today's board matches an upcoming Private/Batch Exam (especially within 1 to 5 days where `studyHoursLogged < aiEstimatedTotalStudyHours`), you MUST heavily prioritize selecting that exam's slice FIRST so the student never runs out of preparation time before exam day!
            - SMART INTERLEAVING EXCEPTION: Only switch away from the urgent exam subject if the student JUST completed a heavy slice of that exact exam subject within the last 10 minutes and needs a 25-minute soft cognitive breather before returning to the exam chapter — and even then, explicitly remind them in `nextRecommendedAfterThis` to return immediately to their upcoming exam chapter!
            - TAILOR THE ADVICE TO MCQ vs CQ: In `whyThisTaskNow`, explicitly mention their upcoming private exam's question count (e.g., 'Your Physics Private Exam has 50 MCQs + 2 CQs in 2 days and needs 4.5 more hours of prep — focus on calculator shortcuts during this 25m sprint!').

            UPCOMING PRIVATE / COACHING BATCH EXAMS:
            $upcomingExamsArray

            CURRENT CONTEXT:
            - Current Local Time: ${LocalTime.now().format(TIME_FORMATTER)}
            - Mental Battery Mode: $batteryModeLabel ($batteryPercent%)
            - Major Exam Urgency: ${pinnedExam?.let { val daysLeft = (it.targetDateEpochDay - LocalDate.now().toEpochDay()).coerceAtLeast(0); "$daysLeft Days Left for ${it.examTitle}" } ?: "No major exam countdown"}
            - Behavioral Log Note: ${if (behavioralNoteStringBuilder.isNotEmpty()) behavioralNoteStringBuilder.toString() else "Starting a fresh study session."}
            - Rejected Slices This Session: ${rejectedSlices.joinToString(", ")}

            RECENTLY COMPLETED SLICES TODAY (Chronological):
            $completedArray

            CANDIDATE INCOMPLETE SLICES FOR TODAY:
            $candidateArray

            GENERAL NEURO-PSYCHOLOGY RULES:
            1. COGNITIVE ALTERNATION: When not constrained by an imminent exam deadline, alternate HEAVY_ANALYTICAL calculation with SOFT_MEMORIZATION or BALANCED_CONCEPTUAL.
            2. USER OVERRIDE AWARENESS: If the student resisted a previously spun task and completed something else instead, acknowledge their focus transition.
            3. AVOID REJECTED: Do NOT select any slice with isRejectedInSession == true if other valid candidate slices exist.
            4. ZOMBIE MODE (15%): If battery is 15% Zombie, prioritize SOFT_MEMORIZATION or light review unless an exam is TOMORROW.
            5. LAG PRIORITY: Prioritize slices with effectiveLagDays > 0 to clear study backlog.

            STRICT OUTPUT FORMAT:
            You must return a valid JSON object only with NO markdown fences, matching this schema:
            {
              "selectedSliceId": "exact sliceId from candidateSlices",
              "cognitiveTransitionTag": "e.g. 🎯 EXAM RADAR LOCK (50 MCQ + 2 CQ) or SOFT ➔ HEAVY",
              "behaviorAnalysisNote": "Brief 1-sentence observation of recent telemetry or exam urgency",
              "whyThisTaskNow": "1-2 punchy sentences explaining why this exact task is optimal for their exam & brain right now",
              "nextRecommendedAfterThis": "Brief teaser of what to tackle after this 25m sprint"
            }
        """.trimIndent()

        // 6. Query Gemini with model fallback chain
        val modelsToTry = listOf(
            preferredModel,
            CustomSubjectAndAiStore.MODEL_GEMINI_3_8_FLASH,
            CustomSubjectAndAiStore.MODEL_GEMINI_3_5_FLASH
        ).distinct()

        for (model in modelsToTry) {
            val result = queryGeminiModel(model, apiKey, prompt, candidateSlices)
            if (result != null) {
                CustomSubjectAndAiStore.saveLastRecommendation(result)
                return@withContext result
            }
        }

        // 7. Local Heuristic Fallback with Exam-First Priority
        val fallback = runSmartCognitiveAlternatingHeuristic(
            candidateSlices = candidateSlices,
            allTodayCompletedSlices = allTodayCompletedSlices,
            batteryPercent = batteryPercent,
            rejectedSlices = rejectedSlices,
            overrideNote = behavioralNoteStringBuilder.toString(),
            upcomingBatchExams = upcomingBatchExams
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
                cognitiveTransitionTag = parsed.optString("cognitiveTransitionTag", "🎯 AI EXAM RADAR LOCK"),
                behaviorAnalysisNote = parsed.optString("behaviorAnalysisNote", "Telemetry analyzed upcoming exams and recent study pattern."),
                whyThisTaskNow = parsed.optString("whyThisTaskNow", "Selected to maximize exam readiness and maintain cognitive rhythm."),
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
        overrideNote: String,
        upcomingBatchExams: List<BatchExamRecord> = BatchExamStore.getActiveUpcomingExams()
    ): AiRouletteRecommendation {
        val nonRejected = candidateSlices.filter { !rejectedSlices.contains(it.slice.sliceId) }
        val pool = if (nonRejected.isNotEmpty()) nonRejected else candidateSlices

        val mostRecentCompleted = allTodayCompletedSlices
            .filter { it.slice.isCompleted }
            .maxByOrNull { it.slice.completedAtTimestamp ?: 0L }

        val lastCat = mostRecentCompleted?.let { getCognitiveCategory(it) }

        // Check if any candidate slice matches an urgent upcoming batch exam (within 5 days)
        val todayEpoch = LocalDate.now().toEpochDay()
        val urgentExams = upcomingBatchExams
            .filter { it.daysLeft(todayEpoch) <= 5L && it.remainingStudyHours > 0f }
            .sortedBy { it.daysLeft(todayEpoch) }

        val examMatchingCandidate = urgentExams.firstNotNullOfOrNull { exam ->
            pool.firstOrNull { exam.matchesSlice(it) }?.let { slice -> Pair(exam, slice) }
        }

        if (examMatchingCandidate != null && batteryPercent > 15) {
            val (exam, slice) = examMatchingCandidate
            val days = exam.daysLeft(todayEpoch)
            val daysStr = if (days == 0L) "TODAY" else "in ${days}d"
            val custom = CustomSubjectAndAiStore.getCustomSubjectForChain(slice.chain.chainId)
            val subName = custom?.customSubjectName ?: slice.chain.subject.fullName

            return AiRouletteRecommendation(
                selectedSliceId = slice.slice.sliceId,
                cognitiveTransitionTag = "🎯 EXAM RADAR LOCK (${exam.formatSummary})",
                behaviorAnalysisNote = "Imminent ${exam.batchOrCoachingName} exam $daysStr. Required prep: ~${String.format(Locale.US, "%.1f", exam.remainingStudyHours)}h left.",
                whyThisTaskNow = "Selected $subName (${exam.chapterNumbersAndTitles}) — urgent exam $daysStr requires high-focus preparation for ${exam.formatSummary}!",
                nextRecommendedAfterThis = "After this 25m sprint, proceed with targeted formula or stem drills.",
                isOfflineFallback = true
            )
        }

        // Standard Cognitive Alternation
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
