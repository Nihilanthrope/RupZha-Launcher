package com.rupzha.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MediaState(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean,
    val pkg: String?,
)

class MediaListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) { previews = snapshot() }
    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) { previews = snapshot() }

    private fun snapshot(): List<String> = runCatching {
        activeNotifications.orEmpty().mapNotNull { sbn ->
            val e = sbn.notification.extras
            val title = e.getCharSequence("android.title")?.toString().orEmpty()
            val text = e.getCharSequence("android.text")?.toString().orEmpty()
            val line = listOf(title, text).filter { it.isNotBlank() }.joinToString(" · ")
            line.takeIf { it.isNotBlank() }
        }.take(4)
    }.getOrDefault(emptyList())

    companion object {
        @Volatile private var previews: List<String> = emptyList()
        fun activePreviews(): List<String> = previews
    }
}

class MediaBridge(private val ctx: Context) {
    private val _state = MutableStateFlow<MediaState?>(null)
    val state: StateFlow<MediaState?> = _state
    private val _access = MutableStateFlow(hasAccess())
    val access: StateFlow<Boolean> = _access
    private var started = false

    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val comp = ComponentName(ctx, MediaListener::class.java)
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = pick(runCatching { msm.getActiveSessions(comp) }.getOrNull())
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }

    fun hasAccess(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun start() {
        _access.value = hasAccess()
        if (started) return
        if (!_access.value) { _state.value = null; return }
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, comp)
            started = true
            pick(msm.getActiveSessions(comp))
        } catch (_: SecurityException) {
            started = false
            _state.value = null
        }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { msm.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun pick(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) { _state.value = null; return }
        val m = c.metadata
        val title = m?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (title.isEmpty()) { _state.value = null; return }
        val artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
        val rawArt = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: m?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val art = rawArt?.let { bitmap ->
            val max = 384
            if (bitmap.width <= max && bitmap.height <= max) bitmap
            else {
                val scale = minOf(max.toFloat() / bitmap.width, max.toFloat() / bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
            }
        }
        _state.value = MediaState(title, artist, art, c.playbackState?.state == PlaybackState.STATE_PLAYING, c.packageName)
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
}
