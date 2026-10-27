package np.com.miti.patro.domain.model

/**
 * Defines how an annual BS-repeating event is resolved when its BS day
 * (e.g., 30, 31, or 32) does not exist in a particular upcoming BS year's month.
 */
enum class MissingDayStrategy(
    val key: String,
    val labelEn: String,
    val descriptionEn: String
) {
    CLAMP_TO_LAST_DAY(
        key = "CLAMP_TO_LAST_DAY",
        labelEn = "Clamp to month end",
        descriptionEn = "Trigger on the last day of the same BS month (e.g., day 32 -> 31)"
    ),
    ROLL_TO_NEXT_MONTH(
        key = "ROLL_TO_NEXT_MONTH",
        labelEn = "Roll to next month",
        descriptionEn = "Overflow into day 1 (or 2) of the following BS month"
    ),
    SKIP_YEAR(
        key = "SKIP_YEAR",
        labelEn = "Exact day only",
        descriptionEn = "Skip years where that BS month has fewer days"
    );

    companion object {
        fun fromKey(key: String?): MissingDayStrategy =
            entries.firstOrNull { it.key == key } ?: CLAMP_TO_LAST_DAY
    }
}

/**
 * Result of resolving an annual BS event in a specific target BS year.
 */
data class ResolvedBsOccurrence(
    val targetDate: BsDate,
    val originalDay: Int,
    val isClampedOrRolled: Boolean
)
