package com.rupzha.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.StandardCopyOption
import java.nio.file.Files

/**
 * Loads the launchable app list. The list is cached on disk so the very first frame
 * after process start already has apps; a background refresh then diffs against it.
 */
class AppRepository(private val ctx: Context) {
    private val cacheFile = File(ctx.filesDir, "apps.v1.cache")
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    fun loadCached() {
        runCatching {
            if (!cacheFile.exists()) return
            val list = cacheFile.readLines().mapNotNull { line ->
                val p = line.split('\t')
                if (p.size != 4) return@mapNotNull null
                val cat = Category.entries.getOrNull(p[3].toIntOrNull() ?: -1) ?: return@mapNotNull null
                AppEntry(p[0], p[1], p[2], cat)
            }
            if (list.isNotEmpty()) _apps.value = list
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val list = resolved.asSequence()
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val ai = ri.activityInfo
                AppEntry(
                    pkg = ai.packageName,
                    activity = ai.name,
                    label = ri.loadLabel(pm).toString().trim().ifEmpty { ai.packageName },
                    category = Categorizer.categorize(ai.packageName, ri.loadLabel(pm).toString(), ai.applicationInfo),
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
            .toList()

        if (list != _apps.value) {
            _apps.value = list
            runCatching {
                val tmp = File(cacheFile.path + ".tmp")
                tmp.writeText(list.joinToString("\n") {
                    "${it.pkg}\t${it.activity}\t${it.label.replace('\t', ' ').replace('\n', ' ')}\t${it.category.ordinal}"
                })
                runCatching {
                    Files.move(tmp.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                }.getOrElse {
                    runCatching { Files.move(tmp.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING) }
                }
            }
        }
    }
}

object Categorizer {
    private val social = listOf(
        "whatsapp", "telegram", "discord", "instagram", "facebook", "twitter", "com.x.", "snapchat",
        "reddit", "linkedin", "signal", "messenger", "threads", "pinterest", "tumblr", "viber",
        "mms", "messaging", "dialer", "contacts", "telecom",
    )
    private val media = listOf(
        "spotify", "youtube", "netflix", "music", "gallery", "photos", "camera", "video", "player",
        "vlc", "soundcloud", "podcast", "tiktok", "prime", "hotstar", "jio", "radio", "audio", "shazam",
    )
    private val work = listOf(
        "gmail", "drive", "docs", "sheets", "slides", "calendar", "office", "teams", "slack", "zoom",
        "notion", "keep", "notes", "outlook", "meet", "chatgpt", "openai", "claude", "anthropic", "github",
        "email", "mail", "pdf", "word", "excel", "evernote", "todo",
    )
    private val tools = listOf(
        "termux", "calculator", "clock", "files", "documentsui", "filemanager", "maps", "chrome", "browser",
        "translate", "weather", "recorder", "compass", "scanner", "vpn", "play.store", "vending", "authenticator",
    )
    private val systemHints = listOf("settings", "android.system", "packageinstaller", "permissioncontroller", "oplus", "coloros")
    private val games = listOf(
        "game", "pubg", "tencent", "supercell", "mojang", "minecraft", "roblox", "bgmi", "freefire", "garena",
        "codm", "activision", "epicgames", "miHoYo".lowercase(), "genshin", "ludo", "chess", "candy", "clash",
    )

    fun categorize(pkg: String, label: String, info: ApplicationInfo?): Category {
        val p = pkg.lowercase()
        val l = label.lowercase()
        fun hit(words: List<String>) = words.any { p.contains(it) || l.contains(it) }

        if (hit(games)) return Category.GAMES
        if (hit(social)) return Category.SOCIAL
        if (hit(media)) return Category.MEDIA
        if (hit(work)) return Category.WORK
        if (hit(tools)) return Category.TOOLS
        if (info != null) {
            when (info.category) {
                ApplicationInfo.CATEGORY_UNDEFINED -> Unit
                ApplicationInfo.CATEGORY_GAME -> return Category.GAMES
                ApplicationInfo.CATEGORY_SOCIAL -> return Category.SOCIAL
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE, ApplicationInfo.CATEGORY_NEWS -> return Category.MEDIA
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return Category.WORK
                ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_ACCESSIBILITY -> return Category.TOOLS
            }
            if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0 || hit(systemHints)) return Category.SYSTEM
        }
        return Category.TOOLS
    }
}

/** Finds the best app for a set of needles (package or label fragments). Exact package matches win. */
fun findApp(apps: List<AppEntry>, vararg needles: String): AppEntry? {
    for (n in needles) apps.firstOrNull { it.pkg == n }?.let { return it }
    for (n in needles) {
        val k = n.lowercase()
        apps.firstOrNull { it.pkg.lowercase().contains(k) || it.label.lowercase() == k }?.let { return it }
    }
    for (n in needles) {
        val k = n.lowercase()
        apps.firstOrNull { it.label.lowercase().contains(k) }?.let { return it }
    }
    return null
}

@Suppress("unused")
private fun PackageManager.hasPackage(p: String) = runCatching { getPackageInfo(p, 0) }.isSuccess
