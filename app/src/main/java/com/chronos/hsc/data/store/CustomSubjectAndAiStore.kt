package com.chronos.hsc.data.store

import android.content.Context
import android.content.SharedPreferences
import com.chronos.hsc.data.local.HscSubject
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CognitiveLoadCategory(
    val label: String,
    val icon: String,
    val shortDesc: String
) {
    HEAVY_ANALYTICAL(
        label = "Heavy Analytical / Math-Like",
        icon = "🧠",
        shortDesc = "Calculation, derivations, logic"
    ),
    BALANCED_CONCEPTUAL(
        label = "Balanced / Conceptual",
        icon = "⚖️",
        shortDesc = "Mixed theory and application"
    ),
    SOFT_MEMORIZATION(
        label = "Soft / Memorization & Verbal",
        icon = "📖",
        shortDesc = "Reading, vocabulary, memorization, light review"
    )
}

data class CustomSubject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shortCode: String,
    val colorHex: Long,
    val cognitiveCategory: CognitiveLoadCategory,
    val isSinglePaperOnly: Boolean = true,
    val baseSubject: HscSubject = HscSubject.ICT
) {
    val shortBadge: String get() = shortCode
    val neonColorHex: String get() = String.format("#%06X", 0xFFFFFF and colorHex.toInt())
    val hasSecondPaper: Boolean get() = !isSinglePaperOnly
}

data class CustomSubjectLink(
    val chainId: String,
    val customSubjectId: String,
    val customSubjectName: String,
    val customShortCode: String,
    val customColorHex: Long,
    val cognitiveCategory: CognitiveLoadCategory,
    val isSinglePaperOnly: Boolean = true
) {
    val id: String get() = customSubjectId
    val name: String get() = customSubjectName
    val shortBadge: String get() = customShortCode
    val colorHex: Long get() = customColorHex
    val neonColorHex: String get() = String.format("#%06X", 0xFFFFFF and customColorHex.toInt())
    val hasSecondPaper: Boolean get() = !isSinglePaperOnly
}

data class LastSpunRecord(
    val sliceId: String,
    val title: String,
    val subjectName: String,
    val cognitiveCategory: CognitiveLoadCategory,
    val timestamp: Long
)

data class AiRouletteRecommendation(
    val selectedSliceId: String,
    val cognitiveTransitionTag: String,
    val behaviorAnalysisNote: String,
    val whyThisTaskNow: String,
    val nextRecommendedAfterThis: String,
    val isOfflineFallback: Boolean = false
)

object CustomSubjectAndAiStore {

    private const val PREFS_NAME = "chronos_custom_subjects_and_ai"
    private const val KEY_CUSTOM_SUBJECTS = "key_custom_subjects_json"
    private const val KEY_CHAIN_LINKS = "key_chain_links_json"
    private const val KEY_API_KEY = "key_gemini_api_key"
    private const val KEY_MODEL = "key_gemini_selected_model"
    private const val KEY_LAST_SPUN = "key_last_spun_record_json"
    private const val KEY_LAST_RECOMMENDATION = "key_last_ai_recommendation_json"

    val DEFAULT_API_KEY: String by lazy {
        try {
            String(
                android.util.Base64.decode(
                    "QVEuQWI4Uk42Si16THBIaDJTQmd1OWUtVndUWFROci1MTTI2MGFwUk9tTnRkVDRseUhtQ2c=",
                    android.util.Base64.DEFAULT
                ),
                Charsets.UTF_8
            ).trim()
        } catch (_: Exception) {
            ""
        }
    }
    const val MODEL_GEMINI_3_8_FLASH = "gemini-3.8-flash"
    const val MODEL_GEMINI_3_5_FLASH = "gemini-3.5-flash"
    const val MODEL_GEMINI_2_5_FLASH = "gemini-2.5-flash"

    val COLOR_PRESETS = listOf(
        0xFF00E5FF, // Cyan
        0xFFFF6D00, // Orange
        0xFFD500F9, // Magenta / Purple
        0xFF00E676, // Spring Emerald
        0xFFFFEA00, // Electric Yellow
        0xFFFF1744, // Crimson Red
        0xFF3D5AFE, // Indigo Blue
        0xFF1DE9B6  // Teal
    )

    private var prefs: SharedPreferences? = null
    private val rejectedSliceIdsSession = mutableSetOf<String>()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("CustomSubjectAndAiStore must be initialized with Context first.")
    }

    // =========================================================================
    // CUSTOM SUBJECTS CRUD
    // =========================================================================

    @Synchronized
    fun getCustomSubjects(): List<CustomSubject> {
        val raw = getPrefs().getString(KEY_CUSTOM_SUBJECTS, null) ?: return emptyList()
        val list = mutableListOf<CustomSubject>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val categoryName = obj.optString("cognitiveCategory", CognitiveLoadCategory.BALANCED_CONCEPTUAL.name)
                val baseSubName = obj.optString("baseSubject", HscSubject.ICT.name)
                list.add(
                    CustomSubject(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        shortCode = obj.getString("shortCode"),
                        colorHex = obj.getLong("colorHex"),
                        cognitiveCategory = runCatching { CognitiveLoadCategory.valueOf(categoryName) }
                            .getOrDefault(CognitiveLoadCategory.BALANCED_CONCEPTUAL),
                        isSinglePaperOnly = obj.optBoolean("isSinglePaperOnly", true),
                        baseSubject = runCatching { HscSubject.valueOf(baseSubName) }
                            .getOrDefault(HscSubject.ICT)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    @Synchronized
    fun saveCustomSubject(subject: CustomSubject) {
        val current = getCustomSubjects().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == subject.id }
        if (existingIndex >= 0) {
            current[existingIndex] = subject
        } else {
            current.add(subject)
        }
        persistCustomSubjects(current)
    }

    @Synchronized
    fun deleteCustomSubject(id: String) {
        val current = getCustomSubjects().filter { it.id != id }
        persistCustomSubjects(current)
    }

    private fun persistCustomSubjects(list: List<CustomSubject>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("shortCode", item.shortCode)
                put("colorHex", item.colorHex)
                put("cognitiveCategory", item.cognitiveCategory.name)
                put("isSinglePaperOnly", item.isSinglePaperOnly)
                put("baseSubject", item.baseSubject.name)
            }
            array.put(obj)
        }
        getPrefs().edit().putString(KEY_CUSTOM_SUBJECTS, array.toString()).apply()
    }

    // =========================================================================
    // CHAIN ID -> CUSTOM SUBJECT LINKS
    // =========================================================================

    @Synchronized
    fun getCustomSubjectForChain(chainId: String): CustomSubjectLink? {
        val links = getAllChainLinks()
        return links[chainId]
    }

    @Synchronized
    fun getAllChainLinks(): Map<String, CustomSubjectLink> {
        val raw = getPrefs().getString(KEY_CHAIN_LINKS, null) ?: return emptyMap()
        val map = mutableMapOf<String, CustomSubjectLink>()
        try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val chainId = keys.next()
                val itemObj = obj.getJSONObject(chainId)
                val catName = itemObj.optString("cognitiveCategory", CognitiveLoadCategory.BALANCED_CONCEPTUAL.name)
                map[chainId] = CustomSubjectLink(
                    chainId = chainId,
                    customSubjectId = itemObj.getString("customSubjectId"),
                    customSubjectName = itemObj.getString("customSubjectName"),
                    customShortCode = itemObj.getString("customShortCode"),
                    customColorHex = itemObj.getLong("customColorHex"),
                    cognitiveCategory = runCatching { CognitiveLoadCategory.valueOf(catName) }
                        .getOrDefault(CognitiveLoadCategory.BALANCED_CONCEPTUAL),
                    isSinglePaperOnly = itemObj.optBoolean("isSinglePaperOnly", true)
                )
            }
        } catch (_: Exception) {}
        return map
    }

    fun getCustomSubject(id: String): CustomSubject? = getCustomSubjects().find { it.id == id }

    @Synchronized
    fun linkChainToCustomSubject(chainId: String, subject: CustomSubject) {
        val links = getAllChainLinks().toMutableMap()
        links[chainId] = CustomSubjectLink(
            chainId = chainId,
            customSubjectId = subject.id,
            customSubjectName = subject.name,
            customShortCode = subject.shortCode,
            customColorHex = subject.colorHex,
            cognitiveCategory = subject.cognitiveCategory,
            isSinglePaperOnly = subject.isSinglePaperOnly
        )
        persistChainLinks(links)
    }

    @Synchronized
    fun linkChainToCustomSubject(chainId: String, customSubjectId: String) {
        val sub = getCustomSubject(customSubjectId) ?: return
        linkChainToCustomSubject(chainId, sub)
    }

    @Synchronized
    fun removeChainLink(chainId: String) {
        val links = getAllChainLinks().toMutableMap()
        if (links.remove(chainId) != null) {
            persistChainLinks(links)
        }
    }

    private fun persistChainLinks(map: Map<String, CustomSubjectLink>) {
        val root = JSONObject()
        for ((chainId, link) in map) {
            val item = JSONObject().apply {
                put("customSubjectId", link.customSubjectId)
                put("customSubjectName", link.customSubjectName)
                put("customShortCode", link.customShortCode)
                put("customColorHex", link.customColorHex)
                put("cognitiveCategory", link.cognitiveCategory.name)
                put("isSinglePaperOnly", link.isSinglePaperOnly)
            }
            root.put(chainId, item)
        }
        getPrefs().edit().putString(KEY_CHAIN_LINKS, root.toString()).apply()
    }

    @Synchronized
    fun addRejectedSlice(sliceId: String) {
        addRejectedSliceSession(sliceId)
    }

    // =========================================================================
    // AI SETTINGS & TELEMETRY
    // =========================================================================

    fun getApiKey(): String {
        return getPrefs().getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    fun setApiKey(key: String) {
        getPrefs().edit().putString(KEY_API_KEY, key.trim().ifEmpty { DEFAULT_API_KEY }).apply()
    }

    fun getSelectedModel(): String {
        return getPrefs().getString(KEY_MODEL, MODEL_GEMINI_3_8_FLASH) ?: MODEL_GEMINI_3_8_FLASH
    }

    fun setSelectedModel(model: String) {
        getPrefs().edit().putString(KEY_MODEL, model.trim()).apply()
    }

    @Synchronized
    fun saveLastSpunRecord(record: LastSpunRecord) {
        val obj = JSONObject().apply {
            put("sliceId", record.sliceId)
            put("title", record.title)
            put("subjectName", record.subjectName)
            put("cognitiveCategory", record.cognitiveCategory.name)
            put("timestamp", record.timestamp)
        }
        getPrefs().edit().putString(KEY_LAST_SPUN, obj.toString()).apply()
    }

    @Synchronized
    fun getLastSpunRecord(): LastSpunRecord? {
        val raw = getPrefs().getString(KEY_LAST_SPUN, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val catName = obj.optString("cognitiveCategory", CognitiveLoadCategory.BALANCED_CONCEPTUAL.name)
            LastSpunRecord(
                sliceId = obj.getString("sliceId"),
                title = obj.getString("title"),
                subjectName = obj.getString("subjectName"),
                cognitiveCategory = runCatching { CognitiveLoadCategory.valueOf(catName) }
                    .getOrDefault(CognitiveLoadCategory.BALANCED_CONCEPTUAL),
                timestamp = obj.getLong("timestamp")
            )
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun addRejectedSliceSession(sliceId: String) {
        rejectedSliceIdsSession.add(sliceId)
    }

    @Synchronized
    fun getRejectedSlicesSession(): Set<String> {
        return rejectedSliceIdsSession.toSet()
    }

    @Synchronized
    fun clearRejectedSlicesSession() {
        rejectedSliceIdsSession.clear()
    }

    @Synchronized
    fun saveLastRecommendation(rec: AiRouletteRecommendation) {
        val obj = JSONObject().apply {
            put("selectedSliceId", rec.selectedSliceId)
            put("cognitiveTransitionTag", rec.cognitiveTransitionTag)
            put("behaviorAnalysisNote", rec.behaviorAnalysisNote)
            put("whyThisTaskNow", rec.whyThisTaskNow)
            put("nextRecommendedAfterThis", rec.nextRecommendedAfterThis)
            put("isOfflineFallback", rec.isOfflineFallback)
        }
        getPrefs().edit().putString(KEY_LAST_RECOMMENDATION, obj.toString()).apply()
    }

    @Synchronized
    fun getLastRecommendation(): AiRouletteRecommendation? {
        val raw = getPrefs().getString(KEY_LAST_RECOMMENDATION, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            AiRouletteRecommendation(
                selectedSliceId = obj.getString("selectedSliceId"),
                cognitiveTransitionTag = obj.getString("cognitiveTransitionTag"),
                behaviorAnalysisNote = obj.getString("behaviorAnalysisNote"),
                whyThisTaskNow = obj.getString("whyThisTaskNow"),
                nextRecommendedAfterThis = obj.getString("nextRecommendedAfterThis"),
                isOfflineFallback = obj.optBoolean("isOfflineFallback", false)
            )
        } catch (_: Exception) {
            null
        }
    }
}
