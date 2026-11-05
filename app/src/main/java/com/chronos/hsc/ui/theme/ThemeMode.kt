package com.chronos.hsc.ui.theme

enum class AppThemeMode(val title: String, val subtitle: String) {
    BLACK_LOVER(
        title = "Black Lover",
        subtitle = "Cyberpunk Obsidian, Neon Cyan & Electric Purple"
    ),
    NATURE_LOVER(
        title = "Nature Lover",
        subtitle = "Living Terrarium, Swaying Ferns & Crawling Critters (Poka)"
    )
}

enum class NatureCritterType(val displayName: String, val emoji: String) {
    POKA_INCHWORM("Poka (Inchworm)", "🐛"),
    FIREFLY("Jonaki (Firefly)", "✨"),
    LADYBUG("Ladybug (Gojopoka)", "🐞"),
    HONEYBEE("Honeybee (Moumachhi)", "🐝")
}

enum class FoliageDensity(val displayName: String, val particleCount: Int) {
    LOW("Minimal (Battery Saver)", 6),
    MEDIUM("Balanced", 12),
    LUSH("Lush Canopy (Max Nature)", 18)
}
