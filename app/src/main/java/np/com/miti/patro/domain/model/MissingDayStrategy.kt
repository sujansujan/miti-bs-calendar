package np.com.miti.patro.domain.model

/**
 * Defines how an annually repeating BS event behaves when its day (e.g., 30, 31, or 32)
 * does not exist in that BS month during a specific target year.
 */
enum class MissingDayStrategy(
    val code: String,
    val labelEn: String,
    val labelNe: String,
    val descriptionEn: String,
    val descriptionNe: String
) {
    CLAMP_TO_LAST_DAY(
        code = "CLAMP",
        labelEn = "Clamp to last day of month",
        labelNe = "महिनाको अन्तिम दिनमा सार्ने",
        descriptionEn = "Trigger on the final day of the BS month (e.g., Day 32 -> Day 31 or 30)",
        descriptionNe = "महिना छोटो भएमा सोही महिनाको अन्तिम दिनमा मनाउने (जस्तै: ३२ गते -> ३१ वा ३० गते)"
    ),
    ROLL_TO_NEXT_MONTH(
        code = "ROLL",
        labelEn = "Roll to 1st of next month",
        labelNe = "अर्को महिनाको १ गते सार्ने",
        descriptionEn = "Trigger on the 1st day of the following BS month",
        descriptionNe = "महिना छोटो भएमा अर्को महिनाको १ गते सार्ने"
    ),
    SKIP_YEAR(
        code = "SKIP",
        labelEn = "Skip years without exact day",
        labelNe = "गते नभएको वर्ष छोड्ने",
        descriptionEn = "Only trigger in BS years where the exact day number exists",
        descriptionNe = "जुन वर्ष सोही गते पर्छ, त्यही वर्ष मात्र देखाउने"
    );

    fun label(nepali: Boolean): String = if (nepali) labelNe else labelEn
    fun description(nepali: Boolean): String = if (nepali) descriptionNe else descriptionEn

    companion object {
        fun fromCode(code: String): MissingDayStrategy =
            entries.firstOrNull { it.code == code } ?: CLAMP_TO_LAST_DAY
    }
}
