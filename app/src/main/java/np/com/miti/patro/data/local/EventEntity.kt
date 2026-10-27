package np.com.miti.patro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bs_repeating_events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val bsMonth: Int,                  // 1..12 (Baisakh..Chaitra)
    val bsDay: Int,                    // 1..32
    val startBsYear: Int,              // BS year when the event was created/anchored
    val isPersonalHoliday: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 0,
    val reminderHour: Int = 7,
    val reminderMinute: Int = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
