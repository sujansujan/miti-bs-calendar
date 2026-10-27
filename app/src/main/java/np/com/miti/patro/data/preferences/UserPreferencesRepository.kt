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

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("SYSTEM", "System (Alucard Light / Dracula Dark)"),
    ALUCARD("ALUCARD", "Alucard (Light)"),
    DRACULA("DRACULA", "Dracula (Dark)");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val missingDayStrategy: MissingDayStrategy = MissingDayStrategy.CLAMP_TO_LAST_DAY,
    val useDevanagari: Boolean = false,
    val showGregorianSubDate: Boolean = true,
    val showTithiInGrid: Boolean = true,
    val defaultReminderHour: Int = 7,
    val defaultReminderMinute: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MISSING_DAY_STRATEGY = stringPreferencesKey("missing_day_strategy")
        val USE_DEVANAGARI = booleanPreferencesKey("use_devanagari")
        val SHOW_GREGORIAN_SUB_DATE = booleanPreferencesKey("show_gregorian_sub_date")
        val SHOW_TITHI_IN_GRID = booleanPreferencesKey("show_tithi_in_grid")
        val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour")
        val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            missingDayStrategy = MissingDayStrategy.fromKey(prefs[Keys.MISSING_DAY_STRATEGY]),
            useDevanagari = prefs[Keys.USE_DEVANAGARI] ?: false,
            showGregorianSubDate = prefs[Keys.SHOW_GREGORIAN_SUB_DATE] ?: true,
            showTithiInGrid = prefs[Keys.SHOW_TITHI_IN_GRID] ?: true,
            defaultReminderHour = prefs[Keys.DEFAULT_REMINDER_HOUR] ?: 7,
            defaultReminderMinute = prefs[Keys.DEFAULT_REMINDER_MINUTE] ?: 0
        )
    }

    suspend fun getSettingsOnce(): UserSettings = settingsFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setMissingDayStrategy(strategy: MissingDayStrategy) {
        context.dataStore.edit { it[Keys.MISSING_DAY_STRATEGY] = strategy.key }
    }

    suspend fun setUseDevanagari(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_DEVANAGARI] = enabled }
    }

    suspend fun setShowGregorianSubDate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_GREGORIAN_SUB_DATE] = enabled }
    }

    suspend fun setShowTithiInGrid(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TITHI_IN_GRID] = enabled }
    }

    suspend fun setDefaultReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.DEFAULT_REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.DEFAULT_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
