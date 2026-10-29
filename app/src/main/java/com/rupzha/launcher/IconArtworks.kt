package com.rupzha.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Shared swatches for the hand-painted palette.
private val SpotifyGreen = Color(0xFF38C76F)
private val Sun = Color(0xFFFFC95C)
private val Sky = Color(0xFF5BB8F5)
private val Mint = Color(0xFF5FD3A5)
private val Blurple = Color(0xFF6D75F5)
private val Rose = Color(0xFFF2585C)
private val Wood = Color(0xFFFFD96A)
private val fbColors = listOf(
    Color(0xFFFF9E80), Color(0xFFA78BFA), Mint, Sky, Sun, Color(0xFFFF8FB1),
)

private fun Pen.fb(i: Int = 0): Color = fbColors[((seed ushr 1) + i * 3) % fbColors.size]
private fun union(paths: List<Path>): Path = paths.reduce { a, b -> Path.combine(PathOperation.Union, a, b) }
private fun Pen.arc(cx: Number, cy: Number, r: Number, start: Float, sweep: Float): Path = Path().apply {
    addArc(Rect((cx.f - r.f) * u, (cy.f - r.f) * u, (cx.f + r.f) * u, (cy.f + r.f) * u), start, sweep)
}
private fun Pen.rect(c: Color, x: Number, y: Number, w: Number, h: Number) {
    ds.drawRect(c, Offset(x.f * u, y.f * u), Size(w.f * u, h.f * u))
}

internal fun Pen.drawArt(art: Art) {
    when (art) {
        Art.SPOTIFY -> spotify()
        Art.YOUTUBE -> youtube()
        Art.CHROME -> chrome()
        Art.TELEGRAM -> telegram()
        Art.DISCORD -> discord()
        Art.WHATSAPP -> whatsapp()
        Art.INSTAGRAM -> instagram()
        Art.CAMERA -> camera()
        Art.GALLERY -> gallery()
        Art.PLAYSTORE -> playstore()
        Art.SETTINGS -> gear(Color(0xFFB8A6F2), "")
        Art.FILES -> files()
        Art.CALENDAR -> calendar()
        Art.CALCULATOR -> calculator()
        Art.MAPS -> maps()
        Art.GMAIL -> gmail()
        Art.DRIVE -> drive()
        Art.CHATGPT -> chatgpt()
        Art.GITHUB -> github()
        Art.TERMUX -> termux()
        Art.PHONE -> phone()
        Art.MESSAGES -> messages()
        Art.CLOCK -> clock()
        Art.MUSIC -> music()
        Art.BROWSER -> browser()
        Art.NOTES -> notes()
        Art.WEATHER -> weather()
        Art.VIDEO -> video()
        Art.MINECRAFT -> minecraft()
        Art.SHIELD -> shield()
        Art.FLAME -> flame()
        Art.GAMEPAD -> gamepad()
        Art.FB_SOCIAL -> fbSocial()
        Art.FB_MEDIA -> fbMedia()
        Art.FB_WORK -> fbWork()
        Art.FB_TOOLS -> fbTools()
        Art.FB_SYSTEM -> gear(fb(), letter)
        Art.WIFI -> wifi()
        Art.BLUETOOTH -> bluetooth()
        Art.SUN -> sunArt()
        Art.TORCH -> torch()
        Art.SHOT -> shot()
        Art.BATTERY -> battery()
    }
}

// ------------------------------------------------------------------ named apps

private fun Pen.spotify() {
    val b = blob(50, 52, 40, 40, amp = 1.3f, k = 13)
    body(b, SpotifyGreen, shadow = true)
    stroke(Ink, 8.2f, qc(23, 38, 52, 27, 79, 41))
    stroke(Ink, 6.6f, qc(28, 53, 52, 44, 74, 55))
    stroke(Ink, 5.2f, qc(33, 67, 52, 60, 69, 68))
    hi(22, 30, 30, 22, 3.2f)
    star(85, 15, 7)
    dot(13, 24, 2.4f, Sun)
    dot(90, 82, 2.2f, Cream)
}

private fun Pen.youtube() {
    val stone = poly(9, 26, 22, 15, 50, 12, 79, 15, 92, 25, 95, 52, 91, 77, 78, 85, 50, 88, 21, 85, 9, 76, 5, 50, amp = 1.4f)
    body(stone, Rose, shadow = true, tex = 26)
    pl(Ink, 2.2f, 74, 14, 69, 27, 78, 36, 71, 49)
    pl(Ink, 1.7f, 23, 87, 27, 75, 19, 68)
    body(poly(41, 33, 41, 67, 70, 50, amp = 0.4f), Cream, w = 3.6f, tex = 0)
    hi(16, 28, 26, 19)
    body(poly(84, 86, 94, 82, 96, 92, 87, 95, amp = 0.3f), Rose.shade(0.1f), w = 2.6f, tex = 0)
    star(12, 12, 5)
}

private fun Pen.chrome() {
    rotate(-6f, 50, 50) {
        val disc = blob(50, 50, 42, 42, amp = 0.7f, k = 14)
        body(disc, Cream, shadow = true, tex = 0)
        ds.clipPath(disc) {
            drawPath(wedge(50, 50, 70, 210f, 330f), Color(0xFFEF5B4C))
            drawPath(wedge(50, 50, 70, 330f, 450f), Color(0xFFFFC93C))
            drawPath(wedge(50, 50, 70, 90f, 210f), Color(0xFF3BB873))
        }
        texture(disc, 24)
        ds.drawPath(crescent(disc, 3.6f), Color(0x330B1030))
        ln(Ink, 3, 50, 50, 13.6f, 29)
        ln(Ink, 3, 50, 50, 86.4f, 29)
        ln(Ink, 3, 50, 50, 50, 92)
        dot(50, 50, 17.5f, Cream, 3.2f)
        dot(50, 50, 12.5f, Color(0xFF4A8CF0), 3)
        hi(43, 44, 47, 40, 2.6f)
        outline(disc)
    }
    star(89, 13, 6)
    dot(11, 87, 2.4f, Sun)
}

private fun Pen.telegram() {
    val blue = Color(0xFF47B5F0)
    body(poly(92, 14, 8, 47, 31, 57, 41, 86, 56, 68, 76, 84, amp = 0.8f), blue, shadow = true)
    val flap = poly(31, 57, 41, 86, 56, 68, amp = 0.3f)
    ds.drawPath(flap, blue.shade(0.35f))
    outline(flap, 2.8f)
    pl(Ink, 2.6f, 92, 14, 35, 62)
    hi(20, 45, 40, 38)
    ln(Cream, 2.6f, 5, 70, 17, 68)
    ln(Cream, 2.6f, 9, 80, 23, 78)
    star(18, 16, 6)
    dot(88, 72, 2.4f, Cream)
}

private fun Pen.discord() {
    body(blob(22, 23, 13, 12), Blurple, w = 3.8f, tex = 0)
    body(blob(78, 23, 13, 12), Blurple, w = 3.8f, tex = 0)
    body(blob(30, 84, 11, 9), Blurple, w = 3.8f, tex = 0)
    body(blob(70, 84, 11, 9), Blurple, w = 3.8f, tex = 0)
    body(rr(9, 22, 82, 60, 24, amp = 1.1f), Blurple, shadow = true)
    for (x in listOf(36, 64)) {
        ds.drawPath(blob(x, 52, 7.5f, 11, amp = 0.4f, k = 10), Ink)
        dot(x - 2, 47, 2.6f, Cream, 0.2f)
    }
    stroke(Ink, 3.2f, qc(40, 68, 50, 75, 60, 68))
    dot(24, 64, 3.2f, Color(0xFFFF9EC0), 0.2f)
    dot(76, 64, 3.2f, Color(0xFFFF9EC0), 0.2f)
    hi(18, 32, 30, 28)
    star(89, 10, 5)
}

private fun Pen.whatsapp() {
    val g = Color(0xFF3FD06B)
    body(poly(12, 91, 18, 62, 40, 78, amp = 0.4f), g, tex = 0)
    body(blob(52, 46, 40, 38, amp = 1.1f), g, shadow = true)
    fat(qc(37, 33, 35, 62, 63, 65), Cream, 7.5f)
    dot(37, 33, 5, Cream, 3)
    dot(63, 65, 5, Cream, 3)
    hi(24, 30, 32, 22)
    star(88, 12, 6)
    dot(9, 40, 2.4f, Sun)
}

private fun Pen.instagram() {
    rotate(-5f, 50, 52) {
        val shell = rr(10, 16, 80, 76, 22, amp = 1.1f)
        body(shell, Color(0xFFE94E8B), shadow = true, tex = 0)
        ds.clipPath(shell) {
            drawPath(poly(40, 0, 100, 0, 100, 50, amp = 1f), Color(0xFF7B4FD8))
            drawPath(poly(0, 45, 0, 100, 55, 100, amp = 1f), Color(0xFFFFC857))
        }
        texture(shell, 20)
        ds.drawPath(crescent(shell), Color(0x330B1030))
        dot(50, 54, 21, Cream, 3.4f)
        dot(50, 54, 14, Color(0xFF5B3BB5), 3.2f)
        hi(43, 47, 47, 43, 2.6f)
        dot(75, 29, 4.2f, Cream, 2.6f)
        outline(shell)
    }
    star(90, 10, 5)
}

private fun Pen.camera() {
    val coral = Color(0xFFFF8A78)
    body(rr(30, 14, 28, 20, 7), coral.shade(0.1f), w = 3.8f, tex = 0)
    body(rr(64, 12, 14, 10, 4), Sun, w = 3.4f, tex = 0)
    val b = rr(7, 28, 86, 60, 14, amp = 1.1f)
    body(b, coral, shadow = true)
    ds.clipPath(b) { rect(Cream, 0, 36, 100, 9) }
    outline(b)
    dot(50, 62, 23, Color(0xFFE9DCCB), 3.6f)
    dot(50, 62, 16, Color(0xFF2B3A8C), 3.2f)
    dot(50, 62, 8, Color(0xFF111A55), 2.4f)
    hi(41, 53, 47, 48)
    dot(83, 72, 3.4f, Color(0xFFEF5B4C), 2.2f)
    star(90, 8, 5)
}

private fun Pen.gallery() {
    rotate(-9f, 50, 52) { body(rr(16, 18, 64, 64, 9), Color(0xFFB8A6F2), shadow = true, tex = 8) }
    rotate(6f, 50, 52) {
        body(rr(20, 14, 64, 70, 9), Cream, shadow = true, tex = 6)
        val pic = rr(27, 21, 50, 38, 5, amp = 0.5f)
        body(pic, Color(0xFF8FD3F4), w = 3f, tex = 0)
        ds.clipPath(pic) {
            drawPath(poly(27, 59, 42, 36, 54, 50, 62, 42, 77, 59), Color(0xFF3FB886))
        }
        dot(65, 30, 5.2f, Sun, 2.4f)
        outline(pic, 3f)
        ln(Ink, 2.4f, 29, 68, 70, 68)
        ln(Ink, 2.4f, 29, 76, 56, 76)
    }
    star(88, 12, 6)
}

private fun Pen.playstore() {
    val tri = poly(16, 8, 16, 92, 90, 50, amp = 1f)
    body(tri, Color(0xFF3AA9F2), shadow = true, tex = 0)
    ds.clipPath(tri) {
        drawPath(poly(16, 8, 16, 92, 45, 50), Color(0xFF3AA9F2))
        drawPath(poly(16, 8, 45, 50, 90, 50), Color(0xFF48CE7A))
        drawPath(poly(16, 92, 45, 50, 90, 50), Rose)
        drawPath(poly(66, 43, 66, 57, 90, 50), Sun)
    }
    ln(Ink, 2.6f, 16, 8, 45, 50)
    ln(Ink, 2.6f, 16, 92, 45, 50)
    ln(Ink, 2.6f, 45, 50, 90, 50)
    texture(tri, 18)
    ds.drawPath(crescent(tri), Color(0x2A0B1030))
    outline(tri)
    hi(22, 20, 22, 34)
    star(86, 14, 6)
}

private fun Pen.gear(color: Color, letterText: String) {
    val pts = ArrayList<Number>()
    for (i in 0 until 8) {
        val a = i * 45.0
        fun pt(da: Double, r: Double) {
            val rad = Math.toRadians(a + da)
            pts.add(50 + r * cos(rad)); pts.add(50 + r * sin(rad))
        }
        pt(-17.0, 33.0); pt(-10.0, 43.0); pt(10.0, 43.0); pt(17.0, 33.0)
    }
    body(poly(*pts.toTypedArray(), amp = 0.7f), color, shadow = true, tex = 20)
    if (letterText.isEmpty()) {
        val hole = blob(50, 50, 13, 13, amp = 0.5f, k = 10)
        ds.drawPath(hole, Color(0xFF2A2A66))
        outline(hole, 3.2f)
        hi(44, 45, 48, 41, 2.4f)
        for (k in 0 until 4) {
            val rad = Math.toRadians(45.0 + k * 90.0)
            dot(50 + 26 * cos(rad), 50 + 26 * sin(rad), 1.8f, Cream, 1f)
        }
    } else {
        body(blob(50, 50, 19, 19, amp = 0.5f, k = 10), Cream, w = 3.2f, tex = 0)
        text(letterText, 50, 51, 26, color.shade(0.1f), 3f)
    }
}

private fun Pen.files() {
    val y = Color(0xFFFFC857)
    body(poly(7, 24, 36, 24, 44, 32, 93, 32, 93, 86, 7, 86, amp = 0.9f), y.shade(0.12f), shadow = true)
    rotate(-5f, 50, 50) {
        body(rr(17, 22, 62, 44, 5), Cream, w = 3.2f, tex = 0)
        ln(Ink, 2.2f, 25, 34, 66, 34)
        ln(Ink, 2.2f, 25, 43, 58, 43)
        ln(Ink, 2.2f, 25, 52, 62, 52)
    }
    body(rr(7, 44, 86, 42, 9, amp = 1f), y, tex = 18)
    hi(14, 52, 30, 50)
    star(86, 16, 6)
}

private fun Pen.calendar() {
    val card = rr(11, 18, 78, 72, 11, amp = 1f)
    body(card, Cream, shadow = true, tex = 8)
    ds.clipPath(card) { rect(Color(0xFFFF8A78), 0, 0, 100, 40) }
    ln(Ink, 3, 11, 40, 89, 40)
    outline(card)
    body(rr(27, 9, 9, 19, 4.5f), Color(0xFFB8A6F2), w = 3f, tex = 0)
    body(rr(64, 9, 9, 19, 4.5f), Color(0xFFB8A6F2), w = 3f, tex = 0)
    text(if (extra > 0) extra.toString() else "29", 50, 66, 30, Color(0xFFFF8A78))
}

private fun Pen.calculator() {
    rotate(4f, 50, 50) {
        body(rr(20, 8, 60, 84, 10, amp = 0.9f), Color(0xFFB8A6F2), shadow = true)
        body(rr(27, 15, 46, 20, 5, amp = 0.4f), Color(0xFFBDE8B0), w = 3f, tex = 0)
        text("8", 65, 26, 13, Color(0xFF1C5A3A), 2.4f)
        for (r in 0..2) for (c in 0..2) {
            body(rr(27 + c * 16, 42 + r * 13.5f, 13, 10, 3, amp = 0.3f), if (c == 2) Peach else Cream, w = 2.6f, tex = 0)
        }
        body(rr(27, 82, 29, 8, 3, amp = 0.3f), Cream, w = 2.6f, tex = 0)
        body(rr(59, 82, 14, 8, 3, amp = 0.3f), Coral, w = 2.6f, tex = 0)
    }
    star(88, 12, 5)
}

private fun Pen.maps() {
    body(poly(9, 26, 36, 16, 36, 82, 9, 92, amp = 0.7f), Color(0xFF9ED36A), shadow = true, w = 3.6f, tex = 10)
    body(poly(36, 16, 64, 27, 64, 93, 36, 82, amp = 0.5f), Color(0xFFFFE9B8), w = 3.6f, tex = 10)
    body(poly(64, 27, 91, 17, 91, 83, 64, 93, amp = 0.7f), Color(0xFF8FD0F2), w = 3.6f, tex = 10)
    stroke(Sky, 4.2f, qc(39, 30, 50, 54, 61, 72))
    val tri = poly(53, 42, 71, 42, 62, 66, amp = 0.3f)
    body(tri, Rose, w = 3.6f, tex = 0)
    body(blob(62, 36, 12, 12, amp = 0.4f), Rose, w = 3.8f, tex = 0)
    dot(62, 36, 4.4f, Cream, 2.4f)
}

private fun Pen.gmail() {
    body(rr(8, 22, 84, 58, 9, amp = 1f), Cream, shadow = true, tex = 10)
    val m = Path().apply {
        moveTo(17 * u, 70 * u); lineTo(17 * u, 34 * u); lineTo(50 * u, 58 * u); lineTo(83 * u, 34 * u); lineTo(83 * u, 70 * u)
    }
    fat(m, Color(0xFFEF4B4B), 8.5f)
    star(90, 14, 6)
    dot(10, 88, 2.4f, Peach)
}

private fun Pen.drive() {
    val tri = poly(50, 9, 93, 82, 7, 82, amp = 1f)
    body(tri, Sun, shadow = true, tex = 0)
    ds.clipPath(tri) {
        drawPath(poly(50, 9, 7, 82, 50, 60), Color(0xFF3BB873))
        drawPath(poly(50, 9, 93, 82, 50, 60), Sun)
        drawPath(poly(7, 82, 93, 82, 50, 60), Color(0xFF4A8CF0))
    }
    ln(Ink, 3, 50, 60, 50, 9)
    ln(Ink, 3, 50, 60, 7, 82)
    ln(Ink, 3, 50, 60, 93, 82)
    texture(tri, 18)
    ds.drawPath(crescent(tri), Color(0x2A0B1030))
    outline(tri)
    hi(36, 40, 42, 28)
    star(88, 14, 5)
}

private fun Pen.chatgpt() {
    val g = Color(0xFF3CC9A0)
    fun petal(a: Float): Path {
        val rad = Math.toRadians(a.toDouble())
        val c = cos(rad).toFloat(); val s = sin(rad).toFloat()
        val pts = List(10) { i ->
            val t = i * 2f * PI.toFloat() / 10
            val lx = 13.5f * cos(t) + n(i) * 0.5f
            val ly = -24f + 25f * sin(t) + n(i + 9) * 0.5f
            Offset(50 + lx * c - ly * s, 50 + lx * s + ly * c)
        }
        return smooth(pts)
    }
    for (k in 0 until 6) body(petal(k * 60f + 10f), g, w = 3.8f, tex = 0)
    body(blob(50, 50, 13, 13, amp = 0.4f, k = 10), Cream, w = 3.6f, tex = 0)
    star(50, 50, 6, Sun)
    star(90, 12, 5)
}

private fun Pen.github() {
    val c = Color(0xFFD9CCF6)
    body(blob(30, 86, 8, 10), c, w = 3.6f, tex = 0)
    body(blob(50, 89, 8, 9), c, w = 3.6f, tex = 0)
    body(blob(70, 86, 8, 10), c, w = 3.6f, tex = 0)
    body(poly(14, 42, 16, 8, 42, 22, amp = 0.5f), c, w = 3.8f, tex = 0)
    body(poly(86, 42, 84, 8, 58, 22, amp = 0.5f), c, w = 3.8f, tex = 0)
    body(blob(50, 52, 38, 34, amp = 1f), c, shadow = true)
    for (x in listOf(37, 63)) {
        dot(x, 49, 7f, Cream, 2.6f)
        dot(x + 1, 50, 3.2f, Ink, 0.2f)
    }
    body(poly(46, 58, 54, 58, 50, 63, amp = 0.2f), Color(0xFFFF9EC0), w = 2.2f, tex = 0)
    stroke(Ink, 2.4f, qc(44, 66, 47, 71, 50, 65))
    stroke(Ink, 2.4f, qc(50, 65, 53, 71, 56, 66))
    ln(Ink, 2f, 14, 58, 28, 60)
    ln(Ink, 2f, 86, 58, 72, 60)
    star(90, 10, 5)
}

private fun Pen.termux() {
    val screen = rr(7, 14, 86, 72, 10, amp = 1f)
    body(screen, Color(0xFF1B2352), shadow = true, tex = 0)
    ds.clipPath(screen) { rect(Color(0xFF2C3770), 0, 0, 100, 27) }
    stroke(Color(0x55F6EBDD), 1.6f, rr(10.5f, 17.5f, 79, 65, 7, amp = 0.2f))
    dot(18, 21, 2.6f, Coral, 1.2f)
    dot(27, 21, 2.6f, Sun, 1.2f)
    dot(36, 21, 2.6f, Mint, 1.2f)
    pl(Mint, 5, 20, 40, 32, 50, 20, 60)
    ln(Peach, 5, 38, 62, 54, 62)
    ds.drawPath(rr(60, 52, 9, 13, 1.5f, amp = 0.2f), Peach)
    ln(Color(0x88F6EBDD), 3, 40, 40, 78, 40)
    ln(Color(0x88F6EBDD), 3, 40, 50, 66, 50)
    outline(screen)
    star(90, 10, 5)
}

private fun Pen.phone() {
    val g = Color(0xFF3FD06B)
    fat(qc(25, 27, 20, 78, 74, 76), g, 15f)
    body(blob(27, 28, 13, 10, amp = 0.5f), g, w = 3.8f, tex = 0)
    body(blob(74, 75, 10, 13, amp = 0.5f), g, w = 3.8f, tex = 0)
    stroke(Ink, 3.2f, qc(62, 22, 76, 26, 80, 40))
    stroke(Ink, 3.2f, qc(60, 10, 86, 16, 92, 40))
    hi(23, 42, 22, 56)
    star(14, 12, 5)
}

private fun Pen.messages() {
    val b = Sky
    body(poly(14, 92, 22, 64, 42, 76, amp = 0.4f), b, tex = 0)
    body(rr(8, 14, 84, 62, 24, amp = 1.1f), b, shadow = true)
    for (x in listOf(32, 50, 68)) dot(x, 45, 5.2f, Cream, 2.8f)
    hi(18, 26, 30, 21)
    star(88, 8, 5)
}

private fun Pen.clock() {
    ln(Ink, 5, 28, 86, 20, 95)
    ln(Ink, 5, 72, 86, 80, 95)
    rotate(-30f, 50, 54) { body(blob(50, 13, 13, 9, amp = 0.5f), Coral, w = 3.6f, tex = 0) }
    rotate(30f, 50, 54) { body(blob(50, 13, 13, 9, amp = 0.5f), Coral, w = 3.6f, tex = 0) }
    body(blob(50, 54, 38, 38, amp = 1f), Peach, shadow = true)
    body(blob(50, 54, 29, 29, amp = 0.5f), Cream, w = 3.2f, tex = 0)
    for (k in 0 until 12) {
        val rad = Math.toRadians(k * 30.0)
        ds.drawCircle(Ink, 1.1f * u, o(50 + 24 * cos(rad), 54 + 24 * sin(rad)))
    }
    pl(Ink, 3.6f, 50, 54, 50, 36)
    pl(Ink, 3.6f, 50, 54, 63, 60)
    dot(50, 54, 3.2f, Coral, 1.6f)
}

private fun Pen.music() {
    val d = blob(50, 52, 41, 41, amp = 0.9f, k = 13)
    body(d, Color(0xFF2E3472), shadow = true, tex = 0, shadeColor = Color(0xFF1B2050))
    stroke(Color(0x44F6EBDD), 1.4f, blob(50, 52, 32, 32, amp = 0.3f, k = 13, salt = 140))
    stroke(Color(0x33F6EBDD), 1.4f, blob(50, 52, 24, 24, amp = 0.3f, k = 11, salt = 170))
    stroke(Color(0x77F6EBDD), 2.6f, qc(21, 48, 25, 31, 40, 22))
    body(blob(50, 52, 15, 15, amp = 0.4f, k = 10), Peach, w = 3.2f, tex = 0)
    dot(50, 52, 3, Ink, 0.2f)
    star(87, 13, 6)
}

private fun Pen.browser() {
    val s = blob(50, 50, 41, 41, amp = 0.8f, k = 13)
    body(s, Sky, shadow = true, tex = 0)
    ds.clipPath(s) {
        drawPath(soft(22, 34, 36, 26, 46, 36, 40, 50, 28, 52, amp = 1f), Color(0xFF5CC27A))
        drawPath(soft(54, 56, 70, 50, 82, 62, 72, 78, 58, 74, amp = 1f), Color(0xFF5CC27A))
        drawPath(soft(58, 20, 72, 18, 72, 32, 60, 32, amp = 1f), Color(0xFF5CC27A))
    }
    stroke(Color(0x66F6EBDD), 1.8f, qc(50, 9, 28, 50, 50, 91))
    stroke(Color(0x66F6EBDD), 1.8f, qc(50, 9, 72, 50, 50, 91))
    texture(s, 18)
    ds.drawPath(crescent(s), Color(0x2A0B1030))
    outline(s)
    hi(22, 30, 30, 20)
    star(88, 12, 5)
}

private fun Pen.notes() {
    rotate(-4f, 50, 50) {
        val pad = rr(16, 16, 62, 76, 8)
        body(pad, Wood, shadow = true)
        ds.clipPath(pad) { rect(Coral, 0, 0, 100, 30) }
        outline(pad)
        for (x in listOf(28, 42, 56, 70)) body(rr(x - 2.5f, 9, 5, 16, 2.5f), Cream, w = 2.6f, tex = 0)
        ln(Ink, 2.4f, 26, 44, 68, 44)
        ln(Ink, 2.4f, 26, 56, 62, 56)
        ln(Ink, 2.4f, 26, 68, 66, 68)
    }
    rotate(28f, 80, 62) {
        body(rr(75, 34, 11, 42, 3), Blurple, w = 3.4f, tex = 0)
        body(poly(75, 76, 86, 76, 80.5f, 90, amp = 0.2f), Cream, w = 3f, tex = 0)
    }
}

private fun Pen.weather() {
    for (k in 0 until 8) {
        val rad = Math.toRadians(k * 45.0)
        val x1 = 38 + 26 * cos(rad); val y1 = 38 + 26 * sin(rad)
        val x2 = 38 + 35 * cos(rad); val y2 = 38 + 35 * sin(rad)
        ln(Ink, 8, x1, y1, x2, y2)
        ln(Sun, 4.6f, x1, y1, x2, y2)
    }
    body(blob(38, 38, 21, 21, amp = 0.6f), Sun, tex = 8)
    val cloud = union(
        listOf(blob(34, 74, 15, 13, amp = 0.4f), blob(56, 66, 19, 17, amp = 0.4f), blob(77, 76, 14, 12, amp = 0.4f), rr(26, 76, 52, 14, 7, amp = 0.3f)),
    )
    body(cloud, Cream, shadow = true, tex = 6)
    hi(48, 56, 56, 52)
}

private fun Pen.video() {
    body(rr(12, 40, 76, 50, 8), Blurple, shadow = true)
    body(poly(42, 54, 42, 78, 65, 66, amp = 0.3f), Cream, w = 3.2f, tex = 0)
    rotate(-10f, 12, 40) {
        val clap = rr(12, 22, 76, 16, 4, amp = 0.4f)
        body(clap, Cream, w = 3.6f, tex = 0)
        ds.clipPath(clap) {
            for (k in 0..4) drawPath(poly(16 + k * 16, 22, 26 + k * 16, 22, 18 + k * 16, 38, 8 + k * 16, 38, amp = 0.2f), Ink)
        }
        outline(clap, 3.6f)
    }
    star(88, 12, 5)
}

// ------------------------------------------------------------------ games

private fun Pen.minecraft() {
    val left = poly(10, 30, 50, 50, 50, 92, 10, 72, amp = 0.4f)
    val right = poly(50, 50, 90, 30, 90, 72, 50, 92, amp = 0.4f)
    val top = poly(50, 8, 90, 30, 50, 50, 10, 30, amp = 0.4f)
    body(left, Color(0xFF9A6B45), shadow = true, w = 3.8f, tex = 26)
    body(right, Color(0xFF7B5236), w = 3.8f, tex = 26)
    body(top, Color(0xFF5CC27A), w = 3.8f, tex = 20)
    ds.drawPath(poly(28, 24, 34, 27, 28, 30, 22, 27, amp = 0.1f), Color(0x33000000))
    ds.drawPath(poly(62, 34, 68, 37, 62, 40, 56, 37, amp = 0.1f), Color(0x33000000))
    star(88, 10, 5)
}

private fun Pen.shield() {
    body(soft(50, 8, 88, 20, 86, 56, 70, 80, 50, 94, 30, 80, 14, 56, 12, 20, amp = 0.6f), Blurple, shadow = true)
    body(soft(50, 18, 78, 27, 76, 54, 63, 72, 50, 82, 37, 72, 24, 54, 22, 27, amp = 0.4f), Sun, w = 3f, tex = 0)
    star(50, 48, 16)
    dot(50, 48, 3, Rose, 1.2f)
}

private fun Pen.flame() {
    body(soft(52, 6, 60, 24, 78, 42, 84, 64, 72, 86, 50, 94, 28, 86, 16, 64, 24, 44, 36, 34, 38, 20, amp = 0.6f), Color(0xFFFF6B4A), shadow = true)
    body(soft(50, 40, 62, 58, 66, 74, 50, 86, 34, 74, 38, 56, amp = 0.5f), Sun, w = 3f, tex = 0)
    body(soft(50, 62, 56, 72, 50, 80, 44, 72, amp = 0.3f), Cream, w = 2.4f, tex = 0)
    star(88, 14, 5)
}

private fun Pen.gamepad() {
    body(soft(14, 36, 30, 26, 70, 26, 86, 36, 94, 64, 88, 80, 76, 80, 66, 66, 34, 66, 24, 80, 12, 80, 6, 64, amp = 0.8f), fb(0), shadow = true)
    ln(Ink, 9, 31, 47, 31, 59)
    ln(Ink, 9, 25, 53, 37, 53)
    ln(Cream, 5.4f, 31, 47, 31, 59)
    ln(Cream, 5.4f, 25, 53, 37, 53)
    dot(70, 43, 4f, fb(1), 2f)
    dot(78, 51, 4f, fb(2), 2f)
    dot(62, 51, 4f, Cream, 2f)
    dot(70, 59, 4f, Coral, 2f)
    hi(22, 34, 34, 30)
    star(88, 12, 5)
}

// ------------------------------------------------------------------ category fallbacks (letter-based)

private fun Pen.fbSocial() {
    val c = fb(0)
    body(poly(14, 92, 22, 64, 42, 76, amp = 0.4f), c, tex = 0)
    body(blob(52, 46, 40, 38, amp = 1.1f), c, shadow = true)
    text(letter, 52, 46, 36, Cream)
    hi(24, 30, 32, 22)
    star(88, 10, 5)
}

private fun Pen.fbMedia() {
    val c = fb(0)
    body(rr(8, 18, 84, 64, 10, amp = 1f), c, shadow = true)
    for (k in 0 until 5) {
        ds.drawPath(rr(14 + k * 16, 22, 8, 6, 2, amp = 0.2f), Cream)
        ds.drawPath(rr(14 + k * 16, 72, 8, 6, 2, amp = 0.2f), Cream)
    }
    body(rr(18, 32, 64, 36, 6, amp = 0.4f), Cream.shade(0.04f), w = 3f, tex = 0)
    text(letter, 50, 50, 28, c.shade(0.05f), 3f)
    star(88, 10, 5)
}

private fun Pen.fbWork() {
    val c = fb(0)
    body(rr(18, 14, 64, 78, 9, amp = 0.9f), c, shadow = true)
    body(rr(36, 8, 28, 14, 5, amp = 0.4f), Color(0xFFB8A6F2), w = 3.4f, tex = 0)
    body(rr(26, 28, 48, 56, 5, amp = 0.4f), Cream, w = 3f, tex = 0)
    text(letter, 50, 54, 30, c.shade(0.08f), 3f)
    star(90, 14, 5)
}

private fun Pen.fbTools() {
    val c = fb(0)
    stroke(Ink, 9.5f, qc(34, 30, 50, 4, 66, 30))
    stroke(Cream, 5f, qc(34, 30, 50, 4, 66, 30))
    body(rr(8, 30, 84, 58, 10, amp = 1f), c, shadow = true)
    ln(Ink, 3, 8, 52, 92, 52)
    body(rr(42, 46, 16, 12, 3, amp = 0.3f), Sun, w = 2.8f, tex = 0)
    text(letter, 50, 74, 24, Cream, 3f)
    star(90, 12, 5)
}

// ------------------------------------------------------------------ control center art

private fun Pen.wifi() {
    body(blob(50, 50, 41, 41, amp = 0.9f, k = 13), Blurple, shadow = true)
    for (r in listOf(14, 26, 38)) {
        fat(arc(50, 74, r, -135f, 90f), Cream, 5.5f)
    }
    dot(50, 74, 5, Cream, 2.6f)
    star(86, 14, 5)
}

private fun Pen.bluetooth() {
    body(blob(50, 50, 41, 41, amp = 0.9f, k = 13), Sky, shadow = true)
    val rune = Path().apply {
        moveTo(35 * u, 35 * u); lineTo(65 * u, 65 * u); lineTo(50 * u, 80 * u); lineTo(50 * u, 20 * u); lineTo(65 * u, 35 * u); lineTo(35 * u, 65 * u)
    }
    fat(rune, Cream, 6f)
    star(86, 14, 5)
}

private fun Pen.sunArt() {
    for (k in 0 until 8) {
        val rad = Math.toRadians(k * 45.0)
        val x1 = 50 + 28 * cos(rad); val y1 = 50 + 28 * sin(rad)
        val x2 = 50 + 41 * cos(rad); val y2 = 50 + 41 * sin(rad)
        ln(Ink, 9, x1, y1, x2, y2)
        ln(Sun, 5.4f, x1, y1, x2, y2)
    }
    body(blob(50, 50, 24, 24, amp = 0.7f), Sun, shadow = true, tex = 10)
    hi(40, 42, 46, 36)
}

private fun Pen.torch() {
    rotate(-30f, 50, 50) {
        body(poly(30, 12, 70, 12, 60, 38, 40, 38, amp = 0.5f), Sun, w = 3.8f, tex = 0)
        body(rr(40, 36, 20, 52, 6, amp = 0.6f), Color(0xFFB8A6F2), shadow = true)
        body(rr(44, 52, 12, 8, 3, amp = 0.3f), Coral, w = 2.6f, tex = 0)
    }
    ln(Sun, 3, 8, 8, 18, 14)
    ln(Sun, 3, 5, 24, 16, 26)
    star(88, 14, 5)
}

private fun Pen.shot() {
    body(blob(50, 50, 41, 41, amp = 0.9f, k = 13), Peach, shadow = true)
    fat(Path().apply { moveTo(26 * u, 40 * u); lineTo(26 * u, 26 * u); lineTo(40 * u, 26 * u) }, Cream, 5f)
    fat(Path().apply { moveTo(60 * u, 26 * u); lineTo(74 * u, 26 * u); lineTo(74 * u, 40 * u) }, Cream, 5f)
    fat(Path().apply { moveTo(26 * u, 60 * u); lineTo(26 * u, 74 * u); lineTo(40 * u, 74 * u) }, Cream, 5f)
    fat(Path().apply { moveTo(60 * u, 74 * u); lineTo(74 * u, 74 * u); lineTo(74 * u, 60 * u) }, Cream, 5f)
    dot(50, 50, 5, Cream, 2.6f)
}

private fun Pen.battery() {
    body(rr(8, 28, 76, 46, 10, amp = 0.9f), Mint, shadow = true)
    body(rr(85, 43, 8, 16, 3, amp = 0.3f), Mint.shade(0.1f), w = 3.4f, tex = 0)
    body(poly(54, 34, 36, 54, 48, 54, 43, 70, 64, 46, 52, 46, amp = 0.2f), Cream, w = 3f, tex = 0)
    star(88, 14, 5)
}
