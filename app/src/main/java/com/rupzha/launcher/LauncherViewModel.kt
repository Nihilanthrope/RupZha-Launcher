package com.rupzha.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = AppRepository(app)
    private val store = SettingsStore(app)
    val media = MediaBridge(app)

    val apps: StateFlow<List<AppEntry>> = repo.apps
    val settings: StateFlow<Settings> = store.state

    private val _hero = MutableStateFlow<ImageBitmap?>(null)
    val hero: StateFlow<ImageBitmap?> = _hero

    private var refreshJob: Job? = null

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { scheduleRefresh() }
    }

    init {
        repo.loadCached()
        viewModelScope.launch {
            repo.refresh()
            seedFavorites()
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(app, pkgReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)

        viewModelScope.launch {
            settings.map { it.heroUri }.distinctUntilChanged().collect { _hero.value = loadHero(it) }
        }
    }

    /** Debounced: installing an app fires several broadcasts in a row. */
    private fun scheduleRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(500)
            repo.refresh()
        }
    }

    fun refreshNow() { viewModelScope.launch { repo.refresh() } }

    private suspend fun loadHero(uri: String?): ImageBitmap? {
        if (uri == null) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val src = ImageDecoder.createSource(app.contentResolver, Uri.parse(uri))
                ImageDecoder.decodeBitmap(src) { dec, info, _ ->
                    val maxW = 1080
                    val maxH = 720
                    val scale = minOf(
                        maxW.toFloat() / info.size.width.toFloat(),
                        maxH.toFloat() / info.size.height.toFloat(),
                        1f,
                    )
                    dec.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                    dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }.asImageBitmap()
            }.getOrNull()
        }
    }

    /** First run: fill favorites with the quick apps from the brief, whichever are installed. */
    private fun seedFavorites() {
        val s = settings.value
        if (s.seeded) return
        val list = apps.value
        if (list.isEmpty()) return
        val wanted = listOf(
            arrayOf("com.oplus.camera", "com.android.camera", "camera"),
            arrayOf("com.google.android.apps.photos", "com.coloros.gallery3d", "gallery", "photos"),
            arrayOf("com.spotify.music", "spotify"),
            arrayOf("com.discord", "discord"),
            arrayOf("com.android.chrome", "chrome"),
            arrayOf("com.google.android.youtube", "youtube"),
            arrayOf("org.telegram.messenger", "telegram"),
            arrayOf("com.google.android.documentsui", "com.coloros.filemanager", "files"),
        )
        val picked = wanted.mapNotNull { findApp(list, *it)?.key }.distinct()
        store.update { it.copy(favorites = picked, seeded = true) }
    }

    // ---- user intents ----
    fun update(block: (Settings) -> Settings) = store.update(block)

    fun toggleFavorite(key: String) = store.update {
        if (key in it.favorites) it.copy(favorites = it.favorites - key) else it.copy(favorites = it.favorites + key)
    }

    fun hide(key: String) = store.update { it.copy(hidden = it.hidden + key, favorites = it.favorites - key) }
    fun unhide(key: String) = store.update { it.copy(hidden = it.hidden - key) }

    fun setCategory(key: String, cat: Category?) = store.update {
        it.copy(catOverrides = if (cat == null) it.catOverrides - key else it.catOverrides + (key to cat))
    }

    fun saveWorkspace(w: Workspace) = store.update { it.copy(workspaces = it.workspaces + w) }
    fun deleteWorkspace(w: Workspace) = store.update { it.copy(workspaces = it.workspaces - w) }

    override fun onCleared() {
        runCatching { app.unregisterReceiver(pkgReceiver) }
        media.stop()
    }
}
