package com.rupzha.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

@Composable
fun ControlCenter(vm: LauncherViewModel, p: Palette, s: Settings, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val battery by rememberBattery()
    val media by vm.media.state.collectAsState()
    WhileStarted(onStart = { vm.media.start() }, onStop = { vm.media.stop() })

    // Torch state comes from the system so the button stays right even if another app toggles it.
    var torchOn by remember { mutableStateOf(false) }
    val torchOk = remember { Actions.torchSupported(ctx) }
    DisposableEffect(Unit) {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { torchOn = enabled }
        }
        runCatching { cm.registerTorchCallback(cb, Handler(Looper.getMainLooper())) }
        onDispose { runCatching { cm.unregisterTorchCallback(cb) } }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InkButton(Glyph.BACK, p, s, 42.dp, onBack)
            Spacer(Modifier.width(14.dp))
            Column {
                InkText("CONTROL CENTER", 24.sp, p.accent, stroke = 4.dp, shadow = p.shadow)
                Label("Big buttons for quick things", p.textDim, 13)
            }
        }

        BrightnessSlider(p, s)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Toggle("Wi-Fi", Art.WIFI, false, p, s, Modifier.weight(1f)) { Actions.wifi(ctx) }
            Toggle("Bluetooth", Art.BLUETOOTH, false, p, s, Modifier.weight(1f)) { Actions.bluetooth(ctx) }
            Toggle("Torch", Art.TORCH, torchOn, p, s, Modifier.weight(1f)) {
                if (torchOk) Actions.setTorch(ctx, !torchOn)
                else android.widget.Toast.makeText(ctx, "No flashlight on this device", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Toggle("Camera", Art.CAMERA, false, p, s, Modifier.weight(1f)) { Actions.camera(ctx) }
            Toggle("Calculator", Art.CALCULATOR, false, p, s, Modifier.weight(1f)) { Actions.calculator(ctx) }
            Toggle("Screenshot", Art.SHOT, false, p, s, Modifier.weight(1f)) { Actions.screenshot(ctx) }
        }

        // Media
        Row(
            Modifier.fillMaxWidth().inkSurface(p.paperC, p, s.cardStyle, seed = 19)
                .padding(start = 14.dp, top = 12.dp, end = 19.dp, bottom = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtIcon(Art.MUSIC, 52.dp, s.iconStyle)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Label(media?.title ?: "Nothing playing", p.onPaper, 15, FontWeight.Black)
                Label(media?.artist ?: "Media", p.onPaperDim, 12, FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniButton(Glyph.PREV, p, s) { vm.media.previous() }
                MiniButton(if (media?.playing == true) Glyph.PAUSE else Glyph.PLAY, p, s) { vm.media.playPause() }
                MiniButton(Glyph.NEXT, p, s) { vm.media.next() }
            }
        }

        // Battery
        Row(
            Modifier.fillMaxWidth().inkSurface(p.paperA, p, s.cardStyle, seed = 23)
                .padding(start = 14.dp, top = 12.dp, end = 19.dp, bottom = 17.dp).pressable(s.anim, null) { Actions.battery(ctx) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtIcon(Art.BATTERY, 52.dp, s.iconStyle)
            Spacer(Modifier.width(12.dp))
            Column {
                Label("${battery.pct}%  ${if (battery.charging) "· charging" else ""}", p.onPaper, 20, FontWeight.Black)
                Label("Tap for battery usage", p.onPaperDim, 12, FontWeight.Medium)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PillButton("NOTIFICATIONS", p.paperB, p.onPaper, p, s) { Actions.notifications(ctx) }
            PillButton("LOCK SCREEN", p.paperB, p.onPaper, p, s) { Actions.lockScreen(ctx) }
        }
    }
}

@Composable
private fun MiniButton(g: Glyph, p: Palette, s: Settings, onClick: () -> Unit) {
    Box(
        Modifier.width(40.dp).height(40.dp).inkSurface(p.paperA, p, s.cardStyle, seed = 4 + g.ordinal, radius = 20.dp, elevate = 3.dp, outline = 2.2.dp, scratches = false)
            .pressable(s.anim, null, onClick),
        contentAlignment = Alignment.Center,
    ) { GlyphIcon(g, 17.dp, p.onPaper) }
}

@Composable
private fun Toggle(label: String, art: Art, on: Boolean, p: Palette, s: Settings, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.aspectRatio(0.86f).inkSurface(if (on) p.accent else p.paperA, p, s.cardStyle, seed = label.length * 7)
            .padding(start = 6.dp, top = 8.dp, end = 11.dp, bottom = 13.dp).pressable(s.anim, null, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ArtIcon(art, 58.dp, s.iconStyle)
        Spacer(Modifier.height(4.dp))
        Label(label, if (on) p.onAccent else p.onPaper, 12, FontWeight.Black, align = TextAlign.Center)
    }
}

/** Drag-to-set brightness. Needs the one-time "Modify system settings" permission. */
@Composable
private fun BrightnessSlider(p: Palette, s: Settings) {
    val ctx = LocalContext.current
    var v by remember { mutableFloatStateOf(Actions.getBrightness(ctx)) }
    var allowed by remember { mutableStateOf(Actions.canWriteSettings(ctx)) }
    // Re-check when the user returns from the permission screen.
    WhileStarted(onStart = { allowed = Actions.canWriteSettings(ctx); v = Actions.getBrightness(ctx) }, onStop = {})

    Column(
        Modifier.fillMaxWidth().inkSurface(p.paperB, p, s.cardStyle, seed = 29)
            .padding(start = 16.dp, top = 12.dp, end = 21.dp, bottom = 17.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtIcon(Art.SUN, 40.dp, s.iconStyle)
            Spacer(Modifier.width(10.dp))
            Label("Brightness", p.onPaper, 16, FontWeight.Black, Modifier.weight(1f))
            if (!allowed) PillButton("ALLOW", p.accent, p.onAccent, p, s) { Actions.requestWriteSettings(ctx) }
            else Label("${(v * 100).toInt()}%", p.onPaperDim, 14, FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            Modifier.fillMaxWidth().height(30.dp).pointerInput(allowed) {
                if (!allowed) return@pointerInput
                detectTapGestures { o -> v = (o.x / size.width).coerceIn(0.02f, 1f); Actions.setBrightness(ctx, v) }
            }.pointerInput(allowed) {
                if (!allowed) return@pointerInput
                detectHorizontalDragGestures { change, _ ->
                    v = (change.position.x / size.width).coerceIn(0.02f, 1f)
                    Actions.setBrightness(ctx, v)
                }
            },
        ) {
            val h = size.height; val w = size.width
            val track = wobblyRRect(2f, 3f, w - 4f, h - 6f, (h - 6f) / 2, 1f, 8)
            drawPath(track, Color(0x1F0B1030))
            clipPath(track) { drawRect(if (allowed) p.accent else Color(0x330B1030), Offset.Zero, androidx.compose.ui.geometry.Size(w * v, h)) }
            drawPath(track, Ink, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            val kx = (w * v).coerceIn(14.dp.toPx(), w - 14.dp.toPx())
            drawCircle(Cream, 12.dp.toPx(), Offset(kx, h / 2))
            drawCircle(Ink, 12.dp.toPx(), Offset(kx, h / 2), style = Stroke(2.5.dp.toPx()))
        }
    }
}
