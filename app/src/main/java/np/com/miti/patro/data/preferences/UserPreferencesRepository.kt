package np.com.miti.patro.data.preferences

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import np.com.miti.patro.domain.model.MissingDayStrategy

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "miti_user_settings")

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("SYSTEM", "System Auto"),
    ALUCARD("ALUCARD", "Alucard (Lokta Light)"),
    DRACULA("DRACULA", "Dracula (Dark)");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.find { it.key == key } ?: SYSTEM
    }
}

enum class NepaliScriptMode(val key: String, val labelEn: String, val labelNe: String) {
    ENGLISH("ENGLISH", "English", "अंग्रेजी"),
    CALENDAR_TAB_ONLY("CALENDAR_TAB_ONLY", "Calendar Tab Only", "पात्रो मात्र"),
    EVERYWHERE("EVERYWHERE", "Whole App (नेपाली)", "पूरै एप (नेपाली)");

    companion object {
        fun fromKey(key: String?): NepaliScriptMode = entries.find { it.key == key } ?: ENGLISH
    }
}

@Immutable
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
    val useDevanagari: Boolean
        get() = nepaliScriptMode != NepaliScriptMode.ENGLISH
    val useDevanagariEverywhere: Boolean
        get() = nepaliScriptMode == NepaliScriptMode.EVERYWHERE
    val showGregorianInGrid: Boolean get() = showGregorianSubDate
    val defaultMissingDayStrategy: MissingDayStrategy get() = missingDayStrategy
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MISSING_DAY_STRATEGY = stringPreferencesKey("missing_day_strategy")
        val NEPALI_SCRIPT_MODE = stringPreferencesKey("nepali_script_mode")
        val LEGACY_USE_DEVANAGARI = booleanPreferencesKey("use_devanagari_numerals")
        val SHOW_GREGORIAN_SUB = booleanPreferencesKey("show_gregorian_sub_date")
        val SHOW_TITHI_IN_GRID = booleanPreferencesKey("show_tithi_in_grid")
        val SHOW_PUBLIC_FESTIVALS = booleanPreferencesKey("show_public_festivals")
        val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour")
        val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute")
        val DEFAULT_REMINDER_DAYS_BEFORE = intPreferencesKey("default_reminder_days_before")
        val SYNC_TO_GOOGLE_CALENDAR = booleanPreferencesKey("sync_to_google_calendar")
        val GOOGLE_SYNC_YEARS_AHEAD = intPreferencesKey("google_sync_years_ahead")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val rawScriptMode = prefs[Keys.NEPALI_SCRIPT_MODE]
        val resolvedScriptMode = if (rawScriptMode != null) {
            NepaliScriptMode.fromKey(rawScriptMode)
        } else if (prefs[Keys.LEGACY_USE_DEVANAGARI] == true) {
            NepaliScriptMode.EVERYWHERE
        } else {
            NepaliScriptMode.ENGLISH
        }

        UserSettings(
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            missingDayStrategy = MissingDayStrategy.fromKey(prefs[Keys.MISSING_DAY_STRATEGY]),
            nepaliScriptMode = resolvedScriptMode,
            showGregorianSubDate = prefs[Keys.SHOW_GREGORIAN_SUB] ?: true,
            showTithiInGrid = prefs[Keys.SHOW_TITHI_IN_GRID] ?: true,
            showPublicFestivals = prefs[Keys.SHOW_PUBLIC_FESTIVALS] ?: true,
            defaultReminderHour = prefs[Keys.DEFAULT_REMINDER_HOUR] ?: 7,
            defaultReminderMinute = prefs[Keys.DEFAULT_REMINDER_MINUTE] ?: 0,
            defaultReminderDaysBefore = prefs[Keys.DEFAULT_REMINDER_DAYS_BEFORE] ?: 0,
            syncToGoogleCalendar = prefs[Keys.SYNC_TO_GOOGLE_CALENDAR] ?: false,
            googleSyncYearsAhead = (prefs[Keys.GOOGLE_SYNC_YEARS_AHEAD] ?: 15).coerceIn(1, 50)
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setMissingDayStrategy(strategy: MissingDayStrategy) {
        context.dataStore.edit { it[Keys.MISSING_DAY_STRATEGY] = strategy.key }
    }

    suspend fun setNepaliScriptMode(mode: NepaliScriptMode) {
        context.dataStore.edit { it[Keys.NEPALI_SCRIPT_MODE] = mode.key }
    }

    suspend fun setShowGregorianSubDate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_GREGORIAN_SUB] = enabled }
    }

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

    suspend fun setDefaultReminderDaysBefore(daysBefore: Int) {
        context.dataStore.edit {
            it[Keys.DEFAULT_REMINDER_DAYS_BEFORE] = daysBefore.coerceIn(0, 30)
        }
    }

    suspend fun setSyncToGoogleCalendar(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SYNC_TO_GOOGLE_CALENDAR] = enabled }
    }

    suspend fun setGoogleSyncYearsAhead(years: Int) {
        context.dataStore.edit { it[Keys.GOOGLE_SYNC_YEARS_AHEAD] = years.coerceIn(1, 50) }
    }

    suspend fun restoreAllSettings(settings: UserSettings) {
        context.dataStore.edit {
            it[Keys.THEME_MODE] = settings.themeMode.key
            it[Keys.MISSING_DAY_STRATEGY] = settings.missingDayStrategy.key
            it[Keys.NEPALI_SCRIPT_MODE] = settings.nepaliScriptMode.key
            it[Keys.SHOW_GREGORIAN_SUB] = settings.showGregorianSubDate
            it[Keys.SHOW_TITHI_IN_GRID] = settings.showTithiInGrid
            it[Keys.SHOW_PUBLIC_FESTIVALS] = settings.showPublicFestivals
            it[Keys.DEFAULT_REMINDER_HOUR] = settings.defaultReminderHour.coerceIn(0, 23)
            it[Keys.DEFAULT_REMINDER_MINUTE] = settings.defaultReminderMinute.coerceIn(0, 59)
            it[Keys.DEFAULT_REMINDER_DAYS_BEFORE] = settings.defaultReminderDaysBefore.coerceIn(0, 30)
            it[Keys.SYNC_TO_GOOGLE_CALENDAR] = settings.syncToGoogleCalendar
            it[Keys.GOOGLE_SYNC_YEARS_AHEAD] = settings.googleSyncYearsAhead.coerceIn(1, 50)
        }
    }
}
