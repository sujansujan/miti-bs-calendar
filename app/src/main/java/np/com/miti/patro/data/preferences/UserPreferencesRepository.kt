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
import kotlinx.coroutines.flow.map
import np.com.miti.patro.domain.model.MissingDayStrategy

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "miti_preferences")

enum class ThemeMode(val code: String, val labelEn: String, val labelNe: String) {
    SYSTEM("SYSTEM", "System (Alucard Light / Dracula Dark)", "प्रणाली अनुसार (आलुकार्ड उज्यालो / ड्राकुला अँध्यारो)"),
    ALUCARD_LIGHT("ALUCARD", "Alucard (Light)", "आलुकार्ड (उज्यालो)"),
    DRACULA_DARK("DRACULA", "Dracula (Dark)", "ड्राकुला (अँध्यारो)");

    fun label(nepali: Boolean): String = if (nepali) labelNe else labelEn

    companion object {
        fun fromCode(code: String): ThemeMode = entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useNepaliScript: Boolean = false,
    val showGregorianSubDate: Boolean = true,
    val showTithiInGrid: Boolean = true,
    val defaultMissingDayStrategy: MissingDayStrategy = MissingDayStrategy.CLAMP_TO_LAST_DAY,
    val defaultReminderHour: Int = 7,
    val defaultReminderMinute: Int = 0,
    val syncToGoogleCalendar: Boolean = false,
    val googleSyncYearsAhead: Int = 5
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_NEPALI_SCRIPT = booleanPreferencesKey("use_nepali_script")
        val SHOW_GREGORIAN_SUB = booleanPreferencesKey("show_gregorian_sub_date")
        val SHOW_TITHI_IN_GRID = booleanPreferencesKey("show_tithi_in_grid")
        val DEFAULT_MISSING_STRATEGY = stringPreferencesKey("default_missing_day_strategy")
        val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour")
        val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute")
        val SYNC_TO_GOOGLE_CALENDAR = booleanPreferencesKey("sync_to_google_calendar")
        val GOOGLE_SYNC_YEARS_AHEAD = intPreferencesKey("google_sync_years_ahead")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = ThemeMode.fromCode(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.code),
            useNepaliScript = prefs[Keys.USE_NEPALI_SCRIPT] ?: false,
            showGregorianSubDate = prefs[Keys.SHOW_GREGORIAN_SUB] ?: true,
            showTithiInGrid = prefs[Keys.SHOW_TITHI_IN_GRID] ?: true,
            defaultMissingDayStrategy = MissingDayStrategy.fromCode(
                prefs[Keys.DEFAULT_MISSING_STRATEGY] ?: MissingDayStrategy.CLAMP_TO_LAST_DAY.code
            ),
            defaultReminderHour = prefs[Keys.DEFAULT_REMINDER_HOUR] ?: 7,
            defaultReminderMinute = prefs[Keys.DEFAULT_REMINDER_MINUTE] ?: 0,
            syncToGoogleCalendar = prefs[Keys.SYNC_TO_GOOGLE_CALENDAR] ?: false,
            googleSyncYearsAhead = prefs[Keys.GOOGLE_SYNC_YEARS_AHEAD] ?: 5
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.code }
    }

    suspend fun setUseNepaliScript(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_NEPALI_SCRIPT] = enabled }
    }

    suspend fun setShowGregorianSubDate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_GREGORIAN_SUB] = enabled }
    }

    suspend fun setShowTithiInGrid(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TITHI_IN_GRID] = enabled }
    }

    suspend fun setDefaultMissingDayStrategy(strategy: MissingDayStrategy) {
        context.dataStore.edit { it[Keys.DEFAULT_MISSING_STRATEGY] = strategy.code }
    }

    suspend fun setDefaultReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.DEFAULT_REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.DEFAULT_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setSyncToGoogleCalendar(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SYNC_TO_GOOGLE_CALENDAR] = enabled }
    }

    suspend fun setGoogleSyncYearsAhead(years: Int) {
        context.dataStore.edit { it[Keys.GOOGLE_SYNC_YEARS_AHEAD] = years.coerceIn(1, 15) }
    }
}
