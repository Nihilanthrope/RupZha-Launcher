package com.rupzha.launcher

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

/** Everything the launcher asks the OS to do. Only public, non-root Android APIs are used. */
object Actions {

    private fun Context.safeStart(i: Intent, opts: android.os.Bundle? = null): Boolean = try {
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), opts)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    fun launch(ctx: Context, app: AppEntry) {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.pkg, app.activity)
            .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val opts = ActivityOptions.makeCustomAnimation(ctx, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
        if (!ctx.safeStart(i, opts)) ctx.toast("Couldn't open ${app.label}")
    }

    fun appInfo(ctx: Context, app: AppEntry) {
        ctx.safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.pkg, null)))
    }

    fun uninstall(ctx: Context, app: AppEntry) {
        if (!ctx.safeStart(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.pkg, null)))) appInfo(ctx, app)
    }

    fun openSettings(ctx: Context, action: String = Settings.ACTION_SETTINGS) {
        if (!ctx.safeStart(Intent(action))) ctx.safeStart(Intent(Settings.ACTION_SETTINGS))
    }

    fun wifi(ctx: Context) {
        if (!ctx.safeStart(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))) openSettings(ctx, Settings.ACTION_WIFI_SETTINGS)
    }

    fun bluetooth(ctx: Context) = openSettings(ctx, Settings.ACTION_BLUETOOTH_SETTINGS)
    fun battery(ctx: Context) = openSettings(ctx, Intent.ACTION_POWER_USAGE_SUMMARY)

    fun camera(ctx: Context) {
        if (!ctx.safeStart(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))) ctx.toast("No camera app found")
    }

    fun calculator(ctx: Context) {
        val i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)
        if (!ctx.safeStart(i)) ctx.toast("No calculator app found")
    }

    fun webSearch(ctx: Context, query: String) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
        if (!ctx.safeStart(i)) ctx.toast("No browser found")
    }

    fun pickWallpaper(ctx: Context) {
        val i = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper")
        ctx.safeStart(i)
    }

    fun openNotificationAccess(ctx: Context) = openSettings(ctx, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    // ---- Flashlight (no permission required) ----
    private fun torchId(cm: CameraManager): String? = cm.cameraIdList.firstOrNull {
        cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    fun setTorch(ctx: Context, on: Boolean): Boolean = runCatching {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val id = torchId(cm) ?: return false
        cm.setTorchMode(id, on)
        true
    }.getOrDefault(false)

    fun torchSupported(ctx: Context): Boolean = runCatching {
        torchId(ctx.getSystemService(CameraManager::class.java)) != null
    }.getOrDefault(false)

    // ---- Brightness (needs the user to grant "Modify system settings" once) ----
    fun canWriteSettings(ctx: Context) = Settings.System.canWrite(ctx)

    fun requestWriteSettings(ctx: Context) {
        ctx.safeStart(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
    }

    fun getBrightness(ctx: Context): Float = runCatching {
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
    }.getOrDefault(0.5f)

    fun setBrightness(ctx: Context, v: Float) {
        if (!canWriteSettings(ctx)) return
        runCatching {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (v * 255).toInt().coerceIn(1, 255))
        }
    }

    // ---- Accessibility-backed global actions ----
    private fun global(ctx: Context, action: Int, what: String) {
        val svc = RupzhaAccessibility.instance
        if (svc == null) {
            ctx.toast("Enable RUPZHA in Accessibility to use $what")
            openSettings(ctx, Settings.ACTION_ACCESSIBILITY_SETTINGS)
        } else {
            svc.performGlobalAction(action)
        }
    }

    fun screenshot(ctx: Context) = global(ctx, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "screenshots")
    fun notifications(ctx: Context) = global(ctx, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "the notification shade")
    fun lockScreen(ctx: Context) = global(ctx, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "screen lock")
}
