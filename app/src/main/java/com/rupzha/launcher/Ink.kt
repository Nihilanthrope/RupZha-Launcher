package com.rupzha.launcher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------------------------
// Deterministic hand-drawn wobble
// ---------------------------------------------------------------------------------------------

/** Deterministic pseudo-random value in [-1, 1]. Same seed + index always gives the same wobble. */
internal fun noise(seed: Int, i: Int): Float {
    var h = seed * 374761393 + i * 668265263 + 0x9E3779B9.toInt()
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFF) / 32767.5f - 1f
}

/** A rounded rectangle whose edges bow slightly, like it was drawn with a marker. All values in px. */
internal fun wobblyRRect(x: Float, y: Float, w: Float, h: Float, r: Float, amp: Float, seed: Int, salt: Int = 0): Path {
    val rr = r.coerceAtMost(minOf(w, h) / 2f).coerceAtLeast(0f)
    fun j(i: Int) = noise(seed, salt + i) * amp
    return Path().apply {
        moveTo(x + rr, y + j(0))
        quadraticTo(x + w / 2, y + j(1), x + w - rr, y + j(2))
        quadraticTo(x + w, y, x + w + j(4), y + rr)
        quadraticTo(x + w + j(5), y + h / 2, x + w + j(6), y + h - rr)
        quadraticTo(x + w, y + h, x + w - rr, y + h + j(8))
        quadraticTo(x + w / 2, y + h + j(9), x + rr, y + h + j(10))
        quadraticTo(x, y + h, x + j(12), y + h - rr)
        quadraticTo(x + j(13), y + h / 2, x + j(14), y + rr)
        quadraticTo(x, y, x + rr, y + j(0))
        close()
    }
}

// ---------------------------------------------------------------------------------------------
// Ink surfaces (cards, pills, buttons)
// ---------------------------------------------------------------------------------------------

/**
 * Draws an illustrated card behind the content: wobbly outline, hard offset shadow, a few pencil scratches.
 * The bottom/right [elevate] px of the box hold the shadow, so give content matching padding.
 */
fun Modifier.inkSurface(
    fill: Color,
    palette: Palette,
    style: CardStyle,
    seed: Int = 7,
    radius: Dp = 24.dp,
    outline: Dp = 2.5.dp,
    elevate: Dp = 5.dp,
    scratches: Boolean = true,
): Modifier = drawWithCache {
    val o = outline.toPx()
    val e = if (style == CardStyle.FLAT) 0f else elevate.toPx()
    val w = (size.width - o * 2 - e).coerceAtLeast(1f)
    val h = (size.height - o * 2 - e).coerceAtLeast(1f)
    val path = wobblyRRect(o, o, w, h, radius.toPx(), 1.4.dp.toPx(), seed)
    val stroke = Stroke(o, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val scratch = Color(0x2E0B1030)
    onDrawBehind {
        when (style) {
            CardStyle.INK -> translate(e, e) { drawPath(path, palette.shadow) }
            CardStyle.SOFT -> for (k in 1..3) translate(e * k / 3f, e * k / 3f) {
                drawPath(path, palette.shadow.copy(alpha = palette.shadow.alpha * 0.35f))
            }
            CardStyle.FLAT -> {}
        }
        drawPath(path, fill)
        if (scratches && w > 90.dp.toPx()) {
            val sx = o + w * 0.06f
            val sy = o + h - 12.dp.toPx()
            for (k in 0 until 3) {
                drawLine(
                    scratch, Offset(sx + k * 6.dp.toPx(), sy), Offset(sx + k * 6.dp.toPx() + 7.dp.toPx(), sy - 7.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round,
                )
            }
        }
        drawPath(path, palette.outline, style = stroke)
    }
}

/** Press-down spring + haptic on long press. Scale is skipped entirely when animations are off. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    anim: AnimLevel,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && anim != AnimLevel.OFF) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = if (anim == AnimLevel.LIVELY) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = src,
            indication = null,
            onLongClick = onLongClick?.let { lc -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); lc() } },
            onClick = onClick,
        )
}

// ---------------------------------------------------------------------------------------------
// Typography
// ---------------------------------------------------------------------------------------------

/** Chunky outlined lettering: stroke layer + fill layer, with an optional hard offset shadow. */
@Composable
fun InkText(
    text: String,
    size: TextUnit,
    fill: Color,
    modifier: Modifier = Modifier,
    outline: Color = Ink,
    stroke: Dp = 4.dp,
    shadow: Color? = null,
    family: FontFamily = FontFamily.SansSerif,
    letterSpacing: TextUnit = 0.sp,
) {
    val strokePx = with(LocalDensity.current) { stroke.toPx() }
    val base = TextStyle(
        fontSize = size, fontFamily = family, fontWeight = FontWeight.Black,
        letterSpacing = letterSpacing, lineHeight = size,
    )
    Box(modifier) {
        if (shadow != null) {
            BasicText(
                text,
                Modifier.offset(3.dp, 3.dp),
                style = base.copy(color = shadow, drawStyle = Stroke(strokePx, join = StrokeJoin.Round)),
            )
            BasicText(text, Modifier.offset(3.dp, 3.dp), style = base.copy(color = shadow))
        }
        BasicText(text, style = base.copy(color = outline, drawStyle = Stroke(strokePx, join = StrokeJoin.Round)))
        BasicText(text, style = base.copy(color = fill))
    }
}

val Handwriting: FontFamily = FontFamily.Cursive

// ---------------------------------------------------------------------------------------------
// Glyphs: little ink-drawn UI symbols (status, transport, nav)
// ---------------------------------------------------------------------------------------------

enum class Glyph { WIFI, CELL, NONET, BOLT, PLAY, PAUSE, NEXT, PREV, SEARCH, HOME, APPS, GEAR, CLOSE, PLUS, BACK, CHIP, DISK, BATTERY }

@Composable
fun GlyphIcon(g: Glyph, size: Dp, tint: Color, modifier: Modifier = Modifier, ink: Color? = null, fill: Float = 1f) {
    Canvas(modifier.size(size)) { drawGlyph(g, tint, ink, this.size.minDimension, fill) }
}

private fun DrawScope.drawGlyph(g: Glyph, tint: Color, ink: Color?, px: Float, fillPct: Float) {
    val u = px / 24f
    val w = 2.3f * u
    fun st(width: Float) = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun shape(p: Path, filled: Boolean) {
        if (ink != null) drawPath(p, ink, style = st(w + 2.4f * u))
        if (filled) drawPath(p, tint) else drawPath(p, tint, style = st(w))
        if (filled && ink == null) drawPath(p, tint, style = st(w * 0.5f))
    }
    fun poly(vararg v: Float): Path = Path().apply {
        for (i in 0 until v.size / 2) if (i == 0) moveTo(v[0] * u, v[1] * u) else lineTo(v[i * 2] * u, v[i * 2 + 1] * u)
        close()
    }
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = Path().apply { moveTo(x1 * u, y1 * u); lineTo(x2 * u, y2 * u) }
    fun rrect(x: Float, y: Float, wd: Float, h: Float, r: Float) = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(x * u, y * u, (x + wd) * u, (y + h) * u, CornerRadius(r * u)))
    }

    when (g) {
        Glyph.PLAY -> shape(poly(7f, 4f, 19.5f, 12f, 7f, 20f), true)
        Glyph.PAUSE -> { shape(rrect(5.5f, 4f, 4.8f, 16f, 1.6f), true); shape(rrect(13.7f, 4f, 4.8f, 16f, 1.6f), true) }
        Glyph.NEXT -> { shape(poly(5f, 5f, 15f, 12f, 5f, 19f), true); shape(rrect(16.5f, 5f, 3.2f, 14f, 1.4f), true) }
        Glyph.PREV -> { shape(poly(19f, 5f, 9f, 12f, 19f, 19f), true); shape(rrect(4.3f, 5f, 3.2f, 14f, 1.4f), true) }
        Glyph.SEARCH -> {
            shape(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(4.5f * u, 4.5f * u), Size(11f * u, 11f * u))) }, false)
            shape(line(14f, 14f, 20f, 20f), false)
        }
        Glyph.HOME -> shape(poly(4f, 12f, 12f, 4f, 20f, 12f, 20f, 20f, 14.5f, 20f, 14.5f, 14f, 9.5f, 14f, 9.5f, 20f, 4f, 20f), true)
        Glyph.APPS -> {
            shape(rrect(4f, 4f, 7f, 7f, 2f), true); shape(rrect(13f, 4f, 7f, 7f, 2f), true)
            shape(rrect(4f, 13f, 7f, 7f, 2f), true); shape(rrect(13f, 13f, 7f, 7f, 2f), true)
        }
        Glyph.GEAR -> {
            shape(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(7f * u, 7f * u), Size(10f * u, 10f * u))) }, false)
            for (k in 0 until 8) {
                val a = Math.toRadians(k * 45.0)
                val c = kotlin.math.cos(a).toFloat(); val s = kotlin.math.sin(a).toFloat()
                shape(line(12f + c * 7f, 12f + s * 7f, 12f + c * 9.5f, 12f + s * 9.5f), false)
            }
        }
        Glyph.CLOSE -> { shape(line(6f, 6f, 18f, 18f), false); shape(line(18f, 6f, 6f, 18f), false) }
        Glyph.PLUS -> { shape(line(12f, 5f, 12f, 19f), false); shape(line(5f, 12f, 19f, 12f), false) }
        Glyph.BACK -> shape(Path().apply { moveTo(15f * u, 5f * u); lineTo(8f * u, 12f * u); lineTo(15f * u, 19f * u) }, false)
        Glyph.BOLT -> shape(poly(13f, 2.5f, 5f, 13.5f, 11f, 13.5f, 10f, 21.5f, 19f, 10f, 13f, 10f), true)
        Glyph.WIFI, Glyph.NONET -> {
            val c = if (g == Glyph.NONET) tint.copy(alpha = 0.4f) else tint
            for (r in listOf(5f, 9f, 13f)) {
                val p = Path().apply {
                    addArc(
                        androidx.compose.ui.geometry.Rect(Offset((12f - r) * u, (19f - r) * u), Size(r * 2 * u, r * 2 * u)),
                        -135f, 90f,
                    )
                }
                if (ink != null) drawPath(p, ink, style = st(w + 2.4f * u))
                drawPath(p, c, style = st(w))
            }
            drawCircle(c, 1.7f * u, Offset(12f * u, 19f * u))
        }
        Glyph.CELL -> for (k in 0 until 4) {
            val h = 5f + k * 4f
            shape(rrect(3.5f + k * 4.7f, 20f - h, 3f, h, 1f), k < 3 || fillPct > 0f)
        }
        Glyph.CHIP -> {
            shape(rrect(6f, 6f, 12f, 12f, 2.5f), false)
            for (k in 0 until 3) {
                val p = 9f + k * 3f
                shape(line(p, 2.5f, p, 6f), false); shape(line(p, 18f, p, 21.5f), false)
                shape(line(2.5f, p, 6f, p), false); shape(line(18f, p, 21.5f, p), false)
            }
        }
        Glyph.DISK -> {
            shape(rrect(3.5f, 7f, 17f, 10f, 3f), false)
            drawCircle(tint, 1.2f * u, Offset(7.5f * u, 12f * u))
            shape(line(11f, 12f, 17f, 12f), false)
        }
        Glyph.BATTERY -> {
            shape(rrect(2.5f, 7f, 17f, 10f, 2.5f), false)
            shape(rrect(20f, 10.5f, 2f, 3f, 1f), true)
            if (fillPct > 0.02f) drawPath(rrect(4.7f, 9.2f, 12.6f * fillPct.coerceIn(0f, 1f), 5.6f, 1.2f), tint)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Background: flat color + a few seeded doodles, drawn once
// ---------------------------------------------------------------------------------------------

fun Modifier.paperBackground(p: Palette): Modifier = drawWithCache {
    val doodle = p.text.copy(alpha = if (p.dark) 0.07f else 0.09f)
    val items = 46
    onDrawBehind {
        drawRect(p.bg)
        for (i in 0 until items) {
            val x = (noise(11, i * 3) * 0.5f + 0.5f) * size.width
            val y = (noise(11, i * 3 + 1) * 0.5f + 0.5f) * size.height
            val kind = ((noise(11, i * 3 + 2) * 0.5f + 0.5f) * 4).toInt()
            val r = (3 + (noise(13, i) * 0.5f + 0.5f) * 4).dp.toPx()
            when (kind) {
                0 -> drawCircle(doodle, r * 0.45f, Offset(x, y))
                1 -> { // plus
                    drawLine(doodle, Offset(x - r, y), Offset(x + r, y), 1.6.dp.toPx(), StrokeCap.Round)
                    drawLine(doodle, Offset(x, y - r), Offset(x, y + r), 1.6.dp.toPx(), StrokeCap.Round)
                }
                2 -> { // tiny sparkle
                    val s = Path().apply {
                        moveTo(x, y - r); lineTo(x + r * .3f, y - r * .3f); lineTo(x + r, y); lineTo(x + r * .3f, y + r * .3f)
                        lineTo(x, y + r); lineTo(x - r * .3f, y + r * .3f); lineTo(x - r, y); lineTo(x - r * .3f, y - r * .3f); close()
                    }
                    drawPath(s, doodle)
                }
                else -> drawLine(doodle, Offset(x - r, y + r * .6f), Offset(x + r, y - r * .6f), 1.6.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
fun FillBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize()) { content() }
}
