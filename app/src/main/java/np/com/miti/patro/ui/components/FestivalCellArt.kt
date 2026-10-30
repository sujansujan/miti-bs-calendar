package np.com.miti.patro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import np.com.miti.patro.data.repository.ResolvedEventInstance
import np.com.miti.patro.domain.model.FestivalInfo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Monoline woodblock-style cell artwork motifs for:
 * - Each day of Dashain (Ghatasthapana, Fulpati, Maha Ashtami, Maha Nawami, Vijaya Dashami)
 * - Each day of Tihar (Kaag Tihar, Kukur Tihar, Gai Tihar / Laxmi Puja, Gobardhan / Mha Puja, Bhai Tika)
 * - User Personal Events (Birthday & Shraddha Tithi)
 * - Major Nepali Festivals (Chhath, Maha Shivaratri, Buddha Jayanti, Yomari Punhi, Asar 15 Paddy Day,
 *   Nepali New Year / Lhosar, Teej, Holi, Janai Purnima / Jatra, Maghe Sankranti / Saraswati Puja).
 */
enum class CellArtworkMotif {
    // Dashain (Day-by-Day)
    GHATASTHAPANA,
    FULPATI,
    MAHA_ASHTAMI,
    MAHA_NAWAMI,
    VIJAYA_DASHAMI,

    // Tihar (All 5 Days of Yamapanchak)
    KAAG_TIHAR,
    KUKUR_TIHAR,
    GAI_LAXMI_PUJA,
    MHA_GOBARDHAN_PUJA,
    BHAI_TIKA,

    // Personal Events
    BIRTHDAY,
    SHRADDHA,

    // Major Nepali Festivals
    CHHATH,
    SHIVARATRI,
    BUDDHA_JAYANTI,
    YOMARI_PUNHI,
    ASAR_PADDY,
    NEW_YEAR_LHOSAR,
    TEEJ,
    HOLI,
    JANAI_JATRA,
    MAGHE_SARASWATI
}

object FestivalArtResolver {

    /**
     * Resolves the highest-priority monoline artwork motif for a calendar day cell.
     * Checks personal Shraddha and Birthday events first, then maps specific Dashain days,
     * Tihar days, and major Nepali festivals.
     */
    fun resolveMotif(
        festivals: List<FestivalInfo>,
        events: List<ResolvedEventInstance>,
        showFestivals: Boolean
    ): CellArtworkMotif? {
        // 1. Check user personal events for Shraddha or Birthday
        for (inst in events) {
            val ev = inst.event
            if (ev.isShraddha) return CellArtworkMotif.SHRADDHA
            val tLower = ev.title.lowercase()
            if ("shraddha" in tLower || "shradha" in tLower || "श्राद्ध" in tLower || "तिथि" in tLower) {
                return CellArtworkMotif.SHRADDHA
            }
            if ("birthday" in tLower || "bday" in tLower || "b-day" in tLower ||
                "जन्मदिन" in tLower || "जन्मोत्सव" in tLower || "बर्थडे" in tLower
            ) {
                return CellArtworkMotif.BIRTHDAY
            }
        }

        if (!showFestivals || festivals.isEmpty()) return null

        // 2. Match festivals (Dashain days, Tihar days, and major national/cultural festivals)
        for (f in festivals) {
            val en = f.englishTitle.lowercase()
            val ne = f.nepaliTitle

            // Dashain day-by-day
            if ("ghatasthapana" in en || "घटस्थापना" in ne) return CellArtworkMotif.GHATASTHAPANA
            if ("fulpati" in en || "फूलपाती" in ne) return CellArtworkMotif.FULPATI
            if ("ashtami" in en || "महाष्टमी" in ne) return CellArtworkMotif.MAHA_ASHTAMI
            if ("nawami" in en || "महानवमी" in ne || "राम नवमी" in ne) return CellArtworkMotif.MAHA_NAWAMI
            if ("vijaya dashami" in en || "dashain" in en || "विजया दशमी" in ne || "दशैं" in ne) {
                return CellArtworkMotif.VIJAYA_DASHAMI
            }

            // Tihar day-by-day
            if ("kaag tihar" in en || "काग तिहार" in ne) return CellArtworkMotif.KAAG_TIHAR
            if ("kukur tihar" in en || "कुकुर तिहार" in ne) return CellArtworkMotif.KUKUR_TIHAR
            if ("laxmi puja" in en || "gai tihar" in en || "लक्ष्मी पूजा" in ne || "गाई तिहार" in ne) {
                return CellArtworkMotif.GAI_LAXMI_PUJA
            }
            if ("mha puja" in en || "gobardhan" in en || "म्हः पूजा" in ne || "गोवर्द्धन" in ne) {
                return CellArtworkMotif.MHA_GOBARDHAN_PUJA
            }
            if ("bhai tika" in en || "kija puja" in en || "tihar" in en || "भाइटीका" in ne || "तिहार" in ne) {
                return CellArtworkMotif.BHAI_TIKA
            }

            // Major Nepali Festivals
            if ("chhath" in en || "छठ" in ne) return CellArtworkMotif.CHHATH
            if ("shivaratri" in en || "शिवरात्री" in ne) return CellArtworkMotif.SHIVARATRI
            if ("buddha" in en || "बुद्ध" in ne) return CellArtworkMotif.BUDDHA_JAYANTI
            if ("yomari" in en || "udhauli" in en || "य:मरि" in ne || "योमरी" in ne) return CellArtworkMotif.YOMARI_PUNHI
            if ("paddy" in en || "dahi chiura" in en || "धान दिवस" in ne || "दही चिउरा" in ne) {
                return CellArtworkMotif.ASAR_PADDY
            }
            if ("new year" in en || "lhosar" in en || "नयाँ वर्ष" in ne || "ल्होसार" in ne) {
                return CellArtworkMotif.NEW_YEAR_LHOSAR
            }
            if ("teej" in en || "तीज" in ne) return CellArtworkMotif.TEEJ
            if ("holi" in en || "fagu" in en || "फागु" in ne || "होली" in ne) return CellArtworkMotif.HOLI
            if ("janai" in en || "jatra" in en || "जनै" in ne || "जात्रा" in ne) return CellArtworkMotif.JANAI_JATRA
            if ("maghe" in en || "saraswati" in en || "basanta" in en || "माघे" in ne || "सरस्वती" in ne) {
                return CellArtworkMotif.MAGHE_SARASWATI
            }
        }
        return null
    }
}

@Composable
fun FestivalCellWatermark(
    motif: CellArtworkMotif,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    alpha: Float = 0.25f
) {
    val drawColor = tint.copy(alpha = alpha)
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(
            width = (w * 0.068f).coerceAtLeast(1.8f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (motif) {
            CellArtworkMotif.GHATASTHAPANA -> drawGhatasthapanaKalash(w, h, drawColor, stroke)
            CellArtworkMotif.FULPATI -> drawFulpatiBundle(w, h, drawColor, stroke)
            CellArtworkMotif.MAHA_ASHTAMI -> drawMahaAshtamiKhukuri(w, h, drawColor, stroke)
            CellArtworkMotif.MAHA_NAWAMI -> drawMahaNawamiTemple(w, h, drawColor, stroke)
            CellArtworkMotif.VIJAYA_DASHAMI -> drawDashainChangaKite(w, h, drawColor, stroke)

            CellArtworkMotif.KAAG_TIHAR -> drawKaagCrow(w, h, drawColor, stroke)
            CellArtworkMotif.KUKUR_TIHAR -> drawKukurDog(w, h, drawColor, stroke)
            CellArtworkMotif.GAI_LAXMI_PUJA -> drawTiharDiyoLotus(w, h, drawColor, stroke)
            CellArtworkMotif.MHA_GOBARDHAN_PUJA -> drawMhaPujaMandala(w, h, drawColor, stroke)
            CellArtworkMotif.BHAI_TIKA -> drawBhaiTikaGarland(w, h, drawColor, stroke)

            CellArtworkMotif.BIRTHDAY -> drawBirthdayCake(w, h, drawColor, stroke)
            CellArtworkMotif.SHRADDHA -> drawShraddhaPindaKush(w, h, drawColor, stroke)

            CellArtworkMotif.CHHATH -> drawChhathRisingSun(w, h, drawColor, stroke)
            CellArtworkMotif.SHIVARATRI -> drawShivaratriTrishul(w, h, drawColor, stroke)
            CellArtworkMotif.BUDDHA_JAYANTI -> drawBuddhaLotus(w, h, drawColor, stroke)
            CellArtworkMotif.YOMARI_PUNHI -> drawYomariDumpling(w, h, drawColor, stroke)
            CellArtworkMotif.ASAR_PADDY -> drawAsarPaddyStalk(w, h, drawColor, stroke)
            CellArtworkMotif.NEW_YEAR_LHOSAR -> drawNewYearBellFlags(w, h, drawColor, stroke)
            CellArtworkMotif.TEEJ -> drawTeejKalashPote(w, h, drawColor, stroke)
            CellArtworkMotif.HOLI -> drawHoliSplash(w, h, drawColor, stroke)
            CellArtworkMotif.JANAI_JATRA -> drawSacredKnot(w, h, drawColor, stroke)
            CellArtworkMotif.MAGHE_SARASWATI -> drawSunChakra(w, h, drawColor, stroke)
        }
    }
}

// 1. Ghatasthapana: Clay Kalash pot with sprouting Jamara blades
private fun DrawScope.drawGhatasthapanaKalash(w: Float, h: Float, c: Color, s: Stroke) {
    // Pot belly
    drawOval(
        color = c,
        topLeft = Offset(w * 0.24f, h * 0.46f),
        size = Size(w * 0.52f, h * 0.44f),
        style = s
    )
    // Pot neck & rim
    drawLine(c, Offset(w * 0.34f, h * 0.44f), Offset(w * 0.66f, h * 0.44f), s.width)
    // Base stand
    drawLine(c, Offset(w * 0.34f, h * 0.93f), Offset(w * 0.66f, h * 0.93f), s.width)
    // Sprouting Jamara blades
    val p = Path().apply {
        moveTo(w * 0.50f, h * 0.44f)
        lineTo(w * 0.50f, h * 0.10f)
        moveTo(w * 0.45f, h * 0.44f)
        quadraticBezierTo(w * 0.34f, h * 0.24f, w * 0.22f, h * 0.16f)
        moveTo(w * 0.55f, h * 0.44f)
        quadraticBezierTo(w * 0.66f, h * 0.24f, w * 0.78f, h * 0.16f)
    }
    drawPath(p, c, style = s)
}

// 2. Fulpati: Bundled sacred leaves and sugarcane stalks tied together
private fun DrawScope.drawFulpatiBundle(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Main diagonal stalks
        moveTo(w * 0.22f, h * 0.88f)
        lineTo(w * 0.78f, h * 0.16f)
        moveTo(w * 0.32f, h * 0.92f)
        lineTo(w * 0.62f, h * 0.12f)
        moveTo(w * 0.16f, h * 0.78f)
        lineTo(w * 0.86f, h * 0.34f)
        // Leaf arcs
        moveTo(w * 0.48f, h * 0.52f)
        quadraticBezierTo(w * 0.30f, h * 0.32f, w * 0.42f, h * 0.16f)
        moveTo(w * 0.54f, h * 0.56f)
        quadraticBezierTo(w * 0.74f, h * 0.46f, w * 0.86f, h * 0.26f)
    }
    drawPath(p, c, style = s)
    // Tie knot band
    drawLine(c, Offset(w * 0.28f, h * 0.62f), Offset(w * 0.52f, h * 0.76f), s.width)
}

// 3. Maha Ashtami: Curved Nepali Khukuri / Khadga ritual blade
private fun DrawScope.drawMahaAshtamiKhukuri(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Handle
        moveTo(w * 0.18f, h * 0.86f)
        lineTo(w * 0.34f, h * 0.66f)
        // Spine curving forward to tip
        quadraticBezierTo(w * 0.50f, h * 0.30f, w * 0.88f, h * 0.14f)
        // Belly of the curved blade
        quadraticBezierTo(w * 0.74f, h * 0.50f, w * 0.38f, h * 0.70f)
        close()
    }
    drawPath(p, c, style = s)
    // Bolster ring
    drawLine(c, Offset(w * 0.28f, h * 0.62f), Offset(w * 0.42f, h * 0.74f), s.width)
}

// 4. Maha Nawami: Two-tier Nepali pagoda temple roof with bell
private fun DrawScope.drawMahaNawamiTemple(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Pinnacles (Gajur)
        moveTo(w * 0.50f, h * 0.08f)
        lineTo(w * 0.50f, h * 0.20f)
        // Upper roof tier
        moveTo(w * 0.24f, h * 0.36f)
        lineTo(w * 0.50f, h * 0.20f)
        lineTo(w * 0.76f, h * 0.36f)
        close()
        // Lower wider roof tier
        moveTo(w * 0.12f, h * 0.58f)
        lineTo(w * 0.50f, h * 0.38f)
        lineTo(w * 0.88f, h * 0.58f)
        close()
        // Hanging temple bell below
        moveTo(w * 0.50f, h * 0.58f)
        lineTo(w * 0.50f, h * 0.68f)
        moveTo(w * 0.38f, h * 0.86f)
        lineTo(w * 0.43f, h * 0.68f)
        lineTo(w * 0.57f, h * 0.68f)
        lineTo(w * 0.62f, h * 0.86f)
        close()
    }
    drawPath(p, c, style = s)
    drawCircle(c, radius = w * 0.04f, center = Offset(w * 0.50f, h * 0.91f))
}

// 5. Vijaya Dashami: Flying diamond Changa kite with crossbar & tail
private fun DrawScope.drawDashainChangaKite(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Diamond kite frame
        moveTo(w * 0.56f, h * 0.10f)
        lineTo(w * 0.86f, h * 0.36f)
        lineTo(w * 0.52f, h * 0.66f)
        lineTo(w * 0.24f, h * 0.38f)
        close()
        // Spine & bowed crossbar
        moveTo(w * 0.56f, h * 0.10f)
        lineTo(w * 0.52f, h * 0.66f)
        moveTo(w * 0.24f, h * 0.38f)
        quadraticBezierTo(w * 0.56f, h * 0.24f, w * 0.86f, h * 0.36f)
        // Wavy tail
        moveTo(w * 0.52f, h * 0.66f)
        quadraticBezierTo(w * 0.36f, h * 0.76f, w * 0.42f, h * 0.84f)
        quadraticBezierTo(w * 0.48f, h * 0.92f, w * 0.22f, h * 0.94f)
    }
    drawPath(p, c, style = s)
}

// 6. Kaag Tihar: Perched crow silhouette
private fun DrawScope.drawKaagCrow(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Beak & head
        moveTo(w * 0.18f, h * 0.28f)
        lineTo(w * 0.32f, h * 0.22f)
        quadraticBezierTo(w * 0.46f, h * 0.18f, w * 0.52f, h * 0.32f)
        // Back & wing to tail
        lineTo(w * 0.86f, h * 0.78f)
        lineTo(w * 0.72f, h * 0.80f)
        // Breast & belly
        quadraticBezierTo(w * 0.42f, h * 0.68f, w * 0.34f, h * 0.42f)
        lineTo(w * 0.32f, h * 0.32f)
        close()
        // Legs
        moveTo(w * 0.48f, h * 0.68f)
        lineTo(w * 0.44f, h * 0.90f)
        moveTo(w * 0.56f, h * 0.70f)
        lineTo(w * 0.54f, h * 0.90f)
    }
    drawPath(p, c, style = s)
}

// 7. Kukur Tihar: Sitting dog silhouette with marigold garland
private fun DrawScope.drawKukurDog(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Head & snout
        moveTo(w * 0.22f, h * 0.28f)
        lineTo(w * 0.36f, h * 0.20f)
        lineTo(w * 0.48f, h * 0.24f)
        // Floppy ear
        lineTo(w * 0.52f, h * 0.38f)
        // Back & seated haunch
        quadraticBezierTo(w * 0.72f, h * 0.50f, w * 0.74f, h * 0.88f)
        // Base & front leg
        lineTo(w * 0.34f, h * 0.88f)
        lineTo(w * 0.34f, h * 0.46f)
        lineTo(w * 0.22f, h * 0.36f)
        close()
        // Curled tail
        moveTo(w * 0.74f, h * 0.78f)
        quadraticBezierTo(w * 0.90f, h * 0.66f, w * 0.84f, h * 0.50f)
        // Garland arc around neck
        moveTo(w * 0.32f, h * 0.44f)
        quadraticBezierTo(w * 0.46f, h * 0.56f, w * 0.58f, h * 0.42f)
    }
    drawPath(p, c, style = s)
}

// 8. Gai Tihar / Laxmi Puja: Glowing clay Diyo lamp on lotus petals
private fun DrawScope.drawTiharDiyoLotus(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Diyo bowl
        moveTo(w * 0.16f, h * 0.56f)
        lineTo(w * 0.84f, h * 0.56f)
        quadraticBezierTo(w * 0.74f, h * 0.84f, w * 0.50f, h * 0.84f)
        quadraticBezierTo(w * 0.26f, h * 0.84f, w * 0.16f, h * 0.56f)
        close()
        // Flame
        moveTo(w * 0.50f, h * 0.52f)
        quadraticBezierTo(w * 0.34f, h * 0.34f, w * 0.50f, h * 0.12f)
        quadraticBezierTo(w * 0.66f, h * 0.34f, w * 0.50f, h * 0.52f)
        // Base lotus petals
        moveTo(w * 0.26f, h * 0.86f)
        lineTo(w * 0.74f, h * 0.86f)
    }
    drawPath(p, c, style = s)
}

// 9. Gobardhan / Mha Puja: 8-petal geometric Newa Mandala
private fun DrawScope.drawMhaPujaMandala(w: Float, h: Float, c: Color, s: Stroke) {
    val cx = w * 0.50f
    val cy = h * 0.50f
    drawCircle(c, radius = w * 0.40f, center = Offset(cx, cy), style = s)
    drawCircle(c, radius = w * 0.18f, center = Offset(cx, cy), style = s)
    for (i in 0 until 8) {
        val angle = (i * 45.0) * PI / 180.0
        val x1 = cx + (w * 0.18f) * cos(angle).toFloat()
        val y1 = cy + (h * 0.18f) * sin(angle).toFloat()
        val x2 = cx + (w * 0.40f) * cos(angle).toFloat()
        val y2 = cy + (h * 0.40f) * sin(angle).toFloat()
        drawLine(c, Offset(x1, y1), Offset(x2, y2), s.width * 0.85f)
    }
}

// 10. Bhai Tika: Makhamali flower garland ring with vertical Saptarangi Tika stroke
private fun DrawScope.drawBhaiTikaGarland(w: Float, h: Float, c: Color, s: Stroke) {
    val cx = w * 0.50f
    val cy = h * 0.54f
    val r = w * 0.34f
    // Scalloped Makhamali garland beads
    for (i in 0 until 12) {
        val angle = (i * 30.0) * PI / 180.0
        val bx = cx + r * cos(angle).toFloat()
        val by = cy + r * sin(angle).toFloat()
        drawCircle(c, radius = w * 0.055f, center = Offset(bx, by), style = s)
    }
    // Vertical Saptarangi Tika stroke in center
    drawLine(
        color = c,
        start = Offset(cx, h * 0.14f),
        end = Offset(cx, h * 0.62f),
        strokeWidth = s.width * 1.45f,
        cap = StrokeCap.Round
    )
    drawCircle(c, radius = w * 0.05f, center = Offset(cx, h * 0.70f))
}

// 11. Birthday: Minimal single-tier cake with wavy frosting and lit candle
private fun DrawScope.drawBirthdayCake(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Cake tier body
        moveTo(w * 0.18f, h * 0.52f)
        lineTo(w * 0.82f, h * 0.52f)
        lineTo(w * 0.82f, h * 0.86f)
        lineTo(w * 0.18f, h * 0.86f)
        close()
        // Wavy icing line
        moveTo(w * 0.18f, h * 0.68f)
        quadraticBezierTo(w * 0.34f, h * 0.76f, w * 0.50f, h * 0.68f)
        quadraticBezierTo(w * 0.66f, h * 0.60f, w * 0.82f, h * 0.68f)
        // Candle stick
        moveTo(w * 0.50f, h * 0.52f)
        lineTo(w * 0.50f, h * 0.30f)
        // Candle flame
        moveTo(w * 0.50f, h * 0.26f)
        quadraticBezierTo(w * 0.42f, h * 0.16f, w * 0.50f, h * 0.10f)
        quadraticBezierTo(w * 0.58f, h * 0.16f, w * 0.50f, h * 0.26f)
    }
    drawPath(p, c, style = s)
}

// 12. Shraddha: Three round Pinda offerings resting on sacred Kush grass blades
private fun DrawScope.drawShraddhaPindaKush(w: Float, h: Float, c: Color, s: Stroke) {
    // Three Pinda rice balls in pyramid/cluster
    drawCircle(c, radius = w * 0.13f, center = Offset(w * 0.36f, h * 0.56f), style = s)
    drawCircle(c, radius = w * 0.13f, center = Offset(w * 0.64f, h * 0.56f), style = s)
    drawCircle(c, radius = w * 0.13f, center = Offset(w * 0.50f, h * 0.36f), style = s)
    // Sacred Kush grass blades underneath
    val p = Path().apply {
        moveTo(w * 0.14f, h * 0.86f)
        lineTo(w * 0.88f, h * 0.68f)
        moveTo(w * 0.18f, h * 0.76f)
        lineTo(w * 0.86f, h * 0.82f)
        moveTo(w * 0.24f, h * 0.92f)
        lineTo(w * 0.78f, h * 0.62f)
    }
    drawPath(p, c, style = s)
}

// 13. Chhath: Rising sun with rays over river waves
private fun DrawScope.drawChhathRisingSun(w: Float, h: Float, c: Color, s: Stroke) {
    drawArc(
        color = c,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.26f, h * 0.28f),
        size = Size(w * 0.48f, h * 0.44f),
        style = s
    )
    // Sun rays
    val cx = w * 0.50f
    val cy = h * 0.50f
    for (deg in listOf(200, 235, 270, 305, 340)) {
        val rad = deg * PI / 180.0
        drawLine(
            color = c,
            start = Offset(cx + (w * 0.28f) * cos(rad).toFloat(), cy + (h * 0.28f) * sin(rad).toFloat()),
            end = Offset(cx + (w * 0.40f) * cos(rad).toFloat(), cy + (h * 0.40f) * sin(rad).toFloat()),
            strokeWidth = s.width
        )
    }
    // River waves
    val waves = Path().apply {
        moveTo(w * 0.12f, h * 0.66f)
        quadraticBezierTo(w * 0.30f, h * 0.58f, w * 0.50f, h * 0.66f)
        quadraticBezierTo(w * 0.70f, h * 0.74f, w * 0.88f, h * 0.66f)
        moveTo(w * 0.16f, h * 0.84f)
        quadraticBezierTo(w * 0.34f, h * 0.76f, w * 0.50f, h * 0.84f)
        quadraticBezierTo(w * 0.66f, h * 0.92f, w * 0.84f, h * 0.84f)
    }
    drawPath(waves, c, style = s)
}

// 14. Maha Shivaratri: 3-prong Trishul & Damaru
private fun DrawScope.drawShivaratriTrishul(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Center spear & shaft
        moveTo(w * 0.50f, h * 0.10f)
        lineTo(w * 0.50f, h * 0.90f)
        // Outer prongs
        moveTo(w * 0.26f, h * 0.20f)
        quadraticBezierTo(w * 0.24f, h * 0.48f, w * 0.50f, h * 0.48f)
        quadraticBezierTo(w * 0.76f, h * 0.48f, w * 0.74f, h * 0.20f)
        // Damaru hourglass on shaft
        moveTo(w * 0.32f, h * 0.62f)
        lineTo(w * 0.68f, h * 0.76f)
        lineTo(w * 0.68f, h * 0.62f)
        lineTo(w * 0.32f, h * 0.76f)
        close()
    }
    drawPath(p, c, style = s)
}

// 15. Buddha Jayanti: Sacred 5-petal Lotus
private fun DrawScope.drawBuddhaLotus(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Center petal
        moveTo(w * 0.50f, h * 0.16f)
        quadraticBezierTo(w * 0.68f, h * 0.46f, w * 0.50f, h * 0.76f)
        quadraticBezierTo(w * 0.32f, h * 0.46f, w * 0.50f, h * 0.16f)
        // Left petal
        moveTo(w * 0.50f, h * 0.76f)
        quadraticBezierTo(w * 0.16f, h * 0.68f, w * 0.22f, h * 0.32f)
        quadraticBezierTo(w * 0.38f, h * 0.42f, w * 0.42f, h * 0.62f)
        // Right petal
        moveTo(w * 0.50f, h * 0.76f)
        quadraticBezierTo(w * 0.84f, h * 0.68f, w * 0.78f, h * 0.32f)
        quadraticBezierTo(w * 0.62f, h * 0.42f, w * 0.58f, h * 0.62f)
        // Base line
        moveTo(w * 0.24f, h * 0.84f)
        lineTo(w * 0.76f, h * 0.84f)
    }
    drawPath(p, c, style = s)
}

// 16. Yomari Punhi: Fig-shaped Yomari dumpling
private fun DrawScope.drawYomariDumpling(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        moveTo(w * 0.22f, h * 0.86f)
        quadraticBezierTo(w * 0.26f, h * 0.34f, w * 0.70f, h * 0.22f)
        lineTo(w * 0.82f, h * 0.14f)
        lineTo(w * 0.86f, h * 0.26f)
        lineTo(w * 0.76f, h * 0.32f)
        quadraticBezierTo(w * 0.74f, h * 0.76f, w * 0.22f, h * 0.86f)
        close()
    }
    drawPath(p, c, style = s)
}

// 17. Asar 15 Paddy Day: Curved golden Dhaan stalk
private fun DrawScope.drawAsarPaddyStalk(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Main curved stem
        moveTo(w * 0.68f, h * 0.90f)
        quadraticBezierTo(w * 0.68f, h * 0.32f, w * 0.20f, h * 0.24f)
        // Leaf blade
        moveTo(w * 0.66f, h * 0.84f)
        quadraticBezierTo(w * 0.86f, h * 0.56f, w * 0.76f, h * 0.34f)
    }
    drawPath(p, c, style = s)
    // Paddy grains along upper arc
    drawOval(c, Offset(w * 0.18f, h * 0.20f), Size(w * 0.14f, h * 0.09f), style = s)
    drawOval(c, Offset(w * 0.34f, h * 0.22f), Size(w * 0.14f, h * 0.09f), style = s)
    drawOval(c, Offset(w * 0.48f, h * 0.30f), Size(w * 0.14f, h * 0.09f), style = s)
}

// 18. Nepali New Year / Lhosar: Temple bell & crossed Dhwaja pennants
private fun DrawScope.drawNewYearBellFlags(w: Float, h: Float, c: Color, s: Stroke) {
    val p = Path().apply {
        // Bell body
        moveTo(w * 0.30f, h * 0.82f)
        lineTo(w * 0.38f, h * 0.46f)
        lineTo(w * 0.62f, h * 0.46f)
        lineTo(w * 0.70f, h * 0.82f)
        close()
        // Crossed flags at top
        moveTo(w * 0.50f, h * 0.46f)
        lineTo(w * 0.24f, h * 0.16f)
        lineTo(w * 0.16f, h * 0.28f)
        lineTo(w * 0.34f, h * 0.28f)
        moveTo(w * 0.50f, h * 0.46f)
        lineTo(w * 0.76f, h * 0.16f)
        lineTo(w * 0.84f, h * 0.28f)
        lineTo(w * 0.66f, h * 0.28f)
    }
    drawPath(p, c, style = s)
    drawCircle(c, radius = w * 0.05f, center = Offset(w * 0.50f, h * 0.89f))
}

// 19. Teej: Saubhagya Kalash & Pote bead arc
private fun DrawScope.drawTeejKalashPote(w: Float, h: Float, c: Color, s: Stroke) {
    drawCircle(c, radius = w * 0.26f, center = Offset(w * 0.50f, h * 0.62f), style = s)
    drawArc(
        color = c,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(w * 0.16f, h * 0.14f),
        size = Size(w * 0.68f, h * 0.50f),
        style = s
    )
}

// 20. Holi: 3-drop Abir splash
private fun DrawScope.drawHoliSplash(w: Float, h: Float, c: Color, s: Stroke) {
    drawCircle(c, radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.30f), style = s)
    drawCircle(c, radius = w * 0.14f, center = Offset(w * 0.30f, h * 0.66f), style = s)
    drawCircle(c, radius = w * 0.14f, center = Offset(w * 0.70f, h * 0.66f), style = s)
}

// 21. Janai Purnima / Jatra: Sacred Raksha infinity knot
private fun DrawScope.drawSacredKnot(w: Float, h: Float, c: Color, s: Stroke) {
    drawCircle(c, radius = w * 0.20f, center = Offset(w * 0.34f, h * 0.50f), style = s)
    drawCircle(c, radius = w * 0.20f, center = Offset(w * 0.66f, h * 0.50f), style = s)
}

// 22. Maghe Sankranti / Saraswati Puja: Radiant Sun Wheel
private fun DrawScope.drawSunChakra(w: Float, h: Float, c: Color, s: Stroke) {
    val cx = w * 0.50f
    val cy = h * 0.50f
    drawCircle(c, radius = w * 0.22f, center = Offset(cx, cy), style = s)
    for (i in 0 until 8) {
        val a = (i * 45.0) * PI / 180.0
        drawLine(
            color = c,
            start = Offset(cx + (w * 0.25f) * cos(a).toFloat(), cy + (h * 0.25f) * sin(a).toFloat()),
            end = Offset(cx + (w * 0.42f) * cos(a).toFloat(), cy + (h * 0.42f) * sin(a).toFloat()),
            strokeWidth = s.width
        )
    }
}
