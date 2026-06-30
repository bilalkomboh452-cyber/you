
package com.waglo.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.waglo.app.engine.LinkParser
import com.waglo.app.model.GroupLink
import com.waglo.app.repository.GroupLinkRepository
import com.waglo.app.utils.Classifier
import com.waglo.app.utils.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Passive read-only Accessibility Service.
 *
 * SAFETY GUARANTEES:
 *  • Only reads text visible on screen.
 *  • Never clicks, scrolls, or interacts with any UI element.
 *  • Never sends messages.
 *  • Never joins groups automatically.
 *  • Never accesses WhatsApp contacts or messages.
 */
class WAAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG                = "WAAccessService"
        private const val WA_PACKAGE         = "com.whatsapp"
        private const val WA_BUSINESS_PKG    = "com.whatsapp.w4b"
        const val ACTION_LINKS_UPDATED       = "com.waglo.app.ACTION_LINKS_UPDATED"
        const val EXTRA_NEW_COUNT            = "new_count"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repo: GroupLinkRepository

    // Rate limiter: avoid processing every single event on busy screens.
    private var lastProcessed = 0L
    private val THROTTLE_MS  = 1500L

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = GroupLinkRepository(applicationContext)
        serviceInfo = serviceInfo.apply {
            packageNames         = arrayOf(WA_PACKAGE, WA_BUSINESS_PKG)
            eventTypes           = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                                   AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType         = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout  = THROTTLE_MS
            flags                = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Log.i(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != WA_PACKAGE && pkg != WA_BUSINESS_PKG) return

        val now = System.currentTimeMillis()
        if (now - lastProcessed < THROTTLE_MS) return
        lastProcessed = now

        val rootNode = rootInActiveWindow ?: return
        val text = StringBuilder()
        collectText(rootNode, text)
        rootNode.recycle()

        val raw = text.toString()
        if (raw.isBlank() || !raw.contains("chat.whatsapp.com")) return

        processText(raw)
    }

    /**
     * Recursively collect visible text from the node tree.
     * Capped at depth 12 and 500 nodes to protect low-end devices.
     */
    private fun collectText(
        node: AccessibilityNodeInfo?,
        sb: StringBuilder,
        depth: Int = 0,
        count: IntArray = intArrayOf(0)
    ) {
        if (node == null || depth > 12 || count[0] > 500) return
        count[0]++
        node.text?.let { sb.append(it).append(' ') }
        node.contentDescription?.let { sb.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb, depth + 1, count)
        }
    }

    private fun processText(text: String) {
        serviceScope.launch {
            try {
                val validLinks = LinkParser.extractValid(text)
                if (validLinks.isEmpty()) return@launch

                val existingLinks = repo.getStats() // ensure DB is warmed up
                val groups = validLinks.map { link ->
                    GroupLink(
                        inviteLink    = link,
                        groupName     = extractGroupName(link),
                        source        = GroupLink.Source.ACCESSIBILITY,
                        category      = Classifier.classify(link),
                        discoveryTime = System.currentTimeMillis()
                    )
                }
                val inserted = repo.insertBatch(groups)
                if (inserted > 0) {
                    PrefsHelper.addTotalFoundSession(applicationContext, inserted)
                    broadcastUpdate(inserted)
                    Log.d(TAG, "Captured $inserted new links")
                }
            } catch (e: Exception) {
                Log.e(TAG, "processText error", e)
            }
        }
    }

    private fun extractGroupName(link: String): String {
        // Future: could derive a name from the screen context.
        return ""
    }

    private fun broadcastUpdate(count: Int) {
        val intent = Intent(ACTION_LINKS_UPDATED).apply {
            putExtra(EXTRA_NEW_COUNT, count)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
