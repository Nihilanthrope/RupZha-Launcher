package com.rupzha.launcher

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
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

/**
 * Launches two apps one after the other. Android doesn't let a normal launcher force split-screen,
 * so the first app opens immediately and the second follows a moment later; the user can then
 * use the system's own split-screen / recents gesture if they want them side by side.
 */
private fun launchPair(ctx: Context, a: AppEntry, b: AppEntry) {
    Actions.launch(ctx, a)
    Handler(Looper.getMainLooper()).postDelayed({ Actions.launch(ctx, b) }, 650)
}

@Composable
fun WorkspacesScreen(vm: LauncherViewModel, p: Palette, s: Settings, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val allApps by vm.apps.collectAsState()
    val apps = remember(allApps, s.hidden, s.catOverrides) { visibleApps(allApps, s) }
    val byKey = remember(allApps) { allApps.associateBy { it.key } }

    var creating by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf<AppEntry?>(null) }
    var second by remember { mutableStateOf<AppEntry?>(null) }

    // Starter suggestions built from whatever the phone has installed.
    val suggestions = remember(allApps) {
        listOf(
            Triple("Music + Notes", arrayOf("spotify"), arrayOf("notes", "keep")),
            Triple("Browser + Discord", arrayOf("chrome"), arrayOf("discord")),
            Triple("Video + Chat", arrayOf("youtube"), arrayOf("telegram")),
        ).mapNotNull { (name, x, y) ->
            val a = findApp(allApps, *x); val b = findApp(allApps, *y)
            if (a != null && b != null && a.key != b.key) Workspace(name, a.key, b.key) else null
        }.filter { sug -> s.workspaces.none { it.a == sug.a && it.b == sug.b } }
    }

    Column(Modifier.fillMaxSize().padding(top = 44.dp, start = 20.dp, end = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InkButton(Glyph.BACK, p, s, 42.dp, onBack)
            Spacer(Modifier.width(14.dp))
            Column {
                InkText("2UP WORKSPACES", 24.sp, p.accent, stroke = 4.dp, shadow = p.shadow)
                Label("Two apps, one tap", p.textDim, 13)
            }
        }
        Spacer(Modifier.height(18.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 60.dp, end = 5.dp)) {
            items(s.workspaces, key = { it.name + it.a + it.b }) { w ->
                val a = byKey[w.a]; val b = byKey[w.b]
                WorkspaceCard(w.name, a, b, p, s, enabled = a != null && b != null,
                    onLaunch = { if (a != null && b != null) launchPair(ctx, a, b) },
                    action = "Delete", onAction = { vm.deleteWorkspace(w) })
            }
            if (suggestions.isNotEmpty()) {
                item { Label("SUGGESTED FOR YOUR PHONE", p.textDim, 11, FontWeight.Black, spacing = 2f, modifier = Modifier.padding(top = 8.dp)) }
                items(suggestions, key = { "s" + it.name }) { w ->
                    WorkspaceCard(w.name, byKey[w.a], byKey[w.b], p, s, true,
                        onLaunch = { launchPair(ctx, byKey.getValue(w.a), byKey.getValue(w.b)) },
                        action = "Save", onAction = { vm.saveWorkspace(w) })
                }
            }
            item {
                Box(
                    Modifier.fillMaxWidth().height(64.dp).inkSurface(p.accent, p, s.cardStyle, seed = 3, radius = 22.dp, scratches = false)
                        .padding(bottom = 5.dp, end = 5.dp).pressable(s.anim, null) { creating = true; first = null; second = null },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlyphIcon(Glyph.PLUS, 22.dp, p.onAccent)
                        Spacer(Modifier.width(8.dp))
                        Label("NEW WORKSPACE", p.onAccent, 14, FontWeight.Black, spacing = 1f)
                    }
                }
            }
        }
    }

    if (creating) {
        Box(Modifier.fillMaxSize().paperBackground(p).padding(top = 44.dp, start = 12.dp, end = 12.dp)) {
            Column {
                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    InkButton(Glyph.CLOSE, p, s, 42.dp) { creating = false }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Label(if (first == null) "Pick the first app" else "Now pick the second", p.text, 18, FontWeight.Black)
                        Label(first?.label ?: "", p.textDim, 13)
                    }
                }
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4), contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.Center,
                ) {
                    gridItems(apps, key = { it.key }) { a ->
                        AppTile(a, 60.dp, s.iconStyle, s.anim, p.text, onClick = {
                            val f = first
                            if (f == null) first = a
                            else if (a.key != f.key) {
                                vm.saveWorkspace(Workspace("${f.label} + ${a.label}", f.key, a.key))
                                creating = false
                            }
                        }, onLongClick = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceCard(
    name: String, a: AppEntry?, b: AppEntry?, p: Palette, s: Settings, enabled: Boolean,
    onLaunch: () -> Unit, action: String, onAction: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().inkSurface(p.paperB, p, s.cardStyle, seed = name.length * 5)
            .padding(start = 16.dp, top = 14.dp, end = 21.dp, bottom = 19.dp),
    ) {
        Label(name, p.onPaper, 17, FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp)) { if (a != null) AppIcon(a, 58.dp, s.iconStyle) }
            Label("+", p.onPaper, 26, FontWeight.Black, Modifier.padding(horizontal = 10.dp))
            Box(Modifier.size(58.dp)) { if (b != null) AppIcon(b, 58.dp, s.iconStyle) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(if (enabled) "LAUNCH" else "MISSING APP", p.accent, p.onAccent, p, s, enabled, onLaunch)
                PillButton(action.uppercase(), p.paperA, p.onPaper, p, s, true, onAction)
            }
        }
    }
}

@Composable
fun PillButton(text: String, fill: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color, p: Palette, s: Settings, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.inkSurface(fill, p, CardStyle.FLAT, seed = text.length, radius = 16.dp, outline = 2.2.dp, scratches = false)
            .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 7.dp)
            .then(if (enabled) Modifier.pressable(s.anim, null, onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, if (enabled) textColor else textColor.copy(alpha = 0.5f), 11, FontWeight.Black, spacing = 1f)
    }
}
