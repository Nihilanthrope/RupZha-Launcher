package com.rupzha.launcher

import android.provider.Settings as AndroidSettings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One row in the command center. */
private class Hit(val title: String, val sub: String, val art: Art?, val app: AppEntry?, val go: () -> Unit)

private val systemCommands = listOf(
    Triple("battery", "Battery usage", { c: android.content.Context -> Actions.battery(c) }),
    Triple("wifi", "Wi-Fi panel", { c: android.content.Context -> Actions.wifi(c) }),
    Triple("bluetooth", "Bluetooth settings", { c: android.content.Context -> Actions.bluetooth(c) }),
    Triple("settings", "System settings", { c: android.content.Context -> Actions.openSettings(c) }),
    Triple("display", "Display settings", { c: android.content.Context -> Actions.openSettings(c, AndroidSettings.ACTION_DISPLAY_SETTINGS) }),
    Triple("sound", "Sound settings", { c: android.content.Context -> Actions.openSettings(c, AndroidSettings.ACTION_SOUND_SETTINGS) }),
    Triple("storage", "Storage settings", { c: android.content.Context -> Actions.openSettings(c, AndroidSettings.ACTION_INTERNAL_STORAGE_SETTINGS) }),
    Triple("calculator", "Open calculator", { c: android.content.Context -> Actions.calculator(c) }),
    Triple("camera", "Take a photo", { c: android.content.Context -> Actions.camera(c) }),
    Triple("screenshot", "Take a screenshot", { c: android.content.Context -> Actions.screenshot(c) }),
    Triple("notifications", "Pull down the shade", { c: android.content.Context -> Actions.notifications(c) }),
)

@Composable
fun SearchScreen(vm: LauncherViewModel, p: Palette, s: Settings, initial: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val allApps by vm.apps.collectAsState()
    val apps = remember(allApps, s.hidden, s.catOverrides) { visibleApps(allApps, s) }
    var q by remember(initial) { mutableStateOf(initial) }

    val hits = remember(q, apps) { buildHits(ctx, q, apps, onDone) }

    Column(Modifier.fillMaxSize().padding(top = 44.dp, start = 20.dp, end = 20.dp)) {
        InkText("COMMAND CENTER", 26.sp, p.accent, stroke = 4.dp, shadow = p.shadow, letterSpacing = 1.sp)
        Label("Type an app, a setting, or “open spotify”, “battery”, “search web …”", p.textDim, 13, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp), maxLines = 2)
        SearchField(q, { q = it }, "Search or run a command", p, s, onGo = { hits.firstOrNull()?.go?.invoke() }, autoFocus = true)
        Spacer(Modifier.height(14.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp, end = 5.dp),
        ) {
            items(hits, key = { it.title + it.sub }) { h ->
                Row(
                    Modifier.fillMaxWidth().inkSurface(p.paperA, p, s.cardStyle, seed = h.title.length * 3, radius = 20.dp, scratches = false)
                        .padding(start = 12.dp, top = 10.dp, end = 17.dp, bottom = 15.dp)
                        .pressable(s.anim, null) { h.go() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (h.app != null) AppIcon(h.app, 44.dp, s.iconStyle)
                    else if (h.art != null) ArtIcon(h.art, 44.dp, s.iconStyle)
                    else Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.SEARCH, 26.dp, p.onPaper) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Label(h.title, p.onPaper, 16, FontWeight.Black)
                        Label(h.sub, p.onPaperDim, 12, FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun buildHits(ctx: android.content.Context, raw: String, apps: List<AppEntry>, done: () -> Unit): List<Hit> {
    val q = raw.trim()
    val out = ArrayList<Hit>()
    if (q.isEmpty()) {
        for ((name, desc, act) in systemCommands.take(6)) out.add(Hit(name.replaceFirstChar { it.uppercase() }, desc, null, null) { act(ctx); done() })
        return out
    }
    val lower = q.lowercase()
    val stripped = lower.removePrefix("open ").removePrefix("launch ").removePrefix("go to ").trim()

    // "search web foo" / "search foo" / "google foo"
    val webTerm = listOf("search web ", "search ", "google ", "web ").firstOrNull { lower.startsWith(it) }?.let { q.substring(it.length).trim() }
    if (!webTerm.isNullOrEmpty()) out.add(Hit("Search the web for “$webTerm”", "Opens your browser", null, null) { Actions.webSearch(ctx, webTerm); done() })

    // Apps: prefix matches first, then substring matches.
    val starts = apps.filter { it.label.lowercase().startsWith(stripped) }
    val contains = apps.filter { !it.label.lowercase().startsWith(stripped) && it.label.lowercase().contains(stripped) }
    for (a in (starts + contains).take(8)) out.add(Hit(a.label, "Open app · ${a.category.label.lowercase()}", null, a) { Actions.launch(ctx, a); done() })

    // System commands
    for ((name, desc, act) in systemCommands) {
        if (name.contains(stripped) || stripped.contains(name)) {
            out.add(Hit(name.replaceFirstChar { it.uppercase() }, desc, null, null) { act(ctx); done() })
        }
    }

    out.add(Hit("Search the web for “$q”", "Opens your browser", null, null) { Actions.webSearch(ctx, q); done() })
    return out.distinctBy { it.title + it.sub }
}
