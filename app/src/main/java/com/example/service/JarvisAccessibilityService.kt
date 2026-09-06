package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedList

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        logEvent("Jarvis Accessibility Automation Protocol initialized & connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track current window or package changes for context awareness
        event?.packageName?.let { pkg ->
            _currentPackage.value = pkg.toString()
        }
    }

    override fun onInterrupt() {
        logEvent("Jarvis Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceActive.value = false
        }
        logEvent("Jarvis Accessibility Service disconnected.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance == this) {
            instance = null
            _isServiceActive.value = false
        }
        return super.onUnbind(intent)
    }

    /**
     * Dispatches a slow swipe gesture.
     * As requested: "if I say swip right he slowly swip right"
     */
    fun performSwipe(
        direction: SwipeDirection,
        isSlow: Boolean = true,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val metrics: DisplayMetrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        val startX: Float
        val startY: Float
        val endX: Float
        val endY: Float

        when (direction) {
            SwipeDirection.RIGHT -> {
                // Swiping right: finger moves from left side to right side
                startX = screenWidth * 0.20f
                startY = screenHeight * 0.50f
                endX = screenWidth * 0.85f
                endY = screenHeight * 0.50f
            }
            SwipeDirection.LEFT -> {
                // Swiping left: finger moves from right side to left side
                startX = screenWidth * 0.85f
                startY = screenHeight * 0.50f
                endX = screenWidth * 0.15f
                endY = screenHeight * 0.50f
            }
            SwipeDirection.UP -> {
                // Swiping up (scrolls down): finger moves upwards
                startX = screenWidth * 0.50f
                startY = screenHeight * 0.75f
                endX = screenWidth * 0.50f
                endY = screenHeight * 0.25f
            }
            SwipeDirection.DOWN -> {
                // Swiping down (scrolls up): finger moves downwards
                startX = screenWidth * 0.50f
                startY = screenHeight * 0.25f
                endX = screenWidth * 0.50f
                endY = screenHeight * 0.75f
            }
        }

        // Slow swipe duration is typically 1200ms - 1500ms; regular swipe is 300ms
        val duration = if (isSlow) 1350L else 350L

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                logEvent("Gesture completed: ${if (isSlow) "Slow " else ""}${direction.name} swipe.")
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                logEvent("Gesture cancelled: ${direction.name} swipe.")
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * Dispatches a simulated tap at specific screen coordinates
     */
    fun performTap(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 80L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                logEvent("Tap dispatched at (${x.toInt()}, ${y.toInt()})")
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                logEvent("Tap cancelled at (${x.toInt()}, ${y.toInt()})")
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * Finds and clicks an item by ordinal position and keyword,
     * e.g., "click the 2 video" or "click 2nd video" -> ordinal=2, query="video"
     */
    fun clickElementByOrdinalOrQuery(query: String, ordinal: Int = 1): ElementClickResult {
        val rootNode = rootInActiveWindow
            ?: return ElementClickResult(false, "No active screen window accessible to interact with.")

        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        val lowerQuery = query.lowercase().trim()

        // Breadth-first search for nodes matching query
        val queue = LinkedList<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.poll() ?: continue
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val className = node.className?.toString()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName?.lowercase() ?: ""

            val isVideoQuery = lowerQuery.contains("video")
            val matchesQuery = when {
                lowerQuery.isEmpty() || lowerQuery == "item" -> node.isClickable
                isVideoQuery -> {
                    // Match video player elements, video thumbnails, items with video duration/views or clickable cards
                    text.contains("video") || desc.contains("video") ||
                            text.contains("views") || desc.contains("views") ||
                            className.contains("videoview") || className.contains("player") ||
                            viewId.contains("video") || viewId.contains("thumbnail") ||
                            (node.isClickable && (text.isNotBlank() || desc.isNotBlank()))
                }
                else -> {
                    text.contains(lowerQuery) || desc.contains(lowerQuery) ||
                            viewId.contains(lowerQuery)
                }
            }

            if (matchesQuery) {
                matchingNodes.add(node)
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        if (matchingNodes.isEmpty()) {
            // Fallback: If searching for e.g. "video 2" and no specific video tag matched, find any clickable list/card elements
            val clickableQueue = LinkedList<AccessibilityNodeInfo>()
            clickableQueue.add(rootNode)
            while (clickableQueue.isNotEmpty()) {
                val node = clickableQueue.poll() ?: continue
                if (node.isClickable && (node.text != null || node.contentDescription != null)) {
                    matchingNodes.add(node)
                }
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { clickableQueue.add(it) }
                }
            }
        }

        if (matchingNodes.isEmpty()) {
            return ElementClickResult(false, "Could not find any interactable elements matching '$query'.")
        }

        // 1-based index: ordinal 1 is index 0, ordinal 2 is index 1
        val targetIndex = (ordinal - 1).coerceIn(0, matchingNodes.size - 1)
        val targetNode = matchingNodes[targetIndex]

        val targetLabel = targetNode.text?.toString()
            ?: targetNode.contentDescription?.toString()
            ?: "element #$ordinal"

        // Try standard Accessibility click
        var clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        if (!clicked) {
            // Search parent chain for clickable container
            var parent = targetNode.parent
            while (parent != null && !clicked) {
                if (parent.isClickable) {
                    clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
        }

        if (!clicked) {
            // Coordinate fallback: click the center of the node's bounds
            val rect = Rect()
            targetNode.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                performTap(rect.centerX().toFloat(), rect.centerY().toFloat())
                val message = "Tapped element #${targetIndex + 1} ($targetLabel) at (${rect.centerX()}, ${rect.centerY()})"
                logEvent(message)
                return ElementClickResult(true, message)
            }
        }

        val message = "Clicked element #${targetIndex + 1}: '$targetLabel'"
        logEvent(message)
        return ElementClickResult(true, message)
    }

    /**
     * Executes standard system navigation and hardware shortcuts
     */
    fun performSystemAction(actionType: SystemAction): Boolean {
        val actionId = when (actionType) {
            SystemAction.BACK -> GLOBAL_ACTION_BACK
            SystemAction.HOME -> GLOBAL_ACTION_HOME
            SystemAction.RECENTS -> GLOBAL_ACTION_RECENTS
            SystemAction.NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
            SystemAction.QUICK_SETTINGS -> GLOBAL_ACTION_QUICK_SETTINGS
            SystemAction.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    GLOBAL_ACTION_LOCK_SCREEN
                } else {
                    GLOBAL_ACTION_HOME
                }
            }
            SystemAction.TAKE_SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    GLOBAL_ACTION_TAKE_SCREENSHOT
                } else {
                    GLOBAL_ACTION_NOTIFICATIONS
                }
            }
            SystemAction.POWER_DIALOG -> GLOBAL_ACTION_POWER_DIALOG
        }

        val success = performGlobalAction(actionId)
        logEvent("System action ${actionType.name}: ${if (success) "Executed" else "Failed"}")
        return success
    }

    enum class SwipeDirection {
        RIGHT, LEFT, UP, DOWN
    }

    enum class SystemAction {
        BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, LOCK_SCREEN, TAKE_SCREENSHOT, POWER_DIALOG
    }

    data class ElementClickResult(
        val success: Boolean,
        val message: String
    )

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _currentPackage = MutableStateFlow("")
        val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

        private val _serviceLogs = MutableStateFlow<List<String>>(emptyList())
        val serviceLogs: StateFlow<List<String>> = _serviceLogs.asStateFlow()

        fun logEvent(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val entry = "[$timestamp] $message"
            _serviceLogs.value = (_serviceLogs.value + entry).takeLast(60)
        }
    }
}
