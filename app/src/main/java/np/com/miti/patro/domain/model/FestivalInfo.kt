package np.com.miti.patro.domain.model

data class FestivalInfo(
    val nameEn: String,
    val nameNe: String,
    val isPublicHoliday: Boolean,
    val isLunar: Boolean
) {
    val englishTitle: String get() = nameEn
    val nepaliTitle: String get() = nameNe
}
