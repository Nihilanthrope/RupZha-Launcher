package com.rupzha.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent

/** Optional bridge for global actions such as screenshot and notification shade. */
class RupzhaAccessibility : AccessibilityService() {
    override fun onServiceConnected() { instance = this }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}

    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onInterrupt() {}

    companion object {
        @Volatile var instance: RupzhaAccessibility? = null
    }
}
