package com.rupzha.launcher

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Label(
    text: String,
    color: Color,
    size: Int = 13,
    weight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    align: TextAlign = TextAlign.Start,
    spacing: Float = 0f,
) {
    BasicText(
        text,
        modifier,
        style = TextStyle(color = color, fontSize = size.sp, fontWeight = weight, textAlign = align, letterSpacing = spacing.sp),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Large illustrated app icon with its name underneath. */
@Composable
fun AppTile(
    app: AppEntry,
    iconSize: Dp,
    style: IconStyle,
    anim: AnimLevel,
    textColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.pressable(anim, onLongClick, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app, iconSize, style)
        Spacer(Modifier.height(3.dp))
        Label(app.label, textColor, 12, FontWeight.SemiBold, Modifier.width(iconSize + 10.dp), align = TextAlign.Center)
    }
}

/** Dim scrim + centered card. Tapping outside dismisses. */
@Composable
fun Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(120)), exit = fadeOut(tween(100))) {
        Box(
            Modifier.fillMaxSize().background(Color(0x99060A22))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(visible, enter = scaleIn(tween(160), 0.92f), exit = scaleOut(tween(100), 0.95f)) {
                Box(Modifier.clickable(remember { MutableInteractionSource() }, null) {}) { content() }
            }
        }
    }
}

@Composable
fun MenuRow(text: String, p: Palette, danger: Boolean = false, onClick: () -> Unit) {
    Label(
        text,
        if (danger) Color(0xFFC2352F) else p.onPaper,
        16,
        FontWeight.Bold,
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

/** Long-press menu for an app. */
@Composable
fun AppMenu(
    app: AppEntry?,
    isFav: Boolean,
    p: Palette,
    s: Settings,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onToggleFav: () -> Unit,
    onHide: () -> Unit,
    onCategory: () -> Unit,
) {
    // Keep the last app around so the exit animation doesn't flash empty content.
    val shown = remember { arrayOfNulls<AppEntry>(1) }
    if (app != null) shown[0] = app
    val a = shown[0]
    Sheet(app != null, onDismiss) {
        if (a != null) {
            Column(
                Modifier.width(270.dp).inkSurface(p.paperA, p, s.cardStyle, seed = 21).padding(start = 3.dp, top = 3.dp, end = 8.dp, bottom = 8.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(a, 54.dp, s.iconStyle)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Label(a.label, p.onPaper, 17, FontWeight.Black)
                        Label(a.category.label, p.onPaperDim, 11, spacing = 1.5f)
                    }
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0x220B1030)))
                MenuRow("Open", p) { onOpen(); onDismiss() }
                MenuRow(if (isFav) "Remove from favorites" else "Add to favorites", p) { onToggleFav(); onDismiss() }
                MenuRow("Change category", p) { onCategory() }
                MenuRow("App info", p) { onInfo(); onDismiss() }
                MenuRow("Hide app", p) { onHide(); onDismiss() }
                MenuRow("Uninstall", p, danger = true) { onUninstall(); onDismiss() }
            }
        }
    }
}

@Composable
fun CategoryPicker(
    app: AppEntry?,
    p: Palette,
    s: Settings,
    onPick: (Category?) -> Unit,
    onDismiss: () -> Unit,
) {
    val shown = remember { arrayOfNulls<AppEntry>(1) }
    if (app != null) shown[0] = app
    Sheet(app != null, onDismiss) {
        Column(Modifier.width(250.dp).inkSurface(p.paperA, p, s.cardStyle, seed = 33).padding(start = 3.dp, top = 3.dp, end = 8.dp, bottom = 8.dp)) {
            Label("Category", p.onPaper, 17, FontWeight.Black, Modifier.padding(16.dp))
            Category.entries.filter { it != Category.ALL }.forEach { c ->
                MenuRow(c.label.lowercase().replaceFirstChar { it.uppercase() }, p) { onPick(c); onDismiss() }
            }
            MenuRow("Automatic", p) { onPick(null); onDismiss() }
        }
    }
}

enum class Tab { HOME, APPS, SEARCH }

/** Bottom navigation: three chunky ink pills, the selected one filled with the accent color. */
@Composable
fun BottomNav(current: Tab, p: Palette, s: Settings, onTab: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 24.dp).fillMaxWidth().height(72.dp)
            .inkSurface(p.surface, p, s.cardStyle, seed = 51, radius = 30.dp, scratches = false)
            .padding(start = 8.dp, top = 8.dp, end = 13.dp, bottom = 13.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            Triple(Tab.HOME, Glyph.HOME, "HOME"),
            Triple(Tab.APPS, Glyph.APPS, "APPS"),
            Triple(Tab.SEARCH, Glyph.SEARCH, "SEARCH"),
        ).forEach { (tab, glyph, name) ->
            val sel = tab == current
            Row(
                Modifier.weight(1f).fillMaxSize().padding(horizontal = 3.dp)
                    .then(if (sel) Modifier.inkSurface(p.accent, p, CardStyle.FLAT, seed = 60 + tab.ordinal, radius = 20.dp, scratches = false) else Modifier)
                    .pressable(s.anim) { onTab(tab) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(glyph, 22.dp, if (sel) p.onAccent else p.textDim)
                if (sel) {
                    Spacer(Modifier.width(6.dp))
                    Label(name, p.onAccent, 12, FontWeight.Black, spacing = 1f)
                }
            }
        }
    }
}

/** Small round ink button with a glyph, used for header actions. */
@Composable
fun InkButton(glyph: Glyph, p: Palette, s: Settings, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).inkSurface(p.surface, p, s.cardStyle, seed = 77, radius = size / 2, elevate = 3.dp, scratches = false)
            .pressable(s.anim, null, onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, size * 0.5f, p.text)
    }
}
