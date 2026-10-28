package np.com.miti.patro.backup

import android.content.Context
import android.net.Uri
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.data.preferences.NepaliScriptMode
import np.com.miti.patro.data.preferences.ThemeMode
import np.com.miti.patro.data.preferences.UserSettings
import np.com.miti.patro.domain.model.EventColor
import np.com.miti.patro.domain.model.MissingDayStrategy
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Handles 100% offline JSON export and import of all Miti user settings and repeating BS events
 * via Android's Storage Access Framework (with strict size and count limits to prevent OOM / AlarmManager DoS).
 */
object BackupManager {

    private const val BACKUP_SCHEMA_VERSION = 1
    private const val MAX_BACKUP_CHARS = 1_048_576 // 1 MB character cap
    private const val MAX_IMPORTED_EVENTS = 300    // Safe cap below Android's 500 concurrent alarm limit
    private const val MAX_TITLE_LENGTH = 100
    private const val MAX_NOTE_LENGTH = 500

    data class BackupPayload(
        val settings: UserSettings,
        val events: List<EventEntity>
    )

    fun serializeToJson(settings: UserSettings, events: List<EventEntity>): String {
        val root = JSONObject()
        root.put("app", "Miti BS Calendar")
        root.put("schemaVersion", BACKUP_SCHEMA_VERSION)
        root.put("exportedAtEpochMillis", System.currentTimeMillis())

        val settingsObj = JSONObject().apply {
            put("themeMode", settings.themeMode.key)
            put("missingDayStrategy", settings.missingDayStrategy.key)
            put("nepaliScriptMode", settings.nepaliScriptMode.key)
            put("showGregorianSubDate", settings.showGregorianSubDate)
            put("showTithiInGrid", settings.showTithiInGrid)
            put("showPublicFestivals", settings.showPublicFestivals)
            put("defaultReminderHour", settings.defaultReminderHour)
            put("defaultReminderMinute", settings.defaultReminderMinute)
            put("defaultReminderDaysBefore", settings.defaultReminderDaysBefore)
            put("syncToGoogleCalendar", settings.syncToGoogleCalendar)
            put("googleSyncYearsAhead", settings.googleSyncYearsAhead)
        }
        root.put("settings", settingsObj)

        val eventsArr = JSONArray()
        for (ev in events) {
            val evObj = JSONObject().apply {
                put("title", ev.title.take(MAX_TITLE_LENGTH))
                put("note", ev.note.take(MAX_NOTE_LENGTH))
                put("bsMonth", ev.bsMonth)
                put("bsDay", ev.bsDay)
                put("startBsYear", ev.startBsYear)
                put("isPersonalHoliday", ev.isPersonalHoliday)
                put("colorKey", ev.colorKey)
                put("isShraddha", ev.isShraddha)
                put("shraddhaTithi", ev.shraddhaTithi)
                put("reminderEnabled", ev.reminderEnabled)
                put("reminderDaysBefore", ev.reminderDaysBefore)
                put("reminderHour", ev.reminderHour)
                put("reminderMinute", ev.reminderMinute)
                put("createdAtEpochMillis", ev.createdAtEpochMillis)
            }
            eventsArr.put(evObj)
        }
        root.put("events", eventsArr)

        return root.toString(2)
    }

    fun deserializeFromJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val settingsObj = root.optJSONObject("settings") ?: JSONObject()

        val restoredSettings = UserSettings(
            themeMode = ThemeMode.fromKey(settingsObj.optString("themeMode", ThemeMode.SYSTEM.key)),
            missingDayStrategy = MissingDayStrategy.fromKey(
                settingsObj.optString("missingDayStrategy", MissingDayStrategy.CLAMP_TO_LAST_DAY.key)
            ),
            nepaliScriptMode = NepaliScriptMode.fromKey(
                settingsObj.optString("nepaliScriptMode", NepaliScriptMode.ENGLISH.key)
            ),
            showGregorianSubDate = settingsObj.optBoolean("showGregorianSubDate", true),
            showTithiInGrid = settingsObj.optBoolean("showTithiInGrid", true),
            showPublicFestivals = settingsObj.optBoolean("showPublicFestivals", true),
            defaultReminderHour = settingsObj.optInt("defaultReminderHour", 7).coerceIn(0, 23),
            defaultReminderMinute = settingsObj.optInt("defaultReminderMinute", 0).coerceIn(0, 59),
            defaultReminderDaysBefore = settingsObj.optInt("defaultReminderDaysBefore", 0).coerceIn(0, 30),
            syncToGoogleCalendar = settingsObj.optBoolean("syncToGoogleCalendar", false),
            googleSyncYearsAhead = settingsObj.optInt("googleSyncYearsAhead", 15).coerceIn(1, 50)
        )

        val eventsList = mutableListOf<EventEntity>()
        val eventsArr = root.optJSONArray("events") ?: JSONArray()
        val count = minOf(eventsArr.length(), MAX_IMPORTED_EVENTS)
        for (i in 0 until count) {
            val obj = eventsArr.optJSONObject(i) ?: continue
            val title = obj.optString("title", "").trim().take(MAX_TITLE_LENGTH)
            if (title.isEmpty()) continue
            val note = obj.optString("note", "").trim().take(MAX_NOTE_LENGTH)
            val bsMonth = obj.optInt("bsMonth", 1).coerceIn(1, 12)
            val bsDay = obj.optInt("bsDay", 1).coerceIn(1, 32)
            val startBsYear = obj.optInt("startBsYear", 2081).coerceIn(1970, 2100)
            val isPersonalHoliday = obj.optBoolean("isPersonalHoliday", false)
            val colorKey = EventColor.fromKey(
                obj.optString("colorKey", ""),
                fallbackPersonalHoliday = isPersonalHoliday
            ).key

            eventsList.add(
                EventEntity(
                    id = 0L,
                    title = title,
                    note = note,
                    bsMonth = bsMonth,
                    bsDay = bsDay,
                    startBsYear = startBsYear,
                    isPersonalHoliday = isPersonalHoliday,
                    colorKey = colorKey,
                    isShraddha = obj.optBoolean("isShraddha", false),
                    shraddhaTithi = obj.optInt("shraddhaTithi", 0).coerceIn(0, 30),
                    reminderEnabled = obj.optBoolean("reminderEnabled", true),
                    reminderDaysBefore = obj.optInt("reminderDaysBefore", 0).coerceIn(0, 30),
                    reminderHour = obj.optInt("reminderHour", 7).coerceIn(0, 23),
                    reminderMinute = obj.optInt("reminderMinute", 0).coerceIn(0, 59),
                    createdAtEpochMillis = obj.optLong("createdAtEpochMillis", System.currentTimeMillis())
                )
            )
        }

        return BackupPayload(settings = restoredSettings, events = eventsList)
    }

    fun writeBackupToUri(
        context: Context,
        uri: Uri,
        settings: UserSettings,
        events: List<EventEntity>
    ): Boolean {
        return try {
            val json = serializeToJson(settings, events)
            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                OutputStreamWriter(outStream, Charsets.UTF_8).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): BackupPayload? {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inStream ->
                InputStreamReader(inStream, Charsets.UTF_8).use { reader ->
                    val sb = StringBuilder()
                    val buf = CharArray(4096)
                    var totalRead = 0
                    while (true) {
                        val n = reader.read(buf)
                        if (n == -1) break
                        totalRead += n
                        if (totalRead > MAX_BACKUP_CHARS) return null
                        sb.append(buf, 0, n)
                    }
                    sb.toString()
                }
            } ?: return null
            deserializeFromJson(json)
        } catch (_: Exception) {
            null
        }
    }
}
