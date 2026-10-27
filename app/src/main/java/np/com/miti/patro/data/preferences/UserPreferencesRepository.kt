package np.com.miti.patro.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import np.com.miti.patro.domain.model.MissingDayStrategy

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "miti_settings")

enum class ThemeMode(
    val key: String,
    val labelEn: String,
    val labelNe: String
) {
    SYSTEM(
        key = "SYSTEM",
        labelEn = "System (Alucard Light / Dracula Dark)",
        labelNe = "प्रणाली अनुसार (आलुकार्ड उज्यालो / ड्राकुला अँध्यारो)"
    ),
    ALUCARD(
        key = "ALUCARD",
        labelEn = "Alucard (Light)",
        labelNe = "आलुकार्ड (उज्यालो)"
    ),
    DRACULA(
        key = "DRACULA",
        labelEn = "Dracula (Dark)",
        labelNe = "ड्राकुला (अँध्यारो)"
    );

    val label: String get() = labelEn
    fun label(useDevanagari: Boolean): String = if (useDevanagari) labelNe else labelEn

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/**
 * Controls where Nepali text & Devanagari numerals are used:
 * - ENGLISH: English everywhere
 * - CALENDAR_TAB_ONLY: Nepali inside the Calendar tab (grid, tithis, festivals, day details),
 *   while Settings, Date Converter, and Dialogs remain in English
 * - EVERYWHERE: Nepali across all tabs, dialogs, notifications, and Settings
 */
enum class NepaliScriptMode(
    val key: String,
    val labelEn: String,
    val labelNe: String
) {
    ENGLISH(
        key = "ENGLISH",
        labelEn = "English everywhere",
        labelNe = "अंग्रेजी (सबै ठाउँमा)"
    ),
    CALENDAR_TAB_ONLY(
        key = "CALENDAR_TAB_ONLY",
        labelEn = "Nepali on Calendar tab only",
        labelNe = "पात्रो ट्याबमा मात्र नेपाली (अन्यत्र अंग्रेजी)"
    ),
    EVERYWHERE(
        key = "EVERYWHERE",
        labelEn = "Nepali everywhere (All tabs & Settings)",
        labelNe = "नेपाली (सबै ट्याब र सेटिङहरूमा)"
    );

    fun label(useDevanagari: Boolean): String = if (useDevanagari) labelNe else labelEn

    companion object {
        fun fromKey(key: String?, legacyUseDevanagari: Boolean = false): NepaliScriptMode {
            if (!key.isNullOrBlank()) {
                entries.firstOrNull { it.key == key }?.let { return it }
            }
            return if (legacyUseDevanagari) EVERYWHERE else ENGLISH
        }
    }
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val missingDayStrategy: MissingDayStrategy = MissingDayStrategy.CLAMP_TO_LAST_DAY,
    val nepaliScriptMode: NepaliScriptMode = NepaliScriptMode.ENGLISH,
    val showGregorianSubDate: Boolean = true,
    val showTithiInGrid: Boolean = true,
    val showPublicFestivals: Boolean = true,
    val defaultReminderHour: Int = 7,
    val defaultReminderMinute: Int = 0,
    val defaultReminderDaysBefore: Int = 0,
    val syncToGoogleCalendar: Boolean = false,
    val googleSyncYearsAhead: Int = 15
) {
    val defaultMissingDayStrategy: MissingDayStrategy get() = missingDayStrategy
    val showGregorianInGrid: Boolean get() = showGregorianSubDate

    /** True when the Calendar tab should render in Nepali (either Calendar-tab-only or Everywhere). */
    val useDevanagari: Boolean
        get() = nepaliScriptMode == NepaliScriptMode.CALENDAR_TAB_ONLY ||
            nepaliScriptMode == NepaliScriptMode.EVERYWHERE

    /** True only when the user wants Nepali across Settings, Date Converter, and dialogs too. */
    val useDevanagariEverywhere: Boolean
        get() = nepaliScriptMode == NepaliScriptMode.EVERYWHERE
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MISSING_DAY_STRATEGY = stringPreferencesKey("missing_day_strategy")
        val USE_DEVANAGARI = booleanPreferencesKey("use_devanagari")
        val NEPALI_SCRIPT_MODE = stringPreferencesKey("nepali_script_mode")
        val SHOW_GREGORIAN_SUB_DATE = booleanPreferencesKey("show_gregorian_sub_date")
        val SHOW_TITHI_IN_GRID = booleanPreferencesKey("show_tithi_in_grid")
        val SHOW_PUBLIC_FESTIVALS = booleanPreferencesKey("show_public_festivals")
        val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour")
        val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute")
        val DEFAULT_REMINDER_DAYS_BEFORE = intPreferencesKey("default_reminder_days_before")
        val SYNC_TO_GOOGLE_CALENDAR = booleanPreferencesKey("sync_to_google_calendar")
        val GOOGLE_SYNC_YEARS_AHEAD = intPreferencesKey("google_sync_years_ahead")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val legacyNe = prefs[Keys.USE_DEVANAGARI] ?: false
        val scriptMode = NepaliScriptMode.fromKey(prefs[Keys.NEPALI_SCRIPT_MODE], legacyNe)
        UserSettings(
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            missingDayStrategy = MissingDayStrategy.fromKey(prefs[Keys.MISSING_DAY_STRATEGY]),
            nepaliScriptMode = scriptMode,
            showGregorianSubDate = prefs[Keys.SHOW_GREGORIAN_SUB_DATE] ?: true,
            showTithiInGrid = prefs[Keys.SHOW_TITHI_IN_GRID] ?: true,
            showPublicFestivals = prefs[Keys.SHOW_PUBLIC_FESTIVALS] ?: true,
            defaultReminderHour = prefs[Keys.DEFAULT_REMINDER_HOUR] ?: 7,
            defaultReminderMinute = prefs[Keys.DEFAULT_REMINDER_MINUTE] ?: 0,
            defaultReminderDaysBefore = prefs[Keys.DEFAULT_REMINDER_DAYS_BEFORE] ?: 0,
            syncToGoogleCalendar = prefs[Keys.SYNC_TO_GOOGLE_CALENDAR] ?: false,
            googleSyncYearsAhead = prefs[Keys.GOOGLE_SYNC_YEARS_AHEAD] ?: 15
        )
    }

    val userSettingsFlow: Flow<UserSettings> get() = settingsFlow

    suspend fun getSettingsOnce(): UserSettings = settingsFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setMissingDayStrategy(strategy: MissingDayStrategy) {
        context.dataStore.edit { it[Keys.MISSING_DAY_STRATEGY] = strategy.key }
    }

    suspend fun setDefaultMissingDayStrategy(strategy: MissingDayStrategy) = setMissingDayStrategy(strategy)

    suspend fun setNepaliScriptMode(mode: NepaliScriptMode) {
        context.dataStore.edit {
            it[Keys.NEPALI_SCRIPT_MODE] = mode.key
            it[Keys.USE_DEVANAGARI] = (mode != NepaliScriptMode.ENGLISH)
        }
    }

    suspend fun setUseDevanagari(enabled: Boolean) {
        setNepaliScriptMode(if (enabled) NepaliScriptMode.EVERYWHERE else NepaliScriptMode.ENGLISH)
    }

    suspend fun setShowGregorianSubDate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_GREGORIAN_SUB_DATE] = enabled }
    }

    suspend fun setShowGregorianInGrid(enabled: Boolean) = setShowGregorianSubDate(enabled)

    suspend fun setShowTithiInGrid(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TITHI_IN_GRID] = enabled }
    }

    suspend fun setShowPublicFestivals(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_PUBLIC_FESTIVALS] = enabled }
    }

    suspend fun setDefaultReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.DEFAULT_REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.DEFAULT_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setDefaultReminderDaysBefore(days: Int) {
        context.dataStore.edit {
            it[Keys.DEFAULT_REMINDER_DAYS_BEFORE] = days.coerceIn(0, 30)
        }
    }

    suspend fun setSyncToGoogleCalendar(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SYNC_TO_GOOGLE_CALENDAR] = enabled }
    }

    suspend fun setGoogleSyncYearsAhead(years: Int) {
        context.dataStore.edit { it[Keys.GOOGLE_SYNC_YEARS_AHEAD] = years.coerceIn(1, 50) }
    }

    suspend fun restoreSettings(restored: UserSettings) {
        context.dataStore.edit {
            it[Keys.THEME_MODE] = restored.themeMode.key
            it[Keys.MISSING_DAY_STRATEGY] = restored.missingDayStrategy.key
            it[Keys.NEPALI_SCRIPT_MODE] = restored.nepaliScriptMode.key
            it[Keys.USE_DEVANAGARI] = (restored.nepaliScriptMode != NepaliScriptMode.ENGLISH)
            it[Keys.SHOW_GREGORIAN_SUB_DATE] = restored.showGregorianSubDate
            it[Keys.SHOW_TITHI_IN_GRID] = restored.showTithiInGrid
            it[Keys.SHOW_PUBLIC_FESTIVALS] = restored.showPublicFestivals
            it[Keys.DEFAULT_REMINDER_HOUR] = restored.defaultReminderHour.coerceIn(0, 23)
            it[Keys.DEFAULT_REMINDER_MINUTE] = restored.defaultReminderMinute.coerceIn(0, 59)
            it[Keys.DEFAULT_REMINDER_DAYS_BEFORE] = restored.defaultReminderDaysBefore.coerceIn(0, 30)
            it[Keys.SYNC_TO_GOOGLE_CALENDAR] = restored.syncToGoogleCalendar
            it[Keys.GOOGLE_SYNC_YEARS_AHEAD] = restored.googleSyncYearsAhead.coerceIn(1, 50)
        }
    }
}
