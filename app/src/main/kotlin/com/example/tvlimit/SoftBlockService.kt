package com.example.tvlimit

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class SoftBlockService : AccessibilityService() {

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var lastBlockTs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val blocked = AppState.settings.value.blockedList
        if (pkg !in blocked) return

        // Debounce to avoid spamming actions on rapid events
        val now = System.currentTimeMillis()
        if (now - lastBlockTs < 400) return
        lastBlockTs = now

        // 1) Leave the offending app
        performGlobalAction(GLOBAL_ACTION_HOME)

        // 2) Bring our launcher to foreground after a short delay
        mainHandler.postDelayed({
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }, 120L)
    }

    override fun onInterrupt() {}
}
