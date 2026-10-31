package com.chronos.hsc.data.local

enum class HscSubject(
    val fullName: String,
    val shortCode: String,
    val colorHex: Long
) {
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
