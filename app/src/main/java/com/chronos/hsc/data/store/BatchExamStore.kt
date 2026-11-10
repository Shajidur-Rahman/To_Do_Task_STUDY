package com.chronos.hsc.data.store

import android.content.Context
import android.content.SharedPreferences
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.SliceWithParentChain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

enum class ExamFormatMode(val label: String, val icon: String) {
    MCQ_ONLY("MCQ Only", "⚡"),
    CQ_ONLY("CQ Only", "✍️"),
    MCQ_AND_CQ_BOTH("MCQ + CQ Both", "🔥");

    companion object {
        fun fromString(value: String): ExamFormatMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MCQ_AND_CQ_BOTH
        }
    }
}

data class BatchExamRecord(
    val examId: String = UUID.randomUUID().toString(),
    val batchOrCoachingName: String,
    val subjectName: String,
    val paperName: String = "1st Paper",
    val chapterNumbersAndTitles: String,
    val examDateEpochDay: Long,
    val createdAtEpochDay: Long = LocalDate.now().toEpochDay(),
    val examFormatMode: ExamFormatMode = ExamFormatMode.MCQ_AND_CQ_BOTH,
    val mcqCount: Int = 0,
    val cqCount: Int = 0,
    val aiDifficultyScore: Int = 5,
    val aiDifficultyLabel: String = "Balanced (5/10)",
    val aiEstimatedTotalStudyHours: Float = 4.0f,
    val aiImportanceScore: Int = 50,
    val aiPrepStrategyAdvice: String = "Review core formulas and practice past test questions.",
    val studyHoursLogged: Float = 0f,
    val isCompleted: Boolean = false
) {
    fun daysLeft(today: Long = LocalDate.now().toEpochDay()): Long = examDateEpochDay - today

    val remainingStudyHours: Float
        get() = (aiEstimatedTotalStudyHours - studyHoursLogged).coerceAtLeast(0f)

    val formatSummary: String
        get() = when (examFormatMode) {
            ExamFormatMode.MCQ_ONLY -> "$mcqCount MCQ"
            ExamFormatMode.CQ_ONLY -> "$cqCount CQ"
            ExamFormatMode.MCQ_AND_CQ_BOTH -> "$mcqCount MCQ + $cqCount CQ"
        }

    val shortSubject: String
        get() {
            val p = if (paperName.contains("1st", ignoreCase = true) || paperName.contains("1", ignoreCase = true)) "1"
            else if (paperName.contains("2nd", ignoreCase = true) || paperName.contains("2", ignoreCase = true)) "2"
            else ""
            return when {
                subjectName.contains("Physics", ignoreCase = true) -> "P$p"
                subjectName.contains("Chemistry", ignoreCase = true) -> "C$p"
                subjectName.contains("Math", ignoreCase = true) -> "HM$p"
                subjectName.contains("Biology", ignoreCase = true) -> "Bio$p"
                subjectName.contains("Bangla", ignoreCase = true) -> "Ban$p"
                subjectName.contains("English", ignoreCase = true) -> "Eng$p"
                subjectName.contains("ICT", ignoreCase = true) -> "ICT"
                else -> subjectName.take(4).uppercase(Locale.US) + p
            }
        }

    fun matchesSlice(slice: SliceWithParentChain): Boolean {
        // 1. Check subject match
        val customLink = CustomSubjectAndAiStore.getCustomSubjectForChain(slice.chain.chainId)
        val sliceSubName = customLink?.customSubjectName ?: slice.chain.subject.fullName
        val subMatch = sliceSubName.equals(subjectName, ignoreCase = true) ||
                slice.chain.subject.fullName.equals(subjectName, ignoreCase = true) ||
                slice.chain.subject.name.equals(subjectName, ignoreCase = true) ||
                slice.chain.subject.shortCode.equals(subjectName, ignoreCase = true)

        if (!subMatch) return false

        // 2. Check paper match
        if (paperName.isNotBlank() && paperName != "Single Paper" && slice.chain.paper != HscPaper.SINGLE) {
            val isFirstPaper = paperName.contains("1st", ignoreCase = true) || paperName.contains("1", ignoreCase = true)
            val isSecondPaper = paperName.contains("2nd", ignoreCase = true) || paperName.contains("2", ignoreCase = true)
            if (isFirstPaper && slice.chain.paper != HscPaper.FIRST_PAPER) return false
            if (isSecondPaper && slice.chain.paper != HscPaper.SECOND_PAPER) return false
        }

        // 3. Check chapter match (numeric regex or string match)
        val chNum = slice.chain.chapterNumber.toString()
        val cleanChapters = chapterNumbersAndTitles.trim()
        val regex = Regex("""\b$chNum\b""")
        val chMatches = regex.containsMatchIn(cleanChapters) ||
                cleanChapters.contains("Ch $chNum", ignoreCase = true) ||
                cleanChapters.contains("Chapter $chNum", ignoreCase = true) ||
                cleanChapters.contains("অধ্যায় $chNum", ignoreCase = true) ||
                (slice.chain.chapterName.isNotBlank() && cleanChapters.contains(slice.chain.chapterName, ignoreCase = true))

        return chMatches
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("examId", examId)
            put("batchOrCoachingName", batchOrCoachingName)
            put("subjectName", subjectName)
            put("paperName", paperName)
            put("chapterNumbersAndTitles", chapterNumbersAndTitles)
            put("examDateEpochDay", examDateEpochDay)
            put("createdAtEpochDay", createdAtEpochDay)
            put("examFormatMode", examFormatMode.name)
            put("mcqCount", mcqCount)
            put("cqCount", cqCount)
            put("aiDifficultyScore", aiDifficultyScore)
            put("aiDifficultyLabel", aiDifficultyLabel)
            put("aiEstimatedTotalStudyHours", aiEstimatedTotalStudyHours.toDouble())
            put("aiImportanceScore", aiImportanceScore)
            put("aiPrepStrategyAdvice", aiPrepStrategyAdvice)
            put("studyHoursLogged", studyHoursLogged.toDouble())
            put("isCompleted", isCompleted)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BatchExamRecord {
            return BatchExamRecord(
                examId = json.optString("examId", UUID.randomUUID().toString()),
                batchOrCoachingName = json.optString("batchOrCoachingName", "Private Batch"),
                subjectName = json.optString("subjectName", "Physics"),
                paperName = json.optString("paperName", "1st Paper"),
                chapterNumbersAndTitles = json.optString("chapterNumbersAndTitles", "Ch 1"),
                examDateEpochDay = json.optLong("examDateEpochDay", LocalDate.now().toEpochDay() + 3),
                createdAtEpochDay = json.optLong("createdAtEpochDay", LocalDate.now().toEpochDay()),
                examFormatMode = ExamFormatMode.fromString(json.optString("examFormatMode", ExamFormatMode.MCQ_AND_CQ_BOTH.name)),
                mcqCount = json.optInt("mcqCount", 0),
                cqCount = json.optInt("cqCount", 0),
                aiDifficultyScore = json.optInt("aiDifficultyScore", 5),
                aiDifficultyLabel = json.optString("aiDifficultyLabel", "Balanced (5/10)"),
                aiEstimatedTotalStudyHours = json.optDouble("aiEstimatedTotalStudyHours", 4.0).toFloat(),
                aiImportanceScore = json.optInt("aiImportanceScore", 50),
                aiPrepStrategyAdvice = json.optString("aiPrepStrategyAdvice", "Review core formulas and practice test questions."),
                studyHoursLogged = json.optDouble("studyHoursLogged", 0.0).toFloat(),
                isCompleted = json.optBoolean("isCompleted", false)
            )
        }
    }
}

data class BatchExamAiEvaluation(
    val difficultyScore: Int,
    val difficultyLabel: String,
    val estimatedTotalStudyHours: Float,
    val importanceScore: Int,
    val prepStrategyAdvice: String
)

object BatchExamStore {

    private const val PREFS_NAME = "chronos_batch_exams_store"
    private const val KEY_BATCH_EXAMS = "key_batch_exams_json"

    private var prefs: SharedPreferences? = null

    private val _batchExamsFlow = MutableStateFlow<List<BatchExamRecord>>(emptyList())
    val batchExamsFlow: StateFlow<List<BatchExamRecord>> = _batchExamsFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromStorage()
        }
    }

    @Synchronized
    private fun loadFromStorage() {
        val sp = prefs ?: return
        val rawJson = sp.getString(KEY_BATCH_EXAMS, null)
        if (rawJson.isNullOrBlank()) {
            _batchExamsFlow.value = emptyList()
            return
        }

        try {
            val jsonArray = JSONArray(rawJson)
            val list = mutableListOf<BatchExamRecord>()
            for (i in 0 until jsonArray.length()) {
                val item = BatchExamRecord.fromJson(jsonArray.getJSONObject(i))
                list.add(item)
            }
            _batchExamsFlow.value = list
        } catch (_: Exception) {
            _batchExamsFlow.value = emptyList()
        }
    }

    @Synchronized
    private fun saveToStorage(list: List<BatchExamRecord>) {
        val sp = prefs ?: return
        try {
            val jsonArray = JSONArray()
            for (record in list) {
                jsonArray.put(record.toJson())
            }
            sp.edit().putString(KEY_BATCH_EXAMS, jsonArray.toString()).apply()
            _batchExamsFlow.value = list
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    @Synchronized
    fun addBatchExam(exam: BatchExamRecord) {
        val current = _batchExamsFlow.value.toMutableList()
        current.removeAll { it.examId == exam.examId }
        current.add(0, exam)
        saveToStorage(current)
    }

    @Synchronized
    fun updateBatchExam(exam: BatchExamRecord) {
        val current = _batchExamsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.examId == exam.examId }
        if (index != -1) {
            current[index] = exam
            saveToStorage(current)
        } else {
            addBatchExam(exam)
        }
    }

    @Synchronized
    fun deleteBatchExam(examId: String) {
        val current = _batchExamsFlow.value.toMutableList()
        current.removeAll { it.examId == examId }
        saveToStorage(current)
    }

    @Synchronized
    fun markExamCompleted(examId: String, isCompleted: Boolean) {
        val current = _batchExamsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.examId == examId }
        if (index != -1) {
            current[index] = current[index].copy(isCompleted = isCompleted)
            saveToStorage(current)
        }
    }

    @Synchronized
    fun incrementStudyHours(examId: String, deltaHours: Float) {
        val current = _batchExamsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.examId == examId }
        if (index != -1) {
            val rec = current[index]
            val newLogged = (rec.studyHoursLogged + deltaHours).coerceAtLeast(0f)
            current[index] = rec.copy(studyHoursLogged = newLogged)
            saveToStorage(current)
        }
    }

    /**
     * Checks if a completed or uncompleted slice matches any active upcoming private exam.
     * If matched, increments (or decrements) study hours by deltaHours.
     */
    @Synchronized
    fun logStudyHoursForMatchingSlice(slice: SliceWithParentChain, deltaHours: Float): BatchExamRecord? {
        val today = LocalDate.now().toEpochDay()
        val activeExams = getActiveUpcomingExams(today)
        val matchedExam = activeExams.firstOrNull { it.matchesSlice(slice) }
        if (matchedExam != null) {
            incrementStudyHours(matchedExam.examId, deltaHours)
            return matchedExam
        }
        return null
    }

    /**
     * Finds matching active upcoming batch exam for a slice
     */
    fun findMatchingActiveExam(slice: SliceWithParentChain, today: Long = LocalDate.now().toEpochDay()): BatchExamRecord? {
        return getActiveUpcomingExams(today).firstOrNull { it.matchesSlice(slice) }
    }

    fun getAllExams(): List<BatchExamRecord> = _batchExamsFlow.value

    /**
     * Active upcoming exams: not marked completed, sorted by urgency (daysLeft ascending, then importance descending)
     */
    fun getActiveUpcomingExams(today: Long = LocalDate.now().toEpochDay()): List<BatchExamRecord> {
        return _batchExamsFlow.value
            .filter { !it.isCompleted }
            .sortedWith(
                compareBy<BatchExamRecord> { it.daysLeft(today) }
                    .thenByDescending { it.aiImportanceScore }
            )
    }

    /**
     * The single most urgent upcoming exam for the floating button
     */
    fun getMostUrgentExam(today: Long = LocalDate.now().toEpochDay()): BatchExamRecord? {
        return getActiveUpcomingExams(today).firstOrNull()
    }
}
