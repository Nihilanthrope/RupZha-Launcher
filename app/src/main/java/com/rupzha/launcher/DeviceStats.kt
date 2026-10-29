package com.rupzha.launcher

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.util.Locale

data class BatteryInfo(val pct: Int = 0, val charging: Boolean = false)
enum class NetKind { NONE, WIFI, CELL, OTHER }
data class MemInfo(val usedBytes: Long = 0, val totalBytes: Long = 1)

/**
 * Runs [onStart] while the host lifecycle is STARTED and [onStop] otherwise.
 * Every live data source in the launcher goes through this, so nothing runs while the screen is off
 * or another app is in front.
 */
@Composable
fun WhileStarted(onStart: () -> Unit, onStop: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val start = rememberUpdatedState(onStart)
    val stop = rememberUpdatedState(onStop)
    DisposableEffect(owner) {
        var active = false
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_START && !active) { active = true; start.value() }
            else if (e == Lifecycle.Event.ON_STOP && active) { active = false; stop.value() }
        }
        owner.lifecycle.addObserver(obs)
        onDispose {
            owner.lifecycle.removeObserver(obs)
            if (active) stop.value()
        }
    }
}

private fun parseBattery(i: Intent?): BatteryInfo {
    if (i == null) return BatteryInfo()
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo((level * 100 / scale).coerceIn(0, 100), charging)
}

@Composable
fun rememberBattery(): State<BatteryInfo> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(parseBattery(ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))) }
    val rcv = remember {
        object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { state.value = parseBattery(i) }
        }
    }
    WhileStarted(
        onStart = {
            val sticky = ContextCompat.registerReceiver(
                ctx, rcv, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
            )
            state.value = parseBattery(sticky)
        },
        onStop = { runCatching { ctx.unregisterReceiver(rcv) } },
    )
    return state
}

@Composable
fun rememberNetwork(): State<NetKind> {
    val ctx = LocalContext.current
    val cm = remember { ctx.getSystemService(ConnectivityManager::class.java) }
    fun read(): NetKind {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetKind.NONE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetKind.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetKind.CELL
            else -> NetKind.OTHER
        }
    }
    val state = remember { mutableStateOf(read()) }
    val cb = remember {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state.value = read() }
            override fun onLost(network: Network) { state.value = read() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { state.value = read() }
        }
    }
    WhileStarted(
        onStart = { state.value = read(); runCatching { cm.registerDefaultNetworkCallback(cb) } },
        onStop = { runCatching { cm.unregisterNetworkCallback(cb) } },
    )
    return state
}

/** Memory and storage snapshot, refreshed once whenever the launcher comes to the foreground. */
@Composable
fun rememberRamAndStorage(): Pair<State<MemInfo>, State<MemInfo>> {
    val ctx = LocalContext.current
    val ram = remember { mutableStateOf(readRam(ctx)) }
    val disk = remember { mutableStateOf(readStorage()) }
    WhileStarted(
        onStart = { ram.value = readRam(ctx); disk.value = readStorage() },
        onStop = {},
    )
    return ram to disk
}

fun readRam(ctx: Context): MemInfo {
    val mi = ActivityManager.MemoryInfo()
    ctx.getSystemService(ActivityManager::class.java).getMemoryInfo(mi)
    return MemInfo(mi.totalMem - mi.availMem, mi.totalMem)
}

fun readStorage(): MemInfo {
    val st = StatFs(Environment.getDataDirectory().path)
    val total = st.blockCountLong * st.blockSizeLong
    return MemInfo(total - st.availableBlocksLong * st.blockSizeLong, total)
}

/** Wall clock that ticks once per minute, aligned to the minute edge, and only while visible. */
@Composable
fun rememberNow(): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now.longValue = System.currentTimeMillis()
                delay(60_000L - now.longValue % 60_000L + 50L)
            }
        }
    }
    return now
}

fun gb(bytes: Long): String = String.format(Locale.ROOT, "%.1f", bytes / 1_073_741_824.0)
