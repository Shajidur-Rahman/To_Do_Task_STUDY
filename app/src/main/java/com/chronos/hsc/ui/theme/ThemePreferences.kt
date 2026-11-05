package com.chronos.hsc.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, AppThemeMode.BLACK_LOVER.name) ?: AppThemeMode.BLACK_LOVER.name)
        } catch (_: Exception) {
            AppThemeMode.BLACK_LOVER
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _critterType = MutableStateFlow(
        try {
            NatureCritterType.valueOf(prefs.getString(KEY_CRITTER_TYPE, NatureCritterType.POKA_INCHWORM.name) ?: NatureCritterType.POKA_INCHWORM.name)
        } catch (_: Exception) {
            NatureCritterType.POKA_INCHWORM
        }
    )
    val critterType: StateFlow<NatureCritterType> = _critterType.asStateFlow()

    private val _foliageDensity = MutableStateFlow(
        try {
            FoliageDensity.valueOf(prefs.getString(KEY_FOLIAGE_DENSITY, FoliageDensity.LUSH.name) ?: FoliageDensity.LUSH.name)
        } catch (_: Exception) {
            FoliageDensity.LUSH
        }
    )
    val foliageDensity: StateFlow<FoliageDensity> = _foliageDensity.asStateFlow()

    private val _compactHeader = MutableStateFlow(
        prefs.getBoolean(KEY_COMPACT_HEADER, true)
    )
    val compactHeader: StateFlow<Boolean> = _compactHeader.asStateFlow()

    private val _ambientAnimationsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AMBIENT_ANIMATIONS, true)
    )
    val ambientAnimationsEnabled: StateFlow<Boolean> = _ambientAnimationsEnabled.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setCritterType(critter: NatureCritterType) {
        prefs.edit().putString(KEY_CRITTER_TYPE, critter.name).apply()
        _critterType.value = critter
    }

    fun setFoliageDensity(density: FoliageDensity) {
        prefs.edit().putString(KEY_FOLIAGE_DENSITY, density.name).apply()
        _foliageDensity.value = density
    }

    fun setCompactHeader(compact: Boolean) {
        prefs.edit().putBoolean(KEY_COMPACT_HEADER, compact).apply()
        _compactHeader.value = compact
    }

    fun setAmbientAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMBIENT_ANIMATIONS, enabled).apply()
        _ambientAnimationsEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "chronos_theme_preferences"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_CRITTER_TYPE = "key_critter_type"
        private const val KEY_FOLIAGE_DENSITY = "key_foliage_density"
        private const val KEY_COMPACT_HEADER = "key_compact_header"
        private const val KEY_AMBIENT_ANIMATIONS = "key_ambient_animations"

        @Volatile
        private var instance: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferences(context).also { instance = it }
            }
        }
    }
}
