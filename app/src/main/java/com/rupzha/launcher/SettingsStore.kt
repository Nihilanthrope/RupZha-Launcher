package com.rupzha.launcher

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tiny SharedPreferences-backed store. One synchronous read at startup, async `apply()` writes. */
class SettingsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("rupzha", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state

    fun update(block: (Settings) -> Settings) {
        val next = block(_state.value)
        if (next == _state.value) return
        _state.value = next
        save(next)
    }

    private fun <E : Enum<E>> enumOf(key: String, values: Array<E>, def: E): E =
        values.getOrNull(sp.getInt(key, def.ordinal)) ?: def

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            theme = enumOf("theme", ThemeId.entries.toTypedArray(), d.theme),
            accent = sp.getInt("accent", d.accent),
            iconScale = sp.getFloat("iconScale", d.iconScale),
            gridCols = sp.getInt("gridCols", d.gridCols).coerceIn(3, 5),
            cardStyle = enumOf("cardStyle", CardStyle.entries.toTypedArray(), d.cardStyle),
            iconStyle = enumOf("iconStyle", IconStyle.entries.toTypedArray(), d.iconStyle),
            anim = enumOf("anim", AnimLevel.entries.toTypedArray(), d.anim),
            layout = enumOf("layout", HomeLayout.entries.toTypedArray(), d.layout),
            systemWallpaper = sp.getBoolean("sysWall", d.systemWallpaper),
            heroUri = sp.getString("heroUri", null),
            favorites = sp.getString("fav", "").orEmpty().split('\n').filter { it.isNotBlank() },
            hidden = sp.getString("hidden", "").orEmpty().split('\n').filter { it.isNotBlank() }.toSet(),
            catOverrides = sp.getString("cats", "").orEmpty().split('\n').mapNotNull {
                val p = it.split('=')
                val c = p.getOrNull(1)?.toIntOrNull()?.let { i -> Category.entries.getOrNull(i) }
                if (p.size == 2 && c != null) p[0] to c else null
            }.toMap(),
            workspaces = sp.getString("ws", "").orEmpty().split('\n').mapNotNull {
                val p = it.split('\t')
                if (p.size == 3) Workspace(p[0], p[1], p[2]) else null
            },
            seeded = sp.getBoolean("seeded", false),
        )
    }

    private fun save(s: Settings) {
        sp.edit()
            .putInt("theme", s.theme.ordinal)
            .putInt("accent", s.accent)
            .putFloat("iconScale", s.iconScale)
            .putInt("gridCols", s.gridCols)
            .putInt("cardStyle", s.cardStyle.ordinal)
            .putInt("iconStyle", s.iconStyle.ordinal)
            .putInt("anim", s.anim.ordinal)
            .putInt("layout", s.layout.ordinal)
            .putBoolean("sysWall", s.systemWallpaper)
            .putString("heroUri", s.heroUri)
            .putString("fav", s.favorites.joinToString("\n"))
            .putString("hidden", s.hidden.joinToString("\n"))
            .putString("cats", s.catOverrides.entries.joinToString("\n") { "${it.key}=${it.value.ordinal}" })
            .putString("ws", s.workspaces.joinToString("\n") { "${it.name.clean()}\t${it.a}\t${it.b}" })
            .putBoolean("seeded", s.seeded)
            .apply()
    }

    private fun String.clean() = replace('\t', ' ').replace('\n', ' ')
}
