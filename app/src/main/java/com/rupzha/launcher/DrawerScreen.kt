package com.rupzha.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Apps as the user sees them: hidden ones removed, category overrides applied. */
fun visibleApps(apps: List<AppEntry>, s: Settings): List<AppEntry> =
    apps.asSequence().filter { it.key !in s.hidden }
        .map { a -> s.catOverrides[a.key]?.let { a.copy(category = it) } ?: a }
        .toList()

@Composable
fun DrawerScreen(vm: LauncherViewModel, p: Palette, s: Settings, onMenu: (AppEntry) -> Unit, onSearchCommand: (String) -> Unit) {
    val ctx = LocalContext.current
    val allApps by vm.apps.collectAsState()
    val apps = remember(allApps, s.hidden, s.catOverrides) { visibleApps(allApps, s) }
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(Category.ALL) }

    val shown by remember(apps, query, cat) {
        derivedStateOf {
            val q = query.trim().lowercase()
            apps.filter { (cat == Category.ALL || it.category == cat) && (q.isEmpty() || it.label.lowercase().contains(q)) }
        }
    }
    val iconSize = (64 * s.iconScale).dp

    Column(Modifier.fillMaxSize().padding(top = 44.dp)) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            InkText("RUPZHA", 30.sp, p.accent, stroke = 4.5.dp, shadow = p.shadow, letterSpacing = 2.sp)
            Label("APPLICATIONS", p.textDim, 12, FontWeight.Black, spacing = 3f, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(14.dp))
            SearchField(query, { query = it }, "Search apps...", p, s, onGo = {
                // Enter launches the top match, otherwise hands off to the command center.
                val top = shown.firstOrNull()
                if (top != null) Actions.launch(ctx, top) else if (query.isNotBlank()) onSearchCommand(query)
            })
            Spacer(Modifier.height(12.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            items(Category.entries, key = { it.name }) { c ->
                val sel = c == cat
                Box(
                    Modifier.height(40.dp)
                        .inkSurface(if (sel) p.accent else p.surface, p, if (sel) s.cardStyle else CardStyle.FLAT, seed = 90 + c.ordinal, radius = 18.dp, elevate = 3.dp, scratches = false)
                        .padding(start = 15.dp, end = 18.dp, top = 8.dp, bottom = 11.dp)
                        .pressable(s.anim, null) { cat = c },
                    contentAlignment = Alignment.Center,
                ) {
                    Label(c.label, if (sel) p.onAccent else p.text, 12, FontWeight.Black, spacing = 1f)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(s.gridCols),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(shown, key = { it.key }, contentType = { "app" }) { a ->
                AppTile(
                    a, iconSize, s.iconStyle, s.anim, p.text,
                    onClick = { Actions.launch(ctx, a) },
                    onLongClick = { onMenu(a) },
                )
            }
            if (shown.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Label(
                        if (query.isBlank()) "Nothing in this category yet." else "No app called “$query”. Press enter to try it as a command.",
                        p.textDim, 14, modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, p: Palette, s: Settings, onGo: () -> Unit, autoFocus: Boolean = false) {
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    if (autoFocus) androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        Modifier.fillMaxWidth().height(54.dp).inkSurface(p.paperA, p, s.cardStyle, seed = 71, radius = 22.dp, scratches = false)
            .padding(start = 16.dp, end = 21.dp, top = 5.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(Glyph.SEARCH, 22.dp, p.onPaper)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Label(hint, p.onPaperDim, 16, FontWeight.Medium)
            BasicTextField(
                value, onChange,
                singleLine = true,
                textStyle = TextStyle(color = p.onPaper, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(p.onPaper),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.pressable(s.anim, null) { onChange("") }) { GlyphIcon(Glyph.CLOSE, 20.dp, p.onPaperDim) }
        }
    }
}
