package np.com.miti.patro.domain.model

enum class Paksha(
    val englishName: String,
    val nepaliName: String,
    val shortCode: String,
    val symbol: String
) {
    SHUKLA("Shukla Paksha", "शुक्ल पक्ष", "S", "◐"),
    KRISHNA("Krishna Paksha", "कृष्ण पक्ष", "K", "◑")
}

/**
 * Represents the lunar Tithi (1..30) prevailing at sunrise in Nepal (UTC+05:45).
 */
data class TithiInfo(
    val number: Int,          // 1..30 across synodic month
    val pakshaDay: Int,       // 1..15 within Paksha
    val paksha: Paksha,
    val englishName: String,
    val nepaliName: String,
    val shortLabelEn: String,
    val shortLabelNe: String,
    val elongationDeg: Double
) {
    val isPurnima: Boolean get() = number == 15
    val isAunsi: Boolean get() = number == 30
    val isEkadashi: Boolean get() = pakshaDay == 11
    val shortLabel: String get() = shortLabelEn

    val phaseSymbol: String
        get() = when {
            isPurnima -> "○"
            isAunsi -> "●"
            paksha == Paksha.SHUKLA -> "◐"
            else -> "◑"
        }
}
