package np.com.miti.patro.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import np.com.miti.patro.domain.model.EventColor

/**
 * Room Entity storing a repeating Bikram Sambat event or Lunar Shraddha Tithi event.
 */
@Immutable
@Entity(
    tableName = "bs_events",
    indices = [Index(value = ["bsMonth", "bsDay"])]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val bsMonth: Int,
    val bsDay: Int,
    val startBsYear: Int,
    val isPersonalHoliday: Boolean = false,
    val colorKey: String = EventColor.GREEN.key,
    val isShraddha: Boolean = false,
    val shraddhaTithi: Int = 0,
    val isRepeating: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 0,
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val notes: String get() = note
    val originBsYear: Int get() = startBsYear
    val resolvedColor: EventColor
        get() = EventColor.fromKey(colorKey, fallbackPersonalHoliday = isPersonalHoliday)
}
