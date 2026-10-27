package np.com.miti.patro.domain.model

/**
 * Defines how an annual BS-repeating event is resolved when its BS day
 * (e.g., 30, 31, or 32) does not exist in a particular upcoming BS year's month.
 */
enum class MissingDayStrategy(
    val key: String,
    val labelEn: String,
    val labelNe: String,
    val descriptionEn: String,
    val descriptionNe: String
) {
    CLAMP_TO_LAST_DAY(
        key = "CLAMP_TO_LAST_DAY",
        labelEn = "Clamp to month end (e.g. day 32 -> 31 or 30)",
        labelNe = "महिनाको अन्तिम दिनमा सार्ने (जस्तै: ३२ गते -> ३१ वा ३० गते)",
        descriptionEn = "Trigger on the last day of the same BS month (e.g., day 32 -> 31)",
        descriptionNe = "महिना छोटो भएमा सोही महिनाको अन्तिम दिनमा मनाउने"
    ),
    ROLL_TO_NEXT_MONTH(
        key = "ROLL_TO_NEXT_MONTH",
        labelEn = "Roll to 1st day of next BS month",
        labelNe = "अर्को महिनाको १ गते सार्ने",
        descriptionEn = "Overflow into day 1 (or 2) of the following BS month",
        descriptionNe = "महिना छोटो भएमा अर्को महिनाको १ गते सार्ने"
    ),
    SKIP_YEAR(
        key = "SKIP_YEAR",
        labelEn = "Exact day only (skip short years)",
        labelNe = "सोही गते नभएको वर्ष छोड्ने",
        descriptionEn = "Skip years where that BS month has fewer days",
        descriptionNe = "जुन वर्ष सोही गते पर्छ, त्यही वर्ष मात्र देखाउने"
    );

    fun label(useDevanagari: Boolean): String = if (useDevanagari) labelNe else labelEn
    fun description(useDevanagari: Boolean): String = if (useDevanagari) descriptionNe else descriptionEn

    companion object {
        val LAST_DAY_OF_MONTH: MissingDayStrategy get() = CLAMP_TO_LAST_DAY
        val NEXT_MONTH_FIRST_DAY: MissingDayStrategy get() = ROLL_TO_NEXT_MONTH

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
) {
    val resolvedDate: BsDate get() = targetDate
    val wasAdjusted: Boolean get() = isClampedOrRolled
}
