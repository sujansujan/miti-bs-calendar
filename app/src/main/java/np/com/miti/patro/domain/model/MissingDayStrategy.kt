package np.com.miti.patro.domain.model

/**
 * Policy for handling repeating BS events created on Day 30, 31, or 32
 * when a future BS year has fewer days in that month (e.g., 29 or 30 days).
 */
enum class MissingDayStrategy(val key: String, val displayLabel: String, val description: String) {
    CLAMP_TO_LAST_DAY(
        key = "CLAMP_TO_LAST_DAY",
        displayLabel = "Clamp to Last Day of Month",
        description = "If Day 31/32 does not exist in target BS year, trigger on the final day (29/30) of that same BS month."
    ),
    ROLL_TO_NEXT_MONTH(
        key = "ROLL_TO_NEXT_MONTH",
        displayLabel = "Roll Over to 1st of Next Month",
        description = "If Day 31/32 is missing, roll the event forward to Day 1 of the next BS month."
    ),
    SKIP_YEAR(
        key = "SKIP_YEAR",
        displayLabel = "Strict Match Only (Skip Missing)",
        description = "Only trigger in years where that exact BS day number exists in the month."
    );

    companion object {
        fun fromKey(key: String?): MissingDayStrategy =
            entries.find { it.key == key } ?: CLAMP_TO_LAST_DAY
    }
}

data class ResolvedBsOccurrence(
    val targetDate: BsDate,
    val wasAdjusted: Boolean,
    val adjustmentNote: String? = null
)
