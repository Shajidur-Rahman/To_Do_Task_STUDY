package com.chronos.hsc.data.backup

import com.chronos.hsc.data.local.DailyTaskSlice
import com.chronos.hsc.data.local.DistributionCurve
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.NeuroDrillType
import com.chronos.hsc.data.local.NeuroSessionEntity
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.data.local.UserStats
import org.json.JSONArray
import org.json.JSONObject

data class ChronosFullBackupDto(
    val backupVersion: Int = 2,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val userStats: UserStats,
    val chains: List<StudyTopicChain>,
    val slices: List<DailyTaskSlice>,
    val exams: List<ExamCountdownEntity>,
    val neuroSessions: List<NeuroSessionEntity>,
    val customSubjectsJson: String? = null,
    val chainLinksJson: String? = null,
    val batchExamsJson: String? = null
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("backupVersion", backupVersion)
        root.put("exportTimestamp", exportTimestamp)
        root.put("appVersion", "1.4.4")

        // UserStats
        val statsObj = JSONObject()
        statsObj.put("id", userStats.id)
        statsObj.put("totalXp", userStats.totalXp)
        statsObj.put("currentLevel", userStats.currentLevel)
        statsObj.put("streakDays", userStats.streakDays)
        userStats.lastStudyEpochDay?.let { statsObj.put("lastStudyEpochDay", it) }
        root.put("userStats", statsObj)

        // Chains
        val chainsArray = JSONArray()
        chains.forEach { c ->
            val cObj = JSONObject()
            cObj.put("chainId", c.chainId)
            cObj.put("subject", c.subject.name)
            cObj.put("paper", c.paper.name)
            cObj.put("chapterNumber", c.chapterNumber)
            cObj.put("chapterName", c.chapterName)
            cObj.put("totalDays", c.totalDays)
            cObj.put("distributionCurve", c.distributionCurve.name)
            cObj.put("energyLevel", c.energyLevel.name)
            cObj.put("dailyProblemTarget", c.dailyProblemTarget)
            cObj.put("createdAtEpochDay", c.createdAtEpochDay)
            cObj.put("bossMaxHp", c.bossMaxHp)
            cObj.put("bossBonusShieldHp", c.bossBonusShieldHp)
            cObj.put("isChainCompleted", c.isChainCompleted)
            c.linkedPreviousChainId?.let { cObj.put("linkedPreviousChainId", it) }
            chainsArray.put(cObj)
        }
        root.put("chains", chainsArray)

        // Slices
        val slicesArray = JSONArray()
        slices.forEach { s ->
            val sObj = JSONObject()
            sObj.put("sliceId", s.sliceId)
            sObj.put("chainId", s.chainId)
            sObj.put("sliceNumber", s.sliceNumber)
            sObj.put("totalSlicesInChain", s.totalSlicesInChain)
            sObj.put("displayTitle", s.displayTitle)
            sObj.put("shortBadge", s.shortBadge)
            sObj.put("phaseTag", s.phaseTag)
            sObj.put("scheduledDateEpochDay", s.scheduledDateEpochDay)
            sObj.put("problemsCompleted", s.problemsCompleted)
            sObj.put("problemsTarget", s.problemsTarget)
            sObj.put("isCompleted", s.isCompleted)
            s.completedAtTimestamp?.let { sObj.put("completedAtTimestamp", it) }
            sObj.put("isRetentionEcho", s.isRetentionEcho)
            s.echoIntervalDays?.let { sObj.put("echoIntervalDays", it) }
            s.sliceEnergyOverride?.let { sObj.put("sliceEnergyOverride", it.name) }
            sObj.put("xpMultiplier", s.xpMultiplier)
            sObj.put("originalScheduledEpochDay", s.originalScheduledEpochDay)
            sObj.put("shiftLagCount", s.shiftLagCount)
            slicesArray.put(sObj)
        }
        root.put("slices", slicesArray)

        // Exams
        val examsArray = JSONArray()
        exams.forEach { e ->
            val eObj = JSONObject()
            eObj.put("examId", e.examId)
            eObj.put("examTitle", e.examTitle)
            eObj.put("targetDateEpochDay", e.targetDateEpochDay)
            eObj.put("createdAtEpochDay", e.createdAtEpochDay)
            eObj.put("isPrimaryPinned", e.isPrimaryPinned)
            examsArray.put(eObj)
        }
        root.put("exams", examsArray)

        // Neuro Sessions
        val neuroArray = JSONArray()
        neuroSessions.forEach { n ->
            val nObj = JSONObject()
            nObj.put("sessionId", n.sessionId)
            nObj.put("drillType", n.drillType.name)
            nObj.put("scoreMetric", n.scoreMetric)
            nObj.put("durationSeconds", n.durationSeconds)
            nObj.put("timestamp", n.timestamp)
            neuroArray.put(nObj)
        }
        root.put("neuroSessions", neuroArray)

        customSubjectsJson?.let { root.put("customSubjectsJson", it) }
        chainLinksJson?.let { root.put("chainLinksJson", it) }
        batchExamsJson?.let { root.put("batchExamsJson", it) }

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(json: String): ChronosFullBackupDto {
            val root = JSONObject(json)
            val version = root.optInt("backupVersion", 1)
            val exportTime = root.optLong("exportTimestamp", System.currentTimeMillis())

            // UserStats
            val statsObj = root.optJSONObject("userStats")
            val userStats = if (statsObj != null) {
                UserStats(
                    id = statsObj.optInt("id", 1),
                    totalXp = statsObj.optInt("totalXp", 0),
                    currentLevel = statsObj.optInt("currentLevel", 1),
                    streakDays = statsObj.optInt("streakDays", 0),
                    lastStudyEpochDay = if (statsObj.has("lastStudyEpochDay") && !statsObj.isNull("lastStudyEpochDay")) {
                        statsObj.getLong("lastStudyEpochDay")
                    } else null
                )
            } else {
                UserStats()
            }

            // Chains
            val chains = mutableListOf<StudyTopicChain>()
            val chainsArray = root.optJSONArray("chains") ?: JSONArray()
            for (i in 0 until chainsArray.length()) {
                val cObj = chainsArray.getJSONObject(i)
                chains.add(
                    StudyTopicChain(
                        chainId = cObj.getString("chainId"),
                        subject = safeEnum(cObj.optString("subject"), HscSubject.PHYSICS),
                        paper = safeEnum(cObj.optString("paper"), HscPaper.FIRST_PAPER),
                        chapterNumber = cObj.getInt("chapterNumber"),
                        chapterName = cObj.optString("chapterName", ""),
                        totalDays = cObj.getInt("totalDays"),
                        distributionCurve = safeEnum(cObj.optString("distributionCurve"), DistributionCurve.FLAT),
                        energyLevel = safeEnum(cObj.optString("energyLevel"), EnergyLevel.HIGH_FOCUS),
                        dailyProblemTarget = cObj.optInt("dailyProblemTarget", 5),
                        createdAtEpochDay = cObj.optLong("createdAtEpochDay", 0L),
                        bossMaxHp = cObj.optInt("bossMaxHp", 100),
                        bossBonusShieldHp = cObj.optInt("bossBonusShieldHp", 0),
                        isChainCompleted = cObj.optBoolean("isChainCompleted", false),
                        linkedPreviousChainId = if (cObj.has("linkedPreviousChainId") && !cObj.isNull("linkedPreviousChainId")) {
                            cObj.getString("linkedPreviousChainId")
                        } else null
                    )
                )
            }

            // Slices
            val slices = mutableListOf<DailyTaskSlice>()
            val slicesArray = root.optJSONArray("slices") ?: JSONArray()
            for (i in 0 until slicesArray.length()) {
                val sObj = slicesArray.getJSONObject(i)
                val scheduledEpoch = sObj.getLong("scheduledDateEpochDay")
                slices.add(
                    DailyTaskSlice(
                        sliceId = sObj.getString("sliceId"),
                        chainId = sObj.getString("chainId"),
                        sliceNumber = sObj.getInt("sliceNumber"),
                        totalSlicesInChain = sObj.getInt("totalSlicesInChain"),
                        displayTitle = sObj.getString("displayTitle"),
                        shortBadge = sObj.getString("shortBadge"),
                        phaseTag = sObj.optString("phaseTag", "Standard Slice"),
                        scheduledDateEpochDay = scheduledEpoch,
                        problemsCompleted = sObj.optInt("problemsCompleted", 0),
                        problemsTarget = sObj.optInt("problemsTarget", 5),
                        isCompleted = sObj.optBoolean("isCompleted", false),
                        completedAtTimestamp = if (sObj.has("completedAtTimestamp") && !sObj.isNull("completedAtTimestamp")) {
                            sObj.getLong("completedAtTimestamp")
                        } else null,
                        isRetentionEcho = sObj.optBoolean("isRetentionEcho", false),
                        echoIntervalDays = if (sObj.has("echoIntervalDays") && !sObj.isNull("echoIntervalDays")) {
                            sObj.getInt("echoIntervalDays")
                        } else null,
                        sliceEnergyOverride = if (sObj.has("sliceEnergyOverride") && !sObj.isNull("sliceEnergyOverride")) {
                            safeEnumOrNull<EnergyLevel>(sObj.getString("sliceEnergyOverride"))
                        } else null,
                        xpMultiplier = sObj.optInt("xpMultiplier", 1),
                        originalScheduledEpochDay = sObj.optLong("originalScheduledEpochDay", scheduledEpoch),
                        shiftLagCount = sObj.optInt("shiftLagCount", 0)
                    )
                )
            }

            // Exams
            val exams = mutableListOf<ExamCountdownEntity>()
            val examsArray = root.optJSONArray("exams") ?: JSONArray()
            for (i in 0 until examsArray.length()) {
                val eObj = examsArray.getJSONObject(i)
                exams.add(
                    ExamCountdownEntity(
                        examId = eObj.getString("examId"),
                        examTitle = eObj.getString("examTitle"),
                        targetDateEpochDay = eObj.getLong("targetDateEpochDay"),
                        createdAtEpochDay = eObj.getLong("createdAtEpochDay"),
                        isPrimaryPinned = eObj.optBoolean("isPrimaryPinned", false)
                    )
                )
            }

            // Neuro Sessions
            val neuroSessions = mutableListOf<NeuroSessionEntity>()
            val neuroArray = root.optJSONArray("neuroSessions") ?: JSONArray()
            for (i in 0 until neuroArray.length()) {
                val nObj = neuroArray.getJSONObject(i)
                neuroSessions.add(
                    NeuroSessionEntity(
                        sessionId = nObj.getString("sessionId"),
                        drillType = safeEnum(nObj.getString("drillType"), NeuroDrillType.FOVEAL_LOCK),
                        scoreMetric = nObj.optDouble("scoreMetric", 0.0),
                        durationSeconds = nObj.optInt("durationSeconds", 60),
                        timestamp = nObj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            val customSubjectsJson = if (root.has("customSubjectsJson") && !root.isNull("customSubjectsJson")) {
                root.getString("customSubjectsJson")
            } else null
            val chainLinksJson = if (root.has("chainLinksJson") && !root.isNull("chainLinksJson")) {
                root.getString("chainLinksJson")
            } else null
            val batchExamsJson = if (root.has("batchExamsJson") && !root.isNull("batchExamsJson")) {
                root.getString("batchExamsJson")
            } else null

            return ChronosFullBackupDto(
                backupVersion = version,
                exportTimestamp = exportTime,
                userStats = userStats,
                chains = chains,
                slices = slices,
                exams = exams,
                neuroSessions = neuroSessions,
                customSubjectsJson = customSubjectsJson,
                chainLinksJson = chainLinksJson,
                batchExamsJson = batchExamsJson
            )
        }

        private inline fun <reified T : Enum<T>> safeEnum(name: String?, default: T): T {
            if (name == null) return default
            return try {
                java.lang.Enum.valueOf(T::class.java, name)
            } catch (e: Exception) {
                default
            }
        }

        private inline fun <reified T : Enum<T>> safeEnumOrNull(name: String?): T? {
            if (name == null) return null
            return try {
                java.lang.Enum.valueOf(T::class.java, name)
            } catch (e: Exception) {
                null
            }
        }
    }
}
