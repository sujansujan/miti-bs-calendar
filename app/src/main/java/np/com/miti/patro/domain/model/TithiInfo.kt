package np.com.miti.patro.domain.model

import androidx.compose.runtime.Immutable

enum class Paksha(
    val englishName: String,
    val nepaliName: String,
    val shortCode: String
) {
    SHUKLA("Shukla Paksha", "शुक्ल पक्ष", "S"),
    KRISHNA("Krishna Paksha", "कृष्ण पक्ष", "K")
}

/**
 * Represents the lunar Tithi (1..30) active at sunrise in Nepal Standard Time (UTC+05:45),
 * along with Tithi Kshaya (2 Tithis on the same solar day), Vriddhi (1 Tithi spanning 2 sunrises),
 * and true Amanta Lunar Month (Chandra Maas 1..12 & Adhik Maas / Malamas status).
 */
@Immutable
data class TithiInfo(
    val number: Int,
    val paksha: Paksha,
    val pakshaDay: Int,
    val englishName: String,
    val nepaliName: String,
    val shortLabel: String,
    val shortLabelNe: String,
    val phaseSymbol: String,
    val kshayaTithiNumber: Int? = null,
    val kshayaEnglishName: String? = null,
    val kshayaNepaliName: String? = null,
    val kshayaShortLabel: String? = null,
    val kshayaShortLabelNe: String? = null,
    val isVriddhiSecondDay: Boolean = false,
    val aparahnaTithiNumber: Int = number,
    val shuddhaLunarMonth: Int = 1,
    val isAdhikMaas: Boolean = false,
    val kshayaTithiLunarMonth: Int? = null,
    val kshayaTithiIsAdhikMaas: Boolean = false,
    val aparahnaLunarMonth: Int = shuddhaLunarMonth,
    val aparahnaIsAdhikMaas: Boolean = isAdhikMaas,
    val pradoshTithiNumber: Int = number,
    val nishitaTithiNumber: Int = number,
    val kshayaLunarMonth: Int? = null
) {
    val isPurnima: Boolean get() = number == 15 || kshayaTithiNumber == 15
    val isAunsi: Boolean get() = number == 30 || kshayaTithiNumber == 30
    val isEkadashi: Boolean get() = pakshaDay == 11 || (kshayaTithiNumber == 11 || kshayaTithiNumber == 26)
    val hasKshaya: Boolean get() = kshayaTithiNumber != null

    val nameEn: String get() = englishName
    val nameNe: String get() = nepaliName
    val shortLabelEn: String get() = shortLabel

    fun gridLabel(useDevanagari: Boolean): String {
        return if (useDevanagari) {
            if (kshayaShortLabelNe != null) "$shortLabelNe/$kshayaShortLabelNe" else shortLabelNe
        } else {
            if (kshayaShortLabel != null) "${shortLabel.take(3)}/${kshayaShortLabel.take(2)}" else shortLabel
        }
    }

    fun stripDisplay(useDevanagari: Boolean): String {
        val kPaksha = kshayaTithiNumber?.let { if (it <= 15) Paksha.SHUKLA else Paksha.KRISHNA }
        return if (useDevanagari) {
            val base = "$phaseSymbol $nepaliName"
            if (kPaksha != null && kPaksha != paksha && kshayaNepaliName != null) {
                val p1 = if (isAdhikMaas) "अधिक मास • ${paksha.nepaliName}" else paksha.nepaliName
                val p2 = if (kshayaTithiIsAdhikMaas) "अधिक मास • ${kPaksha.nepaliName}" else kPaksha.nepaliName
                "$base ($p1) + $kshayaNepaliName ($p2, तिथि क्षय)"
            } else {
                val withKshaya = if (kshayaNepaliName != null) "$base + $kshayaNepaliName (तिथि क्षय)" else base
                val pakshaStr = if (isAdhikMaas) "अधिक मास • ${paksha.nepaliName}" else paksha.nepaliName
                "$withKshaya ($pakshaStr)"
            }
        } else {
            val base = "$phaseSymbol $englishName"
            if (kPaksha != null && kPaksha != paksha && kshayaEnglishName != null) {
                val p1 = if (isAdhikMaas) "Adhik • ${paksha.shortCode}" else paksha.shortCode
                val p2 = if (kshayaTithiIsAdhikMaas) "Adhik • ${kPaksha.shortCode}" else kPaksha.shortCode
                "$base ($p1) + $kshayaEnglishName ($p2, Kshaya)"
            } else {
                val withKshaya = if (kshayaEnglishName != null) "$base + $kshayaEnglishName (Kshaya)" else base
                val pakshaStr = if (isAdhikMaas) "Adhik • ${paksha.shortCode}" else paksha.shortCode
                "$withKshaya ($pakshaStr)"
            }
        }
    }
}
