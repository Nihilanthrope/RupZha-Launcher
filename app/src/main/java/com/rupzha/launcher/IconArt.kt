package com.rupzha.launcher

import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Every distinct piece of illustrated artwork in the RUPZHA icon pack. */
enum class Art {
    SPOTIFY, YOUTUBE, CHROME, TELEGRAM, DISCORD, WHATSAPP, INSTAGRAM, CAMERA, GALLERY, PLAYSTORE,
    SETTINGS, FILES, CALENDAR, CALCULATOR, MAPS, GMAIL, DRIVE, CHATGPT, GITHUB, TERMUX,
    PHONE, MESSAGES, CLOCK, MUSIC, BROWSER, NOTES, WEATHER, VIDEO,
    MINECRAFT, SHIELD, FLAME, GAMEPAD,
    FB_SOCIAL, FB_MEDIA, FB_WORK, FB_TOOLS, FB_SYSTEM,
    WIFI, BLUETOOTH, SUN, TORCH, SHOT, BATTERY,
}

/** Picks the artwork for an app. Known apps get hand-made art, everything else a category-based fallback. */
fun artFor(app: AppEntry): Art {
    val pkg = app.pkg.lowercase()
    val label = app.label.lowercase()
    val s = "$pkg $label"
    fun has(vararg k: String) = k.any { s.contains(it) }
    return when {
        has("spotify") -> Art.SPOTIFY
        has("youtube") && !has("music") -> Art.YOUTUBE
        has("chrome") -> Art.CHROME
        has("telegram") -> Art.TELEGRAM
        has("discord") -> Art.DISCORD
        has("whatsapp") -> Art.WHATSAPP
        has("instagram") -> Art.INSTAGRAM
        has("camera") -> Art.CAMERA
        has("gallery", "apps.photos", "galeri") || label == "photos" -> Art.GALLERY
        has("vending", "play store") -> Art.PLAYSTORE
        pkg == "com.android.settings" || label == "settings" -> Art.SETTINGS
        has("documentsui", "filemanager", "file manager", "nbu.files") || label == "files" -> Art.FILES
        has("calendar") -> Art.CALENDAR
        has("calculator") -> Art.CALCULATOR
        has("apps.maps") || label == "maps" -> Art.MAPS
        pkg == "com.google.android.gm" || label == "gmail" -> Art.GMAIL
        pkg == "com.google.android.apps.docs" || label == "drive" -> Art.DRIVE
        has("chatgpt", "openai", "claude") -> Art.CHATGPT
        has("github") -> Art.GITHUB
        has("termux") -> Art.TERMUX
        has("dialer") || label == "phone" -> Art.PHONE
        has("mms", "messaging") || label == "messages" -> Art.MESSAGES
        has("deskclock") || label == "clock" -> Art.CLOCK
        has("music", "audio", "soundcloud", "podcast") -> Art.MUSIC
        has("browser") -> Art.BROWSER
        has("notes", "notepad", "apps.keep") -> Art.NOTES
        has("weather") -> Art.WEATHER
        has("netflix", "hotstar", "primevideo", "video") -> Art.VIDEO
        has("minecraft") -> Art.MINECRAFT
        has("clashofclans", "clashroyale", "pubg", "bgmi", "codm", "callofduty") -> Art.SHIELD
        has("freefire", "garena") -> Art.FLAME
        else -> when (app.category) {
            Category.SOCIAL -> Art.FB_SOCIAL
            Category.MEDIA -> Art.FB_MEDIA
            Category.WORK -> Art.FB_WORK
            Category.GAMES -> Art.GAMEPAD
            Category.SYSTEM -> Art.FB_SYSTEM
            else -> Art.FB_TOOLS
        }
    }
}

private val fallbackArts = setOf(Art.FB_SOCIAL, Art.FB_MEDIA, Art.FB_WORK, Art.FB_TOOLS, Art.FB_SYSTEM, Art.GAMEPAD)

// ---------------------------------------------------------------------------------------------
// The pen: every icon is drawn through this in a 100x100 unit square.
// ---------------------------------------------------------------------------------------------

internal val Number.f: Float get() = toFloat()
internal fun Color.shade(f: Float) = lerp(this, Color(0xFF1A1440), f)
internal fun Color.tint(f: Float) = lerp(this, Color.White, f)

internal class Pen(
    val ds: DrawScope,
    px: Float,
    val seed: Int,
    val style: IconStyle,
    val letter: String = "",
    val extra: Int = 0,
) {
    val u = px / 100f
    val sketch = style == IconStyle.SKETCH
    val flat = style == IconStyle.FLAT
    private val ow = if (sketch) 5.2f else 4.2f

    fun n(i: Int) = noise(seed, i)
    fun o(x: Number, y: Number) = Offset(x.f * u, y.f * u)

    // ---- shapes ----
    fun rr(x: Number, y: Number, w: Number, h: Number, r: Number, amp: Number = 0.8f, salt: Int = 0): Path =
        wobblyRRect(x.f * u, y.f * u, w.f * u, h.f * u, r.f * u, amp.f * u, seed, salt)

    fun smooth(pts: List<Offset>): Path {
        val p = Path()
        val k = pts.size
        fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val s = mid(pts[k - 1], pts[0])
        p.moveTo(s.x * u, s.y * u)
        for (i in 0 until k) {
            val c = pts[i]
            val m = mid(c, pts[(i + 1) % k])
            p.quadraticTo(c.x * u, c.y * u, m.x * u, m.y * u)
        }
        p.close()
        return p
    }

    fun blob(cx: Number, cy: Number, rx: Number, ry: Number = rx, amp: Number = 0.9f, k: Int = 12, salt: Int = 100): Path {
        val pts = List(k) { i ->
            val a = i * 2f * PI.toFloat() / k
            Offset(
                cx.f + cos(a) * (rx.f * 1.04f + n(salt + i) * amp.f),
                cy.f + sin(a) * (ry.f * 1.04f + n(salt + i + 50) * amp.f),
            )
        }
        return smooth(pts)
    }

    /** Smooth closed shape through the given points (x, y, x, y ...). Corners are softened. */
    fun soft(vararg v: Number, amp: Number = 0.6f, salt: Int = 200): Path =
        smooth(List(v.size / 2) { i -> Offset(v[i * 2].f + n(salt + i * 2) * amp.f, v[i * 2 + 1].f + n(salt + i * 2 + 1) * amp.f) })

    /** Straight-edged shape with a little hand jitter. */
    fun poly(vararg v: Number, amp: Number = 0.5f, salt: Int = 300): Path {
        val p = Path()
        for (i in 0 until v.size / 2) {
            val x = (v[i * 2].f + n(salt + i * 2) * amp.f) * u
            val y = (v[i * 2 + 1].f + n(salt + i * 2 + 1) * amp.f) * u
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    fun wedge(cx: Number, cy: Number, r: Number, a0: Float, a1: Float): Path {
        val p = Path()
        p.moveTo(cx.f * u, cy.f * u)
        var a = a0
        while (a <= a1) {
            val rad = Math.toRadians(a.toDouble())
            p.lineTo((cx.f + cos(rad).toFloat() * r.f) * u, (cy.f + sin(rad).toFloat() * r.f) * u)
            a += 8f
        }
        val rad = Math.toRadians(a1.toDouble())
        p.lineTo((cx.f + cos(rad).toFloat() * r.f) * u, (cy.f + sin(rad).toFloat() * r.f) * u)
        p.close()
        return p
    }

    // ---- painting ----
    fun crescent(path: Path, d: Float = 3.4f): Path {
        val shifted = Path().apply { addPath(path, Offset(-d * u, -d * 0.9f * u)) }
        return Path.combine(PathOperation.Difference, path, shifted)
    }

    fun rotate(deg: Float, cx: Number, cy: Number, block: () -> Unit) {
        ds.rotate(deg, o(cx, cy)) { block() }
    }

    /** Fill, painted shade crescent, texture, then the thick dark outline. */
    fun body(path: Path, fill: Color, shadow: Boolean = false, w: Number = ow, shadeColor: Color? = null, tex: Int = 14) {
        if (shadow && !flat) {
            ds.drawPath(Path().apply { addPath(path, Offset(1.9f * u, 2.6f * u)) }, Color(0x590B1030))
        }
        ds.drawPath(path, fill)
        if (!flat) ds.drawPath(crescent(path), shadeColor ?: fill.shade(if (sketch) 0.18f else 0.28f))
        if (tex > 0) texture(path, tex)
        outline(path, w)
    }

    fun outline(path: Path, w: Number = ow) {
        ds.drawPath(path, Ink, style = Stroke(w.f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    /** Speckle and (in sketch style) pencil hatching, clipped to the shape. */
    fun texture(path: Path, count: Int = 14) {
        if (flat) return
        val b = path.getBounds()
        ds.clipPath(path) {
            for (i in 0 until count) {
                val x = b.left + (n(400 + i * 2) * 0.5f + 0.5f) * b.width
                val y = b.top + (n(401 + i * 2) * 0.5f + 0.5f) * b.height
                drawCircle(Color(0x200B1030), (0.5f + (n(500 + i) * 0.5f + 0.5f) * 0.9f) * u, Offset(x, y))
            }
            if (sketch) {
                var x = b.left - b.height
                while (x < b.right) {
                    drawLine(
                        Color(0x260B1030), Offset(x, b.bottom), Offset(x + b.height, b.top),
                        strokeWidth = 0.9f * u, cap = StrokeCap.Round,
                    )
                    x += 4.6f * u
                }
            }
        }
    }

    fun stroke(c: Color, w: Number, path: Path) {
        ds.drawPath(path, c, style = Stroke(w.f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    /** Thick line with dark outline around it ("fat marker"). */
    fun fat(path: Path, c: Color, w: Number) {
        stroke(Ink, w.f + 3.4f, path)
        stroke(c, w, path)
    }

    fun ln(c: Color, w: Number, x0: Number, y0: Number, x1: Number, y1: Number) {
        ds.drawLine(c, o(x0, y0), o(x1, y1), w.f * u, StrokeCap.Round)
    }

    /** Polyline stroke: x, y, x, y ... */
    fun pl(c: Color, w: Number, vararg v: Number) {
        val p = Path()
        for (i in 0 until v.size / 2) if (i == 0) p.moveTo(v[0].f * u, v[1].f * u) else p.lineTo(v[i * 2].f * u, v[i * 2 + 1].f * u)
        stroke(c, w, p)
    }

    /** Single quadratic curve: start, control, end. */
    fun qc(x0: Number, y0: Number, cx: Number, cy: Number, x1: Number, y1: Number): Path = Path().apply {
        moveTo(x0.f * u, y0.f * u); quadraticTo(cx.f * u, cy.f * u, x1.f * u, y1.f * u)
    }

    fun hi(x0: Number, y0: Number, x1: Number, y1: Number, w: Number = 3f) {
        if (flat) return
        ds.drawLine(Color(0xB8FFFFFF), o(x0, y0), o(x1, y1), w.f * u, StrokeCap.Round)
    }

    fun dot(cx: Number, cy: Number, r: Number, c: Color, w: Number = 1.6f) {
        ds.drawCircle(c, r.f * u, o(cx, cy))
        ds.drawCircle(Ink, r.f * u, o(cx, cy), style = Stroke(w.f * u))
    }

    fun star(cx: Number, cy: Number, r: Number, c: Color = Cream) {
        val x = cx.f; val y = cy.f; val a = r.f; val b = r.f * 0.28f
        val p = poly(x, y - a, x + b, y - b, x + a, y, x + b, y + b, x, y + a, x - b, y + b, x - a, y, x - b, y - b, amp = 0.2f, salt = 700)
        ds.drawPath(p, c)
        ds.drawPath(p, Ink, style = Stroke(1.5f * u, join = StrokeJoin.Round))
    }

    fun text(s: String, cx: Number, cy: Number, size: Number, fill: Color, strokeW: Number = 3.4f) {
        if (s.isEmpty()) return
        ds.drawIntoCanvas { c ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = size.f * u
                typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
                strokeJoin = Paint.Join.ROUND
            }
            val y = cy.f * u - (paint.ascent() + paint.descent()) / 2f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeW.f * u
            paint.color = Ink.toArgb()
            c.nativeCanvas.drawText(s, cx.f * u, y, paint)
            paint.style = Paint.Style.FILL
            paint.color = fill.toArgb()
            c.nativeCanvas.drawText(s, cx.f * u, y, paint)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Rendering + cache. Icons are drawn once, off the main thread, into small bitmaps.
// ---------------------------------------------------------------------------------------------

internal data class IconSpec(val art: Art, val seed: Int, val letter: String, val style: IconStyle, val px: Int, val extra: Int)

object IconCache {
    private val lru = object : LruCache<IconSpec, ImageBitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: IconSpec, value: ImageBitmap) = value.width * value.height * 4
    }

    internal fun peek(spec: IconSpec): ImageBitmap? = lru.get(spec)

    internal fun render(spec: IconSpec): ImageBitmap {
        lru.get(spec)?.let { return it }
        val bmp = ImageBitmap(spec.px, spec.px, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(bmp)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, androidx.compose.ui.geometry.Size(spec.px.f, spec.px.f)) {
            Pen(this, spec.px.f, spec.seed, spec.style, spec.letter, spec.extra).drawArt(spec.art)
        }
        lru.put(spec, bmp)
        return bmp
    }

    internal fun specFor(app: AppEntry, style: IconStyle, px: Int): IconSpec {
        val art = artFor(app)
        val fallback = art in fallbackArts
        val letter = if (fallback) app.label.firstOrNull { it.isLetterOrDigit() }?.uppercase().orEmpty() else ""
        return IconSpec(
            art = art,
            seed = if (fallback) app.pkg.hashCode() else art.ordinal * 31 + 5,
            letter = letter,
            style = style,
            px = px,
            extra = if (art == Art.CALENDAR) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) else 0,
        )
    }

    /** Warm the cache for the apps most likely to be seen first. Safe to call from any background thread. */
    suspend fun prewarm(apps: List<AppEntry>, style: IconStyle, px: Int) = withContext(Dispatchers.Default) {
        for (a in apps) render(specFor(a, style, px))
    }
}

internal fun bucketPx(px: Int): Int = (((px + 31) / 32) * 32).coerceIn(96, 192)

@Composable
private fun IconImage(spec: IconSpec, size: Dp, modifier: Modifier) {
    val bmp by produceState(IconCache.peek(spec), spec) {
        if (value == null) value = withContext(Dispatchers.Default) { IconCache.render(spec) }
    }
    Box(modifier.size(size)) {
        bmp?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
fun AppIcon(app: AppEntry, size: Dp, style: IconStyle, modifier: Modifier = Modifier) {
    val px = bucketPx(with(LocalDensity.current) { size.roundToPx() })
    IconImage(IconCache.specFor(app, style, px), size, modifier)
}

@Composable
fun ArtIcon(art: Art, size: Dp, style: IconStyle, modifier: Modifier = Modifier) {
    val px = bucketPx(with(LocalDensity.current) { size.roundToPx() })
    IconImage(IconSpec(art, art.ordinal * 31 + 5, "", style, px, 0), size, modifier)
}
