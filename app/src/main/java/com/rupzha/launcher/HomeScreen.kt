package com.rupzha.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Quotes = listOf(
    "Good things take time.", "Stay curious.", "Make something.", "Small steps still count.",
    "Begin before you're ready.", "Draw outside the lines.", "One thing at a time.",
)

private fun greeting(h: Int) = when (h) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Still up?"
}

@Composable
fun HomeScreen(vm: LauncherViewModel, p: Palette, s: Settings, onOpenControl: () -> Unit, onOpenWorkspaces: () -> Unit, onOpenSettings: () -> Unit, onPickHero: () -> Unit, onMenu: (AppEntry) -> Unit) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsState()
    val hero by vm.hero.collectAsState()
    val now by rememberNow()
    val battery by rememberBattery()
    val net by rememberNetwork()
    val (ram, disk) = rememberRamAndStorage()

    val byKey = remember(apps) { apps.associateBy { it.key } }
    val favs = remember(apps, s.favorites) { s.favorites.mapNotNull { byKey[it] } }
    val cal = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val quote = remember(cal.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.HOUR_OF_DAY) / 3) {
        Quotes[(cal.get(Calendar.DAY_OF_YEAR) * 7 + cal.get(Calendar.HOUR_OF_DAY) / 3) % Quotes.size]
    }
    val timeText = remember(now) { SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now)) }
    val ampm = remember(now) { SimpleDateFormat("a", Locale.getDefault()).format(Date(now)).uppercase() }
    val dateText = remember(now) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "topbar", contentType = "topbar") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                InkText("RUPZHA", 20.sp, p.accent, outline = Ink, stroke = 3.dp, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                when (net) {
                    NetKind.WIFI -> GlyphIcon(Glyph.WIFI, 20.dp, p.text)
                    NetKind.CELL -> GlyphIcon(Glyph.CELL, 20.dp, p.text)
                    NetKind.OTHER -> GlyphIcon(Glyph.WIFI, 20.dp, p.text)
                    NetKind.NONE -> GlyphIcon(Glyph.NONET, 20.dp, p.text)
                }
                Spacer(Modifier.width(10.dp))
                GlyphIcon(Glyph.BATTERY, 24.dp, if (battery.pct <= 15 && !battery.charging) Color(0xFFFF6B5E) else p.text, fill = battery.pct / 100f)
                Spacer(Modifier.width(3.dp))
                Label("${battery.pct}%", p.text, 13, FontWeight.Bold)
                if (battery.charging) GlyphIcon(Glyph.BOLT, 14.dp, p.accent)
                Spacer(Modifier.width(12.dp))
                InkButton(Glyph.GEAR, p, s, 38.dp, onOpenSettings)
            }
        }

        item(key = "clock", contentType = "clock") {
            Column {
                Label(greeting(cal.get(Calendar.HOUR_OF_DAY)), p.textDim, 15, FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.Bottom) {
                    InkText(timeText, 84.sp, p.paperA, shadow = p.accent, stroke = 6.dp)
                    Spacer(Modifier.width(12.dp))
                    Label(ampm, p.accent, 20, FontWeight.Black, Modifier.padding(bottom = 14.dp))
                }
                Spacer(Modifier.height(4.dp))
                Label(dateText, p.text, 18, FontWeight.Bold)
                androidx.compose.foundation.text.BasicText(
                    quote,
                    Modifier.padding(top = 6.dp),
                    style = androidx.compose.ui.text.TextStyle(color = p.accent, fontSize = 22.sp, fontFamily = Handwriting),
                )
            }
        }

        if (s.layout == HomeLayout.DASHBOARD) {
            item(key = "hero", contentType = "hero") { HeroCard(hero, p, s, onPick = onPickHero) }
        }

        item(key = "quick-title", contentType = "title") { SectionTitle("QUICK APPS", p) }

        if (favs.isEmpty()) {
            item(key = "quick-empty", contentType = "text") {
                Label("Long-press any app in the drawer and choose “Add to favorites”.", p.textDim, 13)
            }
        } else {
            val iconSize = (66 * s.iconScale).dp
            val rows = favs.chunked(4)
            items(
                items = rows,
                key = { row -> "apps-" + row.joinToString("|") { it.key } },
                contentType = { "app-row" },
            ) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { a ->
                        AppTile(a, iconSize, s.iconStyle, s.anim, p.text, { Actions.launch(ctx, a) }, { onMenu(a) })
                    }
                    repeat(4 - row.size) { Spacer(Modifier.width(iconSize + 10.dp)) }
                }
            }
        }

        if (s.layout == HomeLayout.DASHBOARD) {
            item(key = "media", contentType = "media") { MediaCard(vm, p, s) }
            item(key = "device", contentType = "device") { DeviceCard(battery, ram.value, disk.value, net, p, s) }
        }

        item(key = "shortcuts", contentType = "shortcuts") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ShortcutCard("2UP WORKSPACES", "Launch app pairs", Art.FILES, p.paperB, p, s, Modifier.weight(1f), onOpenWorkspaces)
                ShortcutCard("CONTROL CENTER", "Quick toggles", Art.SETTINGS, p.paperC, p, s, Modifier.weight(1f), onOpenControl)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, p: Palette) {
    Label(text, p.textDim, 12, FontWeight.Black, spacing = 2f)
}

@Composable
private fun ShortcutCard(title: String, sub: String, art: Art, fill: Color, p: Palette, s: Settings, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(84.dp).inkSurface(fill, p, s.cardStyle, seed = title.length)
            .padding(start = 12.dp, end = 17.dp, top = 8.dp, bottom = 13.dp).pressable(s.anim, null, onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtIcon(art, 44.dp, s.iconStyle)
        Spacer(Modifier.width(8.dp))
        Column {
            Label(title, p.onPaper, 12, FontWeight.Black, maxLines = 2)
            Label(sub, p.onPaperDim, 11, FontWeight.Medium)
        }
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun HeroCard(hero: ImageBitmap?, p: Palette, s: Settings, onPick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.7f).inkSurface(p.paperB, p, s.cardStyle, seed = 5, radius = 28.dp)
            .padding(start = 5.dp, top = 5.dp, end = 10.dp, bottom = 10.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .pressable(s.anim, null, onPick),
    ) {
        if (hero != null) {
            Image(hero, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            DefaultHero(p)
        }
    }
}

/** RUPZHA's own artwork: sunset hills, a moon, clouds and little stars. Drawn once, never animated. */
@Composable
private fun DefaultHero(p: Palette) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(Color(0xFF2B2A63))
        drawRect(Color(0xFF5B4BA8), Offset(0f, h * 0.35f), androidx.compose.ui.geometry.Size(w, h * 0.65f))
        drawRect(Color(0xFFE88FA0), Offset(0f, h * 0.55f), androidx.compose.ui.geometry.Size(w, h * 0.45f))
        drawCircle(Color(0xFFFFE2B8), h * 0.2f, Offset(w * 0.72f, h * 0.38f))
        drawCircle(Color(0x33FFE2B8), h * 0.28f, Offset(w * 0.72f, h * 0.38f))
        fun hill(color: Color, baseY: Float, amp: Float, phase: Float) {
            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY)
                var x = 0f
                while (x <= w) {
                    lineTo(x, baseY - kotlin.math.sin(x / w * 6.28f + phase) * amp)
                    x += w / 40f
                }
                lineTo(w, h); close()
            }
            drawPath(path, color)
            drawPath(path, Ink, style = Stroke(3.dp.toPx()))
        }
        hill(Color(0xFF7C5CC4), h * 0.68f, h * 0.07f, 0.5f)
        hill(Color(0xFF3F3A8C), h * 0.8f, h * 0.06f, 2.4f)
        hill(Color(0xFF1E2252), h * 0.92f, h * 0.05f, 4.1f)
        for (i in 0 until 16) {
            val x = (noise(3, i) * 0.5f + 0.5f) * w
            val y = (noise(4, i) * 0.5f + 0.5f) * h * 0.45f
            drawCircle(Color(0xCCFFF3DC), (1.2f + (noise(5, i) * 0.5f + 0.5f) * 1.8f).dp.toPx(), Offset(x, y))
        }
        // tiny clouds
        for ((cx, cy) in listOf(0.22f to 0.28f, 0.45f to 0.18f)) {
            val c = Offset(w * cx, h * cy)
            drawCircle(Color(0xFFF6EBDD), 12.dp.toPx(), c)
            drawCircle(Color(0xFFF6EBDD), 16.dp.toPx(), c + Offset(16.dp.toPx(), -4.dp.toPx()))
            drawCircle(Color(0xFFF6EBDD), 11.dp.toPx(), c + Offset(32.dp.toPx(), 2.dp.toPx()))
        }
    }
    Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.BottomStart) {
        InkText("tap to pick your artwork", 14.sp, Cream, stroke = 3.dp, family = Handwriting)
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun MediaCard(vm: LauncherViewModel, p: Palette, s: Settings) {
    val ctx = LocalContext.current
    val st by vm.media.state.collectAsState()
    val hasAccess by vm.media.access.collectAsState()
    val m = st

    Row(
        Modifier.fillMaxWidth().inkSurface(p.paperC, p, s.cardStyle, seed = 9)
            .padding(start = 14.dp, top = 14.dp, end = 19.dp, bottom = 19.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Album art as a little vinyl sleeve
        Box(Modifier.size(78.dp).inkSurface(p.paperA, p, CardStyle.FLAT, seed = 12, radius = 16.dp, outline = 2.5.dp, scratches = false).padding(3.dp)) {
            val art = m?.art
            if (art != null) {
                Image(art.asImageBitmap(), null, Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            } else {
                ArtIcon(Art.MUSIC, 66.dp, s.iconStyle)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Label(
                m?.title ?: if (hasAccess) "Nothing playing" else "Music card",
                p.onPaper, 16, FontWeight.Black,
            )
            Label(
                m?.artist?.ifEmpty { "Unknown artist" } ?: if (hasAccess) "Start a song in any app" else "Tap to allow notification access",
                p.onPaperDim, 13, FontWeight.Medium,
                Modifier.then(if (m == null && !hasAccess) Modifier.pressable(s.anim, null) { Actions.openNotificationAccess(ctx) } else Modifier),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TransportButton(Glyph.PREV, 38.dp, p.paperA, p, s) { vm.media.previous() }
                TransportButton(if (m?.playing == true) Glyph.PAUSE else Glyph.PLAY, 46.dp, p.accent, p, s) { vm.media.playPause() }
                TransportButton(Glyph.NEXT, 38.dp, p.paperA, p, s) { vm.media.next() }
            }
        }
    }
}

@Composable
private fun TransportButton(g: Glyph, size: androidx.compose.ui.unit.Dp, fill: Color, p: Palette, s: Settings, onClick: () -> Unit) {
    Box(
        Modifier.size(size).inkSurface(fill, p, s.cardStyle, seed = 41 + g.ordinal, radius = size / 2, elevate = 3.dp, outline = 2.2.dp, scratches = false)
            .pressable(s.anim, null, onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(g, size * 0.44f, p.onPaper)
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun DeviceCard(b: BatteryInfo, ram: MemInfo, disk: MemInfo, net: NetKind, p: Palette, s: Settings) {
    Column(
        Modifier.fillMaxWidth().inkSurface(p.paperA, p, s.cardStyle, seed = 14)
            .padding(start = 16.dp, top = 14.dp, end = 21.dp, bottom = 19.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Label("THIS PHONE", p.onPaperDim, 11, FontWeight.Black, spacing = 2f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(Glyph.BATTERY, "${b.pct}%", if (b.charging) "Charging" else "Battery", b.pct / 100f, Color(0xFF5FD3A5), p, Modifier.weight(1f))
            Stat(Glyph.CHIP, gb(ram.usedBytes), "GB RAM used", ram.usedBytes.toFloat() / ram.totalBytes, Color(0xFFB8A6F2), p, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(Glyph.DISK, gb(disk.totalBytes - disk.usedBytes), "GB free", 1f - disk.usedBytes.toFloat() / disk.totalBytes, Color(0xFFFFC95C), p, Modifier.weight(1f))
            val (netLabel, netGlyph) = when (net) {
                NetKind.WIFI -> "Wi-Fi" to Glyph.WIFI
                NetKind.CELL -> "Mobile data" to Glyph.CELL
                NetKind.OTHER -> "Connected" to Glyph.WIFI
                NetKind.NONE -> "Offline" to Glyph.NONET
            }
            Stat(netGlyph, netLabel, "Network", if (net == NetKind.NONE) 0f else 1f, Color(0xFF5BB8F5), p, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(g: Glyph, value: String, caption: String, frac: Float, bar: Color, p: Palette, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(g, 20.dp, p.onPaper)
            Spacer(Modifier.width(6.dp))
            Label(value, p.onPaper, 20, FontWeight.Black)
        }
        Label(caption, p.onPaperDim, 12, FontWeight.Medium)
        // hand-drawn progress bar
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            val h = size.height; val w = size.width
            val track = wobblyRRect(1f, 1f, w - 2f, h - 2f, h / 2, 0.8f, 3)
            drawPath(track, Color(0x1A0B1030))
            clipPath(track) {
                drawRect(bar, Offset.Zero, androidx.compose.ui.geometry.Size(w * frac.coerceIn(0f, 1f), h))
            }
            drawPath(track, Ink, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
