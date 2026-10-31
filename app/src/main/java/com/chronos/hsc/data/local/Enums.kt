package com.chronos.hsc.data.local

enum class HscSubject(
    val fullName: String,
    val shortCode: String,
    val colorHex: Long
) {
    BANGLA(
        fullName = "Bangla",
        shortCode = "BN",
        colorHex = 0xFFFF4081
    ),
    ENGLISH(
        fullName = "English",
        shortCode = "EN",
        colorHex = 0xFF536DFE
    ),
    PHYSICS(
        fullName = "Physics",
        shortCode = "P",
        colorHex = 0xFF00E5FF
    ),
    CHEMISTRY(
        fullName = "Chemistry",
        shortCode = "C",
        colorHex = 0xFFFF6D00
    ),
    HIGHER_MATH(
        fullName = "Higher Math",
        shortCode = "H",
        colorHex = 0xFFD500F9
    ),
    BIOLOGY(
        fullName = "Biology",
        shortCode = "B",
        colorHex = 0xFF00E676
    ),
    ICT(
        fullName = "ICT",
        shortCode = "ICT",
        colorHex = 0xFFFFEA00
    );

    val isSinglePaperOnly: Boolean
        get() = this == ICT
}

enum class HscPaper(
    val fullName: String,
    val shortCode: String,
    val compactTitleLabel: String
) {
    FIRST_PAPER(
        fullName = "1st Paper",
        shortCode = "F",
        compactTitleLabel = "1st"
    ),
    SECOND_PAPER(
        fullName = "2nd Paper",
        shortCode = "S",
        compactTitleLabel = "2nd"
    ),
    SINGLE(
        fullName = "Single Paper",
        shortCode = "",
        compactTitleLabel = ""
    )
}

enum class DistributionCurve(
    val title: String,
    val subtitle: String
) {
    FLAT(
        title = "Flat Distribution",
        subtitle = "Equal daily workload across all slices"
    ),
    CONCEPT_TO_PROBLEM(
        title = "Concept-to-Problem Curve",
        subtitle = "Day 1: Theory/Lecture -> Middle: Textbook -> Final: Question Bank"
    );

    fun phaseTagForDay(dayIndexOneBased: Int, totalDays: Int): String {
        if (this == FLAT) return "Standard Slice"
        return when {
            totalDays == 1 -> "Full Chapter Mastery"
            dayIndexOneBased == 1 -> "Theory & Lecture"
            dayIndexOneBased == totalDays -> "Question Bank & Timed Test"
            else -> "Textbook & Examples"
        }
    }
}

enum class EnergyLevel(
    val label: String,
    val batteryPercent: Int,
    val description: String,
    val sortWeight: Int
) {
    HIGH_FOCUS(
        label = "100% High Focus",
        batteryPercent = 100,
        description = "Heavy derivations & deep problem solving",
        sortWeight = 3
    ),
    MEDIUM(
        label = "50% Medium",
        batteryPercent = 50,
        description = "Standard conceptual study & textbook exercises",
        sortWeight = 2
    ),
    ZOMBIE_MODE(
        label = "15% Zombie Mode",
        batteryPercent = 15,
        description = "Low-friction video review, flashcards & notes",
        sortWeight = 1
    )
}

enum class ConfidenceRating(
    val title: String,
    val subtitle: String,
    val intervalsInDays: List<Int>
) {
    BRUTAL(
        title = "Brutal (Hard)",
        subtitle = "Echoes at +2d, +5d, and +14d",
        intervalsInDays = listOf(2, 5, 14)
    ),
    MEDIUM(
        title = "Medium (Good)",
        subtitle = "Echoes at +3d, +7d, and +21d",
        intervalsInDays = listOf(3, 7, 21)
    ),
    EASY(
        title = "Easy (Mastered)",
        subtitle = "Echoes at +5d, +14d, and +30d",
        intervalsInDays = listOf(5, 14, 30)
    )
}

enum class NeuroDrillType(
    val title: String,
    val shortTitle: String,
    val tag: String,
    val mechanismSummary: String,
    val metricUnit: String
) {
    FOVEAL_LOCK(
        title = "Foveal Singularity Lock",
        shortTitle = "Point-Staring Lock",
        tag = "FOCUS RESTORATION",
        mechanismSummary = "Suppresses saccadic eye drift & Default Mode Network daydreaming via thumb-anchored central fixation.",
        metricUnit = "sec"
    ),
    SCHULTE_GRID(
        title = "5x5 Schulte Speed Grid",
        shortTitle = "5x5 Schulte Grid",
        tag = "PERIPHERAL SPEED",
        mechanismSummary = "Expands peripheral visual span and accelerates prefrontal-visual search velocity.",
        metricUnit = "sec"
    ),
    QUANT_REFLEX(
        title = "Quant-Reflex Velocity Blitz",
        shortTitle = "Quant-Reflex Blitz",
        tag = "SYNAPSE VELOCITY",
        mechanismSummary = "High-speed arithmetic, inequality & power evaluation to prime Physics/Math problem solving.",
        metricUnit = "QPM"
    ),
    SPATIAL_N_BACK(
        title = "Spatial N-Back Matrix",
        shortTitle = "Spatial N-Back",
        tag = "WORKING MEMORY",
        mechanismSummary = "Gold-standard prefrontal working-memory overload protocol (1-Back, 2-Back, 3-Back).",
        metricUnit = "%"
    ),
    CHIMP_MEMORY(
        title = "Chimp Flash Memory Vault",
        shortTitle = "Chimp Flash Vault",
        tag = "SPATIAL ENCODING",
        mechanismSummary = "Trains instantaneous photographic spatial encoding and masked sequential recall.",
        metricUnit = "Lvl"
    ),
    OCULOMOTOR_SIGH(
        title = "Oculomotor Figure-8 & Sigh",
        shortTitle = "Figure-8 & Sigh",
        tag = "STRESS & FOG RESET",
        mechanismSummary = "Combines smooth-pursuit Lemniscate eye tracking with the double-inhale Physiological Sigh.",
        metricUnit = "sec"
    )
}

enum class CognitiveProblemCategory(
    val badgeEmoji: String,
    val symptomTitle: String,
    val prescriptionNote: String,
    val recommendedDrills: List<NeuroDrillType>
) {
    LOST_FOCUS(
        badgeEmoji = "🎯",
        symptomTitle = "I Lost Focus & My Mind Is Wandering",
        prescriptionNote = "Prescribed: Lock foveal gaze for 60s to silence daydreaming and trigger brainstem norepinephrine.",
        recommendedDrills = listOf(NeuroDrillType.FOVEAL_LOCK, NeuroDrillType.OCULOMOTOR_SIGH)
    ),
    SLOW_BRAIN_SPEED(
        badgeEmoji = "⚡",
        symptomTitle = "My Brain Speed Is Slow & Sluggish",
        prescriptionNote = "Prescribed: Accelerate visual-motor & quantitative synapse firing with high-velocity drills.",
        recommendedDrills = listOf(NeuroDrillType.SCHULTE_GRID, NeuroDrillType.QUANT_REFLEX)
    ),
    BUILD_BRAIN_MUSCLE(
        badgeEmoji = "💪",
        symptomTitle = "Strengthen My Brain Muscle (Working Memory)",
        prescriptionNote = "Prescribed: Overload dorsolateral prefrontal working memory buffers with N-Back & Masked Recall.",
        recommendedDrills = listOf(NeuroDrillType.SPATIAL_N_BACK, NeuroDrillType.CHIMP_MEMORY)
    ),
    BRAIN_FOG_STRESS(
        badgeEmoji = "🌊",
        symptomTitle = "Brain Fog, Stress, or Syllabus Overwhelm",
        prescriptionNote = "Prescribed: Decelerate heart rate via double-inhale Physiological Sigh + Figure-8 ocular pursuit.",
        recommendedDrills = listOf(NeuroDrillType.OCULOMOTOR_SIGH, NeuroDrillType.FOVEAL_LOCK)
    )
}
