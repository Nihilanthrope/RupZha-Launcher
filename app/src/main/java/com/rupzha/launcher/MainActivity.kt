package com.rupzha.launcher

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Overlay { NONE, CONTROL, WORKSPACES, SETTINGS }

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    /** Bumped whenever the Home button is pressed while we're already on top, so the UI can snap back to Home. */
    private val homePresses = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Android 16 / ColorOS can force expensive wallpaper composition behind a
        // transparent HOME surface. Keep the launcher surface opaque and render our
        // own background instead; this avoids an extra live wallpaper composition pass.
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.rgb(6, 10, 34)))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        window.decorView.post { hideSystemBars() }
        window.decorView.postDelayed({ hideSystemBars() }, 300L)
        setContent { RupzhaApp(vm, homePresses.intValue) }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePresses.intValue++
        }
    }

    override fun onStart() {
        super.onStart()
        vm.refreshNow() // catches app changes made while we were in the background
        vm.media.start()
    }

    override fun onStop() {
        vm.media.stop()
        super.onStop()
    }
}

@androidx.compose.runtime.Composable
private fun RupzhaApp(vm: LauncherViewModel, homePress: Int) {
    val s by vm.settings.collectAsState()
    val palette = rupzhaPalette(s)
    val apps by vm.apps.collectAsState()
    val ctx = LocalContext.current

    var tab by remember { mutableStateOf(Tab.HOME) }
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var searchSeed by remember { mutableStateOf("") }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var catApp by remember { mutableStateOf<AppEntry?>(null) }

    // Home button: collapse everything back to the home tab.
    LaunchedEffect(homePress) {
        if (homePress > 0) { tab = Tab.HOME; overlay = Overlay.NONE; menuApp = null; catApp = null }
    }

    val pickHero = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.update { it.copy(heroUri = uri.toString()) }
        }
    }
    val startPick = { pickHero.launch(arrayOf("image/*", "video/*")) }

    BackHandler(enabled = menuApp != null || catApp != null || overlay != Overlay.NONE || tab != Tab.HOME) {
        when {
            catApp != null -> catApp = null
            menuApp != null -> menuApp = null
            overlay != Overlay.NONE -> overlay = Overlay.NONE
            else -> tab = Tab.HOME
        }
    }

    val fade = if (s.anim == AnimLevel.OFF) 0 else 180

    CompositionLocalProvider(LocalPalette provides palette, LocalSettings provides s) {
        Box(
            Modifier.fillMaxSize().then(
                if (s.systemWallpaper) Modifier.background(if (palette.dark) Color(0x88060A22) else Color(0x44FFFFFF))
                else Modifier.paperBackground(palette),
            ),
        ) {
            // Main pages
            Crossfade(
                targetState = if (overlay == Overlay.NONE) tab.name else overlay.name,
                animationSpec = tween(fade),
                label = "page",
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    Overlay.CONTROL.name -> ControlCenter(vm, palette, s) { overlay = Overlay.NONE }
                    Overlay.WORKSPACES.name -> WorkspacesScreen(vm, palette, s) { overlay = Overlay.NONE }
                    Overlay.SETTINGS.name -> SettingsScreen(vm, palette, s, { overlay = Overlay.NONE }, startPick)
                    Tab.APPS.name -> DrawerScreen(vm, palette, s, { menuApp = it }) { q -> searchSeed = q; tab = Tab.SEARCH }
                    Tab.SEARCH.name -> SearchScreen(vm, palette, s, searchSeed) { tab = Tab.HOME; searchSeed = "" }
                    else -> HomeScreen(
                        vm, palette, s,
                        onOpenControl = { overlay = Overlay.CONTROL },
                        onOpenWorkspaces = { overlay = Overlay.WORKSPACES },
                        onOpenSettings = { overlay = Overlay.SETTINGS },
                        onPickHero = startPick,
                        onMenu = { menuApp = it },
                    )
                }
            }

            if (overlay == Overlay.NONE) {
                BottomNav(
                    tab, palette, s,
                    onTab = { if (it != Tab.SEARCH) searchSeed = ""; tab = it },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp),
                )
            }

            // Long-press menu + category picker
            AppMenu(
                app = menuApp,
                isFav = menuApp?.key in s.favorites,
                p = palette, s = s,
                onDismiss = { menuApp = null },
                onOpen = { menuApp?.let { app -> menuApp = null; Actions.launch(ctx, app) } },
                onInfo = { menuApp?.let { app -> menuApp = null; Actions.appInfo(ctx, app) } },
                onUninstall = { menuApp?.let { app -> menuApp = null; Actions.uninstall(ctx, app) } },
                onToggleFav = { menuApp?.let { vm.toggleFavorite(it.key) } },
                onHide = { menuApp?.let { app -> vm.hide(app.key); menuApp = null } },
                onCategory = { catApp = menuApp; menuApp = null },
            )
            CategoryPicker(
                app = catApp, p = palette, s = s,
                onPick = { c -> catApp?.let { vm.setCategory(it.key, c) } },
                onDismiss = { catApp = null },
            )
        }
    }
}
