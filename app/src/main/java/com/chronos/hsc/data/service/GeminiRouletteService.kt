package com.chronos.hsc.data.service

import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.store.AiRouletteRecommendation
import com.chronos.hsc.data.store.BatchExamAiEvaluation
import com.chronos.hsc.data.store.BatchExamRecord
import com.chronos.hsc.data.store.BatchExamStore
import com.chronos.hsc.data.store.BucketRotationTelemetry
import com.chronos.hsc.data.store.CognitiveLoadCategory
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.data.store.ExamFormatMode
import com.chronos.hsc.data.store.LastSpunRecord
import com.chronos.hsc.data.store.WorkloadBucket
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

    fun getSubjectName(slice: SliceWithParentChain): String {
        val custom = CustomSubjectAndAiStore.getCustomSubjectForChain(slice.chain.chainId)
        return custom?.customSubjectName ?: slice.chain.subject.fullName
    }

    fun classifySliceBucket(
        slice: SliceWithParentChain,
        todayEpochDay: Long = LocalDate.now().toEpochDay(),
        upcomingBatchExams: List<BatchExamRecord> = BatchExamStore.getActiveUpcomingExams()
    ): WorkloadBucket {
        return when {
            slice.slice.scheduledDateEpochDay < todayEpochDay || slice.slice.effectiveLagDays > 0 -> WorkloadBucket.COMEBACK_LEFTOVER
            upcomingBatchExams.any { it.matchesSlice(slice) } -> WorkloadBucket.BATCH_EXAM
            else -> WorkloadBucket.TODAY_NORMAL
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
     * Live Gemini 3.8 Flash Task Roulette with 3-Bucket Balanced Pacing & Circadian Engine
     */
    suspend fun analyzeAndSpinLiveRoulette(
        candidateSlices: List<SliceWithParentChain>,
        allTodayCompletedSlices: List<SliceWithParentChain> = emptyList(),
        batteryPercent: Int = 100,
        batteryModeLabel: String = "HIGH_FOCUS",
        pinnedExam: ExamCountdownEntity? = null,
        weeklySlicesByDay: Map<Long, List<SliceWithParentChain>> = emptyMap(),
        upcomingBatchExams: List<BatchExamRecord> = BatchExamStore.getActiveUpcomingExams(),
        userStreakDays: Int = 0,
        userLevel: Int = 1
    ): AiRouletteRecommendation = withContext(Dispatchers.IO) {
        if (candidateSlices.isEmpty()) {
            return@withContext AiRouletteRecommendation(
                selectedSliceId = "",
                cognitiveTransitionTag = "ALL SLICES CONQUERED",
                behaviorAnalysisNote = "All scheduled tasks for this window are completed!",
                whyThisTaskNow = "You have cleared every single slice in your queue.",
                nextRecommendedAfterThis = "Take a well-deserved rest or review upcoming Ebbinghaus Echoes.",
                coachStatusLevel = "HIGH_MOMENTUM",
                coachHeadline = "🏆 FLAWLESS VICTORY: Queue 100% Cleared!",
                coachHonestComment = "Incredible discipline! You have finished all your scheduled slices and prevented any backlog from carrying over into tomorrow.",
                lagConsequenceWarning = "Zero lag risk — your syllabus timeline is completely clean.",
                stepByStepSprintPlan = "• Review key formulas for 5 minutes\n• Hydrate and rest your prefrontal cortex",
                isOfflineFallback = true
            )
        }

        val apiKey = CustomSubjectAndAiStore.getApiKey().trim()
        val preferredModel = CustomSubjectAndAiStore.getSelectedModel().trim()
        val rejectedSlices = CustomSubjectAndAiStore.getRejectedSlicesSession()
        val lastSpun = CustomSubjectAndAiStore.getLastSpunRecord()
        val todayEpoch = LocalDate.now().toEpochDay()
        val bdTime = BangladeshTimeContext.now()
        val telemetry = CustomSubjectAndAiStore.getBucketRotationTelemetry(todayEpoch)
        val lastSpunSubject = lastSpun?.subjectName.orEmpty()

        // 1. Partition Candidates into 3 Unified Workload Buckets
        val comebackSlices = candidateSlices.filter { classifySliceBucket(it, todayEpoch, upcomingBatchExams) == WorkloadBucket.COMEBACK_LEFTOVER }
        val todayNormalSlices = candidateSlices.filter { classifySliceBucket(it, todayEpoch, upcomingBatchExams) == WorkloadBucket.TODAY_NORMAL }
        val batchExamSlices = candidateSlices.filter { classifySliceBucket(it, todayEpoch, upcomingBatchExams) == WorkloadBucket.BATCH_EXAM }

        val totalCompletedToday = allTodayCompletedSlices.size
        val totalRemainingCount = candidateSlices.size
        val maxLagDays = candidateSlices.maxOfOrNull { it.slice.effectiveLagDays } ?: 0

        // Cooldown & Batch Exam Cap Guards
        val hasAlternativeSubject = candidateSlices.any { !getSubjectName(it).equals(lastSpunSubject, ignoreCase = true) }
        val isSubjectOnCooldown = lastSpunSubject.isNotBlank() && hasAlternativeSubject
        val isBatchExamCapped = lastSpun?.workloadBucket == WorkloadBucket.BATCH_EXAM ||
            (telemetry.spinsBatchExam > 0 && (comebackSlices.isNotEmpty() || todayNormalSlices.isNotEmpty()) && telemetry.spinsBatchExam >= (telemetry.spinsTodayNormal + telemetry.spinsComeback))

        // 2. Behavioral Override Analysis
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

        // 3. Build Upcoming Private Exams Array
        val upcomingExamsArray = JSONArray()
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

        // 4. Build Candidate JSON for Gemini
        val candidateArray = JSONArray()
        for (candidate in candidateSlices) {
            val cat = getCognitiveCategory(candidate)
            val subName = getSubjectName(candidate)
            val bucket = classifySliceBucket(candidate, todayEpoch, upcomingBatchExams)
            val matchingExam = upcomingBatchExams.firstOrNull { it.matchesSlice(candidate) }
            val onCooldown = isSubjectOnCooldown && subName.equals(lastSpunSubject, ignoreCase = true)

            candidateArray.put(
                JSONObject().apply {
                    put("sliceId", candidate.slice.sliceId)
                    put("shortBadge", candidate.slice.shortBadge)
                    put("displayTitle", candidate.slice.displayTitle)
                    put("subject", subName)
                    put("chapterNumber", candidate.chain.chapterNumber)
                    put("chapterName", candidate.chain.chapterName)
                    put("sliceDayProgress", "Day ${candidate.slice.sliceNumber} of ${candidate.slice.totalSlicesInChain}")
                    put("workloadBucket", bucket.name)
                    put("workloadBucketEmoji", bucket.emoji)
                    put("workloadBucketLabel", bucket.label)
                    put("cognitiveWeight", cat.name)
                    put("cognitiveLabel", cat.label)
                    put("phaseTag", candidate.slice.phaseTag)
                    put("problemsProgress", "${candidate.slice.problemsCompleted}/${candidate.slice.problemsTarget}")
                    put("effectiveLagDays", candidate.slice.effectiveLagDays)
                    put("isLagging", candidate.slice.isLagging)
                    put("isRejectedInSession", rejectedSlices.contains(candidate.slice.sliceId))
                    put("isSubjectOnCooldown", onCooldown)
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

        // 5. Build Completed Today JSON
        val completedArray = JSONArray()
        for (completed in allTodayCompletedSlices.take(8)) {
            val cat = getCognitiveCategory(completed)
            val subName = getSubjectName(completed)
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

        // 6. Construct Non-Biased 3-Bucket, Circadian & Honest Coach Reality-Check Prompt
        val prompt = """
            You are the Chronos HSC Elite AI Study Coach & Cognitive Roulette Strategist — designed for HSC exam candidates in Bangladesh.
            Your job has TWO critical parts:
            PART A: Select the single best study slice from `candidateSlices` using balanced 3-bucket rotation and circadian neuroscience.
            PART B: Give the student a brutally honest, deeply motivating "HOW YOU ARE GOING" Coach Reality Check (Caution if they are lagging or procrastinating; High-Conviction Encouragement if they are making strong progress).

            🕒 BANGLADESH TIME & CIRCADIAN PROFILE:
            - Current Bangladesh Time: ${bdTime.currentBdTimeFormatted}
            - Active Circadian Phase: ${bdTime.bdCircadianPhase}

            📊 STUDENT'S LIVE PACE & BACKLOG TELEMETRY TODAY:
            - Completed Slices Today: $totalCompletedToday
            - Remaining Incomplete Slices in Pool: $totalRemainingCount
            - Overdue / Lagging Slices from Yesterday & Past Days: ${comebackSlices.size} (Max Lag: ${maxLagDays}d)
            - Today's Regular Syllabus Slices Pending: ${todayNormalSlices.size}
            - Private / Coaching Exam Slices Pending: ${batchExamSlices.size}
            - Student Level: LVL $userLevel | Current Streak: ${userStreakDays}d
            - Mental Battery Mode: $batteryModeLabel ($batteryPercent%)
            - Major Exam Countdown: ${pinnedExam?.let { val daysLeft = (it.targetDateEpochDay - todayEpoch).coerceAtLeast(0); "$daysLeft Days Left for ${it.examTitle}" } ?: "No major exam countdown"}

            📈 DAILY ROTATION TELEMETRY & ANTI-BIAS GUARDRAILS:
            - Spun Today: Comeback=${telemetry.spinsComeback}, Today Normal=${telemetry.spinsTodayNormal}, Batch Exam=${telemetry.spinsBatchExam}
            - Completed Today: Comeback=${telemetry.completedComeback}, Today Normal=${telemetry.completedTodayNormal}, Batch Exam=${telemetry.completedBatchExam}
            - Recent Subjects Studied/Spun: ${if (telemetry.recentSubjects.isEmpty()) "None yet" else telemetry.recentSubjects.joinToString(" ➔ ")}
            - Last Spun Subject: ${if (lastSpunSubject.isBlank()) "None" else lastSpunSubject} (Cooldown Active: $isSubjectOnCooldown)
            - Batch Exam Cap Status: ${if (isBatchExamCapped) "⛔ CAPPED (Batch Exam spun/completed recently. Must prioritize Comeback or Today Normal!)" else "✅ AVAILABLE"}

            CRITICAL ANTI-BIAS & BALANCED ROTATION RULES:
            1. NO EXAM TUNNEL VISION: DO NOT repeatedly pick only Batch Exam tasks. Target workload distribution: ~35% Comeback Leftovers, ~40% Today Normal Tasks, ~25% Batch Exams.
            2. BATCH EXAM CAP: If Batch Exam is capped ($isBatchExamCapped) and Comeback or Today Normal tasks exist, you MUST pick from Comeback or Today Normal!
            3. SUBJECT COOLDOWN: If a candidate has `isSubjectOnCooldown: true`, DO NOT select it unless all candidates belong to the same subject.
            4. CIRCADIAN HARMONY: Align with ${bdTime.bdCircadianPhase}.
            5. COGNITIVE ALTERNATION: Alternate HEAVY_ANALYTICAL calculation with SOFT_MEMORIZATION or BALANCED_CONCEPTUAL.
            6. AVOID REJECTED: Do NOT select any slice with `isRejectedInSession == true` if other candidates exist.

            🧠 RULES FOR PART B — COACH REALITY CHECK ("HOW YOU ARE GOING"):
            - Analyze $totalCompletedToday completed vs $totalRemainingCount remaining and ${comebackSlices.size} lagging tasks.
            - `coachStatusLevel`:
              * Set to `"CRITICAL_CAUTION"` if ${comebackSlices.size} > 0 or $maxLagDays > 0 or ($totalCompletedToday == 0 and $totalRemainingCount >= 4).
              * Set to `"HIGH_MOMENTUM"` if $totalCompletedToday >= 2 and ${comebackSlices.size} == 0.
              * Otherwise set to `"PACE_ALERT"`.
            - `coachHeadline`: A bold, unmistakable status banner (e.g. "⚠️ CAUTION: ${comebackSlices.size} Overdue Tasks — Stop the Lag Now!" or "⚡ PACE WARNING: $totalRemainingCount Slices Waiting Today" or "🔥 ELITE MOMENTUM: $totalCompletedToday Slices Slashed Today!").
            - `coachHonestComment`: 2 to 3 vivid, personal sentences speaking directly to the student ("You..."). If they have unfinished or lagging tasks, warn them clearly: tell them if they don't finish this selected chapter right now, they will lag behind their syllabus, tomorrow's tasks will stack up, and exam pressure will compound. If they are doing well, praise their discipline and push them to lock in this next chapter!
            - `lagConsequenceWarning`: 1 specific, concrete sentence stating the exact consequence of skipping or delaying this selected task today (mention the exact subject/chapter and how many tasks will pile up tomorrow).
            - `stepByStepSprintPlan`: A concrete 3-bullet 25-minute micro-sprint execution plan for this specific subject, chapter, and phaseTag (separated by `\n`).

            UPCOMING PRIVATE / COACHING BATCH EXAMS:
            $upcomingExamsArray

            RECENTLY COMPLETED SLICES TODAY (Chronological):
            $completedArray

            CANDIDATE INCOMPLETE SLICES:
            $candidateArray

            STRICT OUTPUT FORMAT:
            Return a valid JSON object only with NO markdown fences, matching this exact schema:
            {
              "selectedSliceId": "exact sliceId from candidateSlices",
              "cognitiveTransitionTag": "e.g. 🔥 COMEBACK HERO | Rescue Missed Slice",
              "behaviorAnalysisNote": "1-sentence observation connecting Bangladesh time (${bdTime.currentBdTimeFormatted}) and workload balance",
              "whyThisTaskNow": "1-2 punchy sentences explaining why this exact chapter & subject is optimal right now",
              "nextRecommendedAfterThis": "What contrasting subject/bucket to tackle after this 25m sprint",
              "coachStatusLevel": "CRITICAL_CAUTION or PACE_ALERT or HIGH_MOMENTUM",
              "coachHeadline": "Punchy headline about how the student is going right now",
              "coachHonestComment": "2-3 honest coaching sentences (caution warning about lagging behind if unfinished, or strong encouragement)",
              "lagConsequenceWarning": "Exact domino consequence if the student skips or delays this task today",
              "stepByStepSprintPlan": "• Min 0-5: ...\n• Min 5-20: ...\n• Min 20-25: ..."
            }
        """.trimIndent()

        // 7. Query Gemini with model fallback chain
        val modelsToTry = listOf(
            preferredModel,
            CustomSubjectAndAiStore.MODEL_GEMINI_3_8_FLASH,
            CustomSubjectAndAiStore.MODEL_GEMINI_3_5_FLASH,
            CustomSubjectAndAiStore.MODEL_GEMINI_2_5_FLASH
        ).distinct()

        for (model in modelsToTry) {
            val result = queryGeminiModel(
                model = model,
                apiKey = apiKey,
                prompt = prompt,
                candidateSlices = candidateSlices,
                totalCompletedToday = totalCompletedToday,
                comebackCount = comebackSlices.size,
                userStreakDays = userStreakDays
            )
            if (result != null) {
                val winner = candidateSlices.find { it.slice.sliceId == result.selectedSliceId }
                if (winner != null) {
                    val winnerBucket = classifySliceBucket(winner, todayEpoch, upcomingBatchExams)
                    val winnerSubName = getSubjectName(winner)
                    CustomSubjectAndAiStore.recordSpin(
                        sliceId = winner.slice.sliceId,
                        title = winner.slice.displayTitle,
                        subjectName = winnerSubName,
                        category = getCognitiveCategory(winner),
                        bucket = winnerBucket,
                        todayEpochDay = todayEpoch
                    )
                }
                CustomSubjectAndAiStore.saveLastRecommendation(result)
                return@withContext result
            }
        }

        // 8. Local Heuristic Fallback with Balanced 3-Bucket, Circadian & Coach Reality-Check Logic
        val fallback = runSmartCognitiveAlternatingHeuristic(
            candidateSlices = candidateSlices,
            allTodayCompletedSlices = allTodayCompletedSlices,
            batteryPercent = batteryPercent,
            rejectedSlices = rejectedSlices,
            overrideNote = behavioralNoteStringBuilder.toString(),
            upcomingBatchExams = upcomingBatchExams,
            userStreakDays = userStreakDays
        )
        CustomSubjectAndAiStore.saveLastRecommendation(fallback)
        fallback
    }

    private fun queryGeminiModel(
        model: String,
        apiKey: String,
        prompt: String,
        candidateSlices: List<SliceWithParentChain>,
        totalCompletedToday: Int,
        comebackCount: Int,
        userStreakDays: Int
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
                parseGeminiResponse(
                    jsonString = responseText,
                    candidateSlices = candidateSlices,
                    totalCompletedToday = totalCompletedToday,
                    comebackCount = comebackCount,
                    userStreakDays = userStreakDays
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildDefaultCoachDiagnostics(
        winner: SliceWithParentChain,
        totalCompletedToday: Int,
        totalPendingCount: Int,
        comebackCount: Int,
        userStreakDays: Int
    ): CoachDiagnostics {
        val subName = getSubjectName(winner)
        val chNum = winner.chain.chapterNumber
        val chName = winner.chain.chapterName.ifBlank { "Chapter $chNum" }
        val lagDays = winner.slice.effectiveLagDays
        val probTarget = winner.slice.problemsTarget
        val probDone = winner.slice.problemsCompleted

        val statusLevel = when {
            comebackCount > 0 || lagDays > 0 || (totalCompletedToday == 0 && totalPendingCount >= 4) -> "CRITICAL_CAUTION"
            totalCompletedToday >= 2 && comebackCount == 0 -> "HIGH_MOMENTUM"
            else -> "PACE_ALERT"
        }

        val headline = when (statusLevel) {
            "CRITICAL_CAUTION" -> if (comebackCount > 0) {
                "⚠️ CAUTION: $comebackCount Lagging Task${if (comebackCount > 1) "s" else ""} Dragging Your Pace Down!"
            } else {
                "⚠️ PACE WARNING: 0 Completed Today with $totalPendingCount Tasks Waiting!"
            }
            "HIGH_MOMENTUM" -> "🔥 STRONG MOMENTUM: $totalCompletedToday Slashed Today — Keep Pushing!"
            else -> "⚡ PACE CHECK: $totalCompletedToday Done, $totalPendingCount Remaining in Queue"
        }

        val honestComment = when (statusLevel) {
            "CRITICAL_CAUTION" -> {
                "Be careful — you currently have $totalPendingCount unfinished slices in your queue (including $comebackCount overdue from earlier days). " +
                "If you don't finish [$subName • Ch $chNum: $chName] right now, your syllabus chain will slip further behind and tomorrow's schedule will pile up into a heavy bottleneck. Lock in for the next 25 minutes and break this lag!"
            }
            "HIGH_MOMENTUM" -> {
                "You are in a great rhythm today with $totalCompletedToday slices already conquered${if (userStreakDays > 0) " and a ${userStreakDays}-day streak active" else ""}! " +
                "Knocking out [$subName • Ch $chNum: $chName] right now will put you firmly ahead of your daily curve so nothing carries over into tomorrow."
            }
            else -> {
                "You have $totalPendingCount slices waiting today and $totalCompletedToday completed so far. " +
                "Don't let [$subName • Ch $chNum: $chName] sit until late at night — finishing this 25-minute sprint right now prevents last-minute fatigue and keeps your chapter chain on schedule."
            }
        }

        val consequenceWarning = if (lagDays > 0) {
            "Skipping this today pushes $subName Ch $chNum to +${lagDays + 1}d lag, adds +10% Boss Shield penalty, and overloads tomorrow with ${totalPendingCount + 1}+ tasks."
        } else {
            "Delaying this slice pushes $subName Ch $chNum (Day ${winner.slice.sliceNumber}/${winner.slice.totalSlicesInChain}) into tomorrow's queue and creates a Domino backlog."
        }

        val sprintPlan = if (probTarget > 0) {
            "• Min 0–5: Review core formulas & examples for $subName Ch $chNum ($chName)\n" +
            "• Min 5–20: Solve ${probTarget - probDone} remaining target problems (${winner.slice.phaseTag})\n" +
            "• Min 20–25: Verify answers, log problem counter & slash the slice!"
        } else {
            "• Min 0–5: Skim key concepts & headings of $subName Ch $chNum ($chName)\n" +
            "• Min 5–20: Deep active recall & note-making for [${winner.slice.phaseTag}]\n" +
            "• Min 20–25: Quick self-test on core takeaways & slash the slice!"
        }

        return CoachDiagnostics(
            statusLevel = statusLevel,
            headline = headline,
            honestComment = honestComment,
            consequenceWarning = consequenceWarning,
            sprintPlan = sprintPlan
        )
    }

    private data class CoachDiagnostics(
        val statusLevel: String,
        val headline: String,
        val honestComment: String,
        val consequenceWarning: String,
        val sprintPlan: String
    )

    private fun parseGeminiResponse(
        jsonString: String,
        candidateSlices: List<SliceWithParentChain>,
        totalCompletedToday: Int,
        comebackCount: Int,
        userStreakDays: Int
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
            val defaultCoach = buildDefaultCoachDiagnostics(
                winner = matchingCandidate,
                totalCompletedToday = totalCompletedToday,
                totalPendingCount = candidateSlices.size,
                comebackCount = comebackCount,
                userStreakDays = userStreakDays
            )

            AiRouletteRecommendation(
                selectedSliceId = matchingCandidate.slice.sliceId,
                cognitiveTransitionTag = parsed.optString("cognitiveTransitionTag", "🎯 AI STRATEGIST LOCK"),
                behaviorAnalysisNote = parsed.optString("behaviorAnalysisNote", "Telemetry analyzed upcoming exams and recent study pattern."),
                whyThisTaskNow = parsed.optString("whyThisTaskNow", "Selected to maximize exam readiness and maintain cognitive rhythm."),
                nextRecommendedAfterThis = parsed.optString("nextRecommendedAfterThis", "Ready to cascade to your next syllabus slice."),
                coachStatusLevel = parsed.optString("coachStatusLevel", defaultCoach.statusLevel).ifBlank { defaultCoach.statusLevel },
                coachHeadline = parsed.optString("coachHeadline", defaultCoach.headline).ifBlank { defaultCoach.headline },
                coachHonestComment = parsed.optString("coachHonestComment", defaultCoach.honestComment).ifBlank { defaultCoach.honestComment },
                lagConsequenceWarning = parsed.optString("lagConsequenceWarning", defaultCoach.consequenceWarning).ifBlank { defaultCoach.consequenceWarning },
                stepByStepSprintPlan = parsed.optString("stepByStepSprintPlan", defaultCoach.sprintPlan).ifBlank { defaultCoach.sprintPlan },
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
        upcomingBatchExams: List<BatchExamRecord> = BatchExamStore.getActiveUpcomingExams(),
        userStreakDays: Int = 0
    ): AiRouletteRecommendation {
        val nonRejected = candidateSlices.filter { !rejectedSlices.contains(it.slice.sliceId) }
        val pool = if (nonRejected.isNotEmpty()) nonRejected else candidateSlices

        val bdTime = BangladeshTimeContext.now()
        val todayEpoch = LocalDate.now().toEpochDay()
        val telemetry = CustomSubjectAndAiStore.getBucketRotationTelemetry(todayEpoch)
        val lastSpun = CustomSubjectAndAiStore.getLastSpunRecord()
        val lastSpunSubject = lastSpun?.subjectName.orEmpty()

        val mostRecentCompleted = allTodayCompletedSlices
            .filter { it.slice.isCompleted }
            .maxByOrNull { it.slice.completedAtTimestamp ?: 0L }
        val lastCat = mostRecentCompleted?.let { getCognitiveCategory(it) }

        // Partition candidate pool into 3 distinct buckets
        val cList = pool.filter { classifySliceBucket(it, todayEpoch, upcomingBatchExams) == WorkloadBucket.COMEBACK_LEFTOVER }
        val tList = pool.filter { classifySliceBucket(it, todayEpoch, upcomingBatchExams) == WorkloadBucket.TODAY_NORMAL }
        val bList = pool.filter { classifySliceBucket(it, todayEpoch, upcomingBatchExams) == WorkloadBucket.BATCH_EXAM }

        // Anti-Bias Guard 1: Subject Cooldown (don't repeat the exact same subject if other subjects exist)
        val hasDiffSubject = pool.any { !getSubjectName(it).equals(lastSpunSubject, ignoreCase = true) }
        fun applyCooldown(list: List<SliceWithParentChain>): List<SliceWithParentChain> {
            if (!hasDiffSubject || lastSpunSubject.isBlank()) return list
            val filtered = list.filter { !getSubjectName(it).equals(lastSpunSubject, ignoreCase = true) }
            return if (filtered.isNotEmpty()) filtered else list
        }

        val filteredC = applyCooldown(cList)
        val filteredT = applyCooldown(tList)
        val filteredB = applyCooldown(bList)

        // Anti-Bias Guard 2: Batch Exam Cap
        val isBatchExamCapped = lastSpun?.workloadBucket == WorkloadBucket.BATCH_EXAM ||
            (telemetry.spinsBatchExam > 0 && (filteredC.isNotEmpty() || filteredT.isNotEmpty()) && telemetry.spinsBatchExam >= (telemetry.spinsTodayNormal + telemetry.spinsComeback))

        // Cognitive load alternation target
        val targetCategory = when {
            batteryPercent <= 20 -> CognitiveLoadCategory.SOFT_MEMORIZATION
            lastCat == CognitiveLoadCategory.HEAVY_ANALYTICAL -> CognitiveLoadCategory.SOFT_MEMORIZATION
            lastCat == CognitiveLoadCategory.SOFT_MEMORIZATION -> CognitiveLoadCategory.HEAVY_ANALYTICAL
            else -> CognitiveLoadCategory.BALANCED_CONCEPTUAL
        }

        val hour = bdTime.zonedDateTime.hour
        val isMorningOrNight = (hour in 6..11) || (hour in 20..23)

        val selectedSlice: SliceWithParentChain
        val transitionTag: String
        val whyText: String
        val nextText: String

        if (filteredC.isNotEmpty() && (isMorningOrNight || filteredT.isEmpty() || telemetry.spinsComeback <= telemetry.spinsTodayNormal)) {
            // Pick COMEBACK_LEFTOVER: prioritize highest lag or matching cognitive category
            selectedSlice = filteredC.filter { getCognitiveCategory(it) == targetCategory }
                .maxByOrNull { it.slice.effectiveLagDays * 10 + it.slice.xpMultiplier }
                ?: filteredC.maxByOrNull { it.slice.effectiveLagDays * 10 + it.slice.xpMultiplier }
                ?: filteredC.first()
            val subName = getSubjectName(selectedSlice)
            transitionTag = "🔥 COMEBACK HERO | Rescue Missed Slice"
            whyText = "Rescuing overdue slice for $subName (Ch ${selectedSlice.chain.chapterNumber}, Lag: ${selectedSlice.slice.effectiveLagDays}d) to stop compounding backlog."
            nextText = "After this 25m sprint, proceed to today's core syllabus tasks."
        } else if (filteredT.isNotEmpty() && (isBatchExamCapped || filteredB.isEmpty() || telemetry.spinsTodayNormal <= telemetry.spinsBatchExam)) {
            // Pick TODAY_NORMAL: prioritize matching target category, then xpMultiplier
            selectedSlice = filteredT.filter { getCognitiveCategory(it) == targetCategory }
                .maxByOrNull { it.slice.xpMultiplier }
                ?: filteredT.first()
            val subName = getSubjectName(selectedSlice)
            val cat = getCognitiveCategory(selectedSlice)
            transitionTag = "📅 SYLLABUS MOMENTUM | Regular HSC Focus"
            whyText = "Advancing core syllabus for $subName Ch ${selectedSlice.chain.chapterNumber} (${cat.label}) during ${bdTime.bdCircadianPhase} to maintain steady daily pace."
            nextText = "Next, rotate to an analytical problem-solving sprint or private exam drill."
        } else if (filteredB.isNotEmpty() && !isBatchExamCapped) {
            // Pick BATCH_EXAM: prioritize closest exam deadline
            val urgentExams = upcomingBatchExams.filter { it.daysLeft(todayEpoch) <= 7L }.sortedBy { it.daysLeft(todayEpoch) }
            selectedSlice = urgentExams.firstNotNullOfOrNull { exam ->
                filteredB.firstOrNull { exam.matchesSlice(it) }
            } ?: filteredB.first()
            val matchingExam = upcomingBatchExams.firstOrNull { it.matchesSlice(selectedSlice) }
            val subName = getSubjectName(selectedSlice)
            val days = matchingExam?.daysLeft(todayEpoch) ?: 0L
            val daysStr = if (days == 0L) "TODAY" else "in ${days}d"
            transitionTag = "📝 BATCH EXAM RADAR (${matchingExam?.formatSummary ?: "Target Drill"})"
            whyText = "Targeted drill for $subName Ch ${selectedSlice.chain.chapterNumber}: upcoming ${matchingExam?.batchOrCoachingName ?: "coaching exam"} $daysStr (${matchingExam?.formatSummary ?: "MCQ/CQ"})."
            nextText = "After this sprint, transition to a regular syllabus slice to prevent cognitive fatigue."
        } else {
            // Fallback from general pool
            selectedSlice = pool.filter { getCognitiveCategory(it) == targetCategory }
                .maxByOrNull { it.slice.effectiveLagDays * 10 + it.slice.xpMultiplier }
                ?: pool.first()
            val subName = getSubjectName(selectedSlice)
            transitionTag = "⚡ COGNITIVE REBOUND | Brain Channel Switch"
            whyText = "Targeted $subName Ch ${selectedSlice.chain.chapterNumber} to balance cognitive load and maintain study rhythm."
            nextText = "Next, we will rotate to a contrasting conceptual block."
        }

        val winnerBucket = classifySliceBucket(selectedSlice, todayEpoch, upcomingBatchExams)
        val winnerSubName = getSubjectName(selectedSlice)
        val winnerCat = getCognitiveCategory(selectedSlice)

        CustomSubjectAndAiStore.recordSpin(
            sliceId = selectedSlice.slice.sliceId,
            title = selectedSlice.slice.displayTitle,
            subjectName = winnerSubName,
            category = winnerCat,
            bucket = winnerBucket,
            todayEpochDay = todayEpoch
        )

        val behaviorNote = if (overrideNote.isNotBlank()) {
            overrideNote.trim()
        } else {
            "Bangladesh Circadian Engine (${bdTime.currentBdTimeFormatted}): Telemetry balanced across Comeback, Today Normal, and Exam buckets."
        }

        val coach = buildDefaultCoachDiagnostics(
            winner = selectedSlice,
            totalCompletedToday = allTodayCompletedSlices.size,
            totalPendingCount = candidateSlices.size,
            comebackCount = cList.size,
            userStreakDays = userStreakDays
        )

        return AiRouletteRecommendation(
            selectedSliceId = selectedSlice.slice.sliceId,
            cognitiveTransitionTag = transitionTag,
            behaviorAnalysisNote = behaviorNote,
            whyThisTaskNow = whyText,
            nextRecommendedAfterThis = nextText,
            coachStatusLevel = coach.statusLevel,
            coachHeadline = coach.headline,
            coachHonestComment = coach.honestComment,
            lagConsequenceWarning = coach.consequenceWarning,
            stepByStepSprintPlan = coach.sprintPlan,
            isOfflineFallback = true
        )
    }
}
