package np.com.miti.patro.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Immutable representation of a Bikram Sambat (BS) date.
 *
 * @property year BS year (supported lookup range: 1970..2100)
 * @property month BS month (1 = Baisakh .. 12 = Chaitra)
 * @property day BS day of month (1..32 depending on year and month)
 */
data class BsDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<BsDate> {

    override fun compareTo(other: BsDate): Int {
        if (this.year != other.year) return this.year.compareTo(other.year)
        if (this.month != other.month) return this.month.compareTo(other.month)
        return this.day.compareTo(other.day)
    }

    fun formatIso(): String = "%04d-%02d-%02d".format(year, month, day)
}

/**
 * Complete daily cell model combining BS date, Gregorian equivalent, weekday,
 * lunar Tithi, and any national/cultural festivals or user repeating events.
 */
data class BsCalendarDay(
    val bsDate: BsDate,
    val adDate: LocalDate,
    val dayOfWeek: DayOfWeek,
    val tithi: TithiInfo,
    val festivals: List<FestivalInfo>,
    val isToday: Boolean
) {
    val isSaturday: Boolean
        get() = dayOfWeek == DayOfWeek.SATURDAY

    val isPublicHoliday: Boolean
        get() = isSaturday || festivals.any { it.isPublicHoliday }
}
