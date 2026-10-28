package np.com.miti.patro.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import np.com.miti.patro.domain.model.EventColor
import np.com.miti.patro.domain.model.MissingDayStrategy

@Immutable
@Entity(tableName = "bs_repeating_events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val bsMonth: Int,                  // 1..12 (Baisakh..Chaitra)
    val bsDay: Int,                    // 1..32 (Anchor day or Gatey)
    val startBsYear: Int,              // BS year when the event was created/anchored
    val isPersonalHoliday: Boolean = false,
    val colorKey: String = EventColor.GREEN.key,
    val isShraddha: Boolean = false,   // True if repeating annually by Lunar Tithi (Shraddha)
    val shraddhaTithi: Int = 0,        // 1..30 (1..15 Shukla, 16..30 Krishna) when isShraddha = true
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 0,
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val notes: String get() = note
    val originBsYear: Int get() = startBsYear
    val missingDayStrategy: MissingDayStrategy get() = MissingDayStrategy.CLAMP_TO_LAST_DAY
    val resolvedColor: EventColor
        get() = EventColor.fromKey(colorKey, fallbackPersonalHoliday = isPersonalHoliday)
}
