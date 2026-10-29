package com.rupzha.launcher

import androidx.compose.runtime.Immutable

enum class Category(val label: String) {
    ALL("ALL"), SOCIAL("SOCIAL"), MEDIA("MEDIA"), WORK("WORK"),
    TOOLS("TOOLS"), GAMES("GAMES"), SYSTEM("SYSTEM")
}

@Immutable
data class AppEntry(
    val pkg: String,
    val activity: String,
    val label: String,
    val category: Category,
) {
    /** Stable identity used by favorites, hidden apps and workspaces. */
    val key: String get() = "$pkg/$activity"
}

@Immutable
data class Workspace(val name: String, val a: String, val b: String)

enum class ThemeId(val label: String) {
    NAVY("RUPZHA Navy"), CREAM("RUPZHA Cream"), LAVENDER("RUPZHA Lavender"),
    PEACH("RUPZHA Peach"), DYNAMIC("Dynamic"), CUSTOM("Custom")
}

enum class CardStyle(val label: String) { INK("Ink"), SOFT("Soft"), FLAT("Flat") }
enum class IconStyle(val label: String) { PAINTED("Painted"), SKETCH("Sketch"), FLAT("Flat") }
enum class AnimLevel(val label: String) { OFF("Off"), SUBTLE("Subtle"), LIVELY("Lively") }
enum class HomeLayout(val label: String) { DASHBOARD("Dashboard"), MINIMAL("Minimal") }
@Immutable
data class Settings(
    val theme: ThemeId = ThemeId.NAVY,
    val accent: Int = 0xFFFFB59A.toInt(),
    val iconScale: Float = 1f,
    val gridCols: Int = 4,
    val cardStyle: CardStyle = CardStyle.INK,
    val iconStyle: IconStyle = IconStyle.PAINTED,
    val anim: AnimLevel = AnimLevel.SUBTLE,
    val layout: HomeLayout = HomeLayout.DASHBOARD,
    val systemWallpaper: Boolean = false,
    val heroUri: String? = null,
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val catOverrides: Map<String, Category> = emptyMap(),
    val workspaces: List<Workspace> = emptyList(),
    val seeded: Boolean = false,
)
