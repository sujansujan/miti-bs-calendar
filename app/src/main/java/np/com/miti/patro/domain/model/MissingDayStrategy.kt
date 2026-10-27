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
        labelEn = "Clamp to month end",
        labelNe = "महिनाको अन्तिम दिनमा सार्ने",
        descriptionEn = "Trigger on the last day of the same BS month (e.g., day 32 -> 31)",
        descriptionNe = "महिना छोटो भएमा सोही महिनाको अन्तिम दिनमा मनाउने (जस्तै: ३२ गते -> ३१ वा ३० गते)"
    ),
    ROLL_TO_NEXT_MONTH(
        key = "ROLL_TO_NEXT_MONTH",
        labelEn = "Roll to next month",
        labelNe = "अर्को महिनाको १ गते सार्ने",
        descriptionEn = "Overflow into day 1 (or 2) of the following BS month",
        descriptionNe = "महिना छोटो भएमा अर्को महिनाको १ गते सार्ने"
    ),
    SKIP_YEAR(
        key = "SKIP_YEAR",
        labelEn = "Exact day only",
        labelNe = "गते नभएको वर्ष छोड्ने",
        descriptionEn = "Skip years where that BS month has fewer days",
        descriptionNe = "जुन वर्ष सोही गते पर्छ, त्यही वर्ष मात्र देखाउने"
    );

    fun label(useDevanagari: Boolean): String = if (useDevanagari) labelNe else labelEn
    fun description(useDevanagari: Boolean): String = if (useDevanagari) descriptionNe else descriptionEn

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
