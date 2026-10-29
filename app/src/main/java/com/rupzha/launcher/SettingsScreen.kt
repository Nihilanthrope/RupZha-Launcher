package com.rupzha.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: LauncherViewModel, p: Palette, s: Settings, onBack: () -> Unit, onPickHero: () -> Unit) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsState()
    val hiddenApps = apps.filter { it.key in s.hidden }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InkButton(Glyph.BACK, p, s, 42.dp, onBack)
            Spacer(Modifier.width(14.dp))
            InkText("PERSONALIZE", 24.sp, p.accent, stroke = 4.dp, shadow = p.shadow)
        }

        Group("THEME", p, s) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeId.entries.forEach { t -> Chip(t.label, s.theme == t, p, s) { vm.update { it.copy(theme = t) } } }
            }
            if (s.theme == ThemeId.CUSTOM) {
                Label("Accent color", p.onPaperDim, 12, FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentChoices.forEach { c ->
                        Box(
                            Modifier.size(36.dp).inkSurface(c, p, CardStyle.FLAT, seed = c.toArgb(), radius = 18.dp, outline = if (s.accent == c.toArgb()) 4.dp else 2.dp, scratches = false)
                                .pressable(s.anim, null) { vm.update { it.copy(accent = c.toArgb()) } },
                        )
                    }
                }
            }
        }

        Group("ICONS", p, s) {
            Label("Style", p.onPaperDim, 12, FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconStyle.entries.forEach { st -> Chip(st.label, s.iconStyle == st, p, s) { vm.update { it.copy(iconStyle = st) } } }
            }
            Label("Size", p.onPaperDim, 12, FontWeight.Bold)
            Slider((s.iconScale - 0.8f) / 0.5f, p) { f -> vm.update { it.copy(iconScale = (0.8f + f * 0.5f).let { v -> (v * 20).toInt() / 20f }) } }
            Label("Drawer columns", p.onPaperDim, 12, FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 4, 5).forEach { n -> Chip("$n", s.gridCols == n, p, s) { vm.update { it.copy(gridCols = n) } } }
            }
        }

        Group("LOOK & MOTION", p, s) {
            Label("Cards", p.onPaperDim, 12, FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardStyle.entries.forEach { c -> Chip(c.label, s.cardStyle == c, p, s) { vm.update { it.copy(cardStyle = c) } } }
            }
            Label("Animation", p.onPaperDim, 12, FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimLevel.entries.forEach { a -> Chip(a.label, s.anim == a, p, s) { vm.update { it.copy(anim = a) } } }
            }
            Label("Home layout", p.onPaperDim, 12, FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeLayout.entries.forEach { l -> Chip(l.label, s.layout == l, p, s) { vm.update { it.copy(layout = l) } } }
            }
        }

        Group("WALLPAPER & ARTWORK", p, s) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("RUPZHA paper", !s.systemWallpaper, p, s) { vm.update { it.copy(systemWallpaper = false) } }
                Chip("System wallpaper", s.systemWallpaper, p, s) { vm.update { it.copy(systemWallpaper = true) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("PICK HOME ARTWORK", p.accent, p.onAccent, p, s) { onPickHero() }
                if (s.heroUri != null) PillButton("RESET", p.paperA, p.onPaper, p, s) { vm.update { it.copy(heroUri = null) } }
            }
            PillButton("CHANGE SYSTEM WALLPAPER", p.paperA, p.onPaper, p, s) { Actions.pickWallpaper(ctx) }
        }

        Group("PERMISSIONS", p, s) {
            Label("Everything is optional. Nothing is requested until you tap.", p.onPaperDim, 12)
            PillButton("MUSIC CARD · NOTIFICATION ACCESS", p.paperA, p.onPaper, p, s) { Actions.openNotificationAccess(ctx) }
            PillButton("SCREENSHOT / LOCK · ACCESSIBILITY", p.paperA, p.onPaper, p, s) { Actions.openSettings(ctx, android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS) }
            PillButton("BRIGHTNESS · MODIFY SETTINGS", p.paperA, p.onPaper, p, s) { Actions.requestWriteSettings(ctx) }
        }

        Group("HIDDEN APPS", p, s) {
            if (hiddenApps.isEmpty()) Label("Nothing hidden. Long-press an app and choose “Hide app”.", p.onPaperDim, 13)
            hiddenApps.forEach { a ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(a, 40.dp, s.iconStyle)
                    Spacer(Modifier.width(10.dp))
                    Label(a.label, p.onPaper, 15, FontWeight.Bold, Modifier.weight(1f))
                    PillButton("UNHIDE", p.accent, p.onAccent, p, s) { vm.unhide(a.key) }
                }
            }
        }

        Label("RUPZHA Launcher · no analytics, no network, no background services", p.textDim, 11, align = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Group(title: String, p: Palette, s: Settings, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().inkSurface(p.paperA, p, s.cardStyle, seed = title.length * 11)
            .padding(start = 16.dp, top = 14.dp, end = 21.dp, bottom = 19.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Label(title, p.onPaperDim, 11, FontWeight.Black, spacing = 2f)
        content()
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, p: Palette, s: Settings, onClick: () -> Unit) {
    Box(
        Modifier.inkSurface(if (selected) p.accent else p.paperA.copy(alpha = 0.6f), p, CardStyle.FLAT, seed = text.length, radius = 16.dp, outline = 2.2.dp, scratches = false)
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
            .pressable(s.anim, null, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, if (selected) p.onAccent else p.onPaper, 12, FontWeight.Black)
    }
}

@Composable
private fun Slider(value: Float, p: Palette, onChange: (Float) -> Unit) {
    val v = value.coerceIn(0f, 1f)
    Canvas(
        Modifier.fillMaxWidth().height(28.dp).pointerInput(Unit) {
            detectTapGestures { o -> onChange((o.x / size.width).coerceIn(0f, 1f)) }
        }.pointerInput(Unit) {
            detectHorizontalDragGestures { c, _ -> onChange((c.position.x / size.width).coerceIn(0f, 1f)) }
        },
    ) {
        val h = size.height; val w = size.width
        val track = wobblyRRect(2f, 6f, w - 4f, h - 12f, (h - 12f) / 2, 0.8f, 5)
        drawPath(track, Color(0x1F0B1030))
        clipPath(track) { drawRect(p.accent, Offset.Zero, androidx.compose.ui.geometry.Size(w * v, h)) }
        drawPath(track, Ink, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
        val kx = (w * v).coerceIn(12.dp.toPx(), w - 12.dp.toPx())
        drawCircle(Cream, 11.dp.toPx(), Offset(kx, h / 2))
        drawCircle(Ink, 11.dp.toPx(), Offset(kx, h / 2), style = Stroke(2.5.dp.toPx()))
    }
}
