package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import com.example.service.JarvisAccessibilityService

sealed class JarvisCommandResult {
    data class Success(val speechResponse: String, val actionDetail: String) : JarvisCommandResult()
    data class Error(val speechResponse: String, val reason: String) : JarvisCommandResult()
}

class JarvisCommandEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /**
     * Parses and executes a user command on-device without any API key.
     */
    fun processCommand(rawInput: String): JarvisCommandResult {
        val input = rawInput.trim().lowercase()
        JarvisAccessibilityService.logEvent("Voice Command received: \"$rawInput\"")

        // 1. Swipes & Gestures (e.g. "slowly swip right", "swip right", "swipe left")
        if (input.contains("swip") || input.contains("swipe") || input.contains("slide") || input.contains("scroll")) {
            return handleSwipeCommand(input)
        }

        // 2. Click / Tap / Interaction (e.g. "click the 2 video", "click 2nd video", "tap search")
        if (input.contains("click") || input.contains("tap") || input.contains("press") || input.contains("interact") || input.contains("select")) {
            return handleClickCommand(input)
        }

        // 3. Open Apps (e.g. "open youtube", "launch camera", "start settings")
        if (input.startsWith("open ") || input.startsWith("launch ") || input.startsWith("start ")) {
            val appQuery = input.substringAfter(" ").trim()
            return launchApplication(appQuery)
        }

        // 4. Device & Phone Actions (e.g. "turn off mobile", "lock phone", "go home", "go back")
        if (input.contains("lock") || input.contains("turn off") || input.contains("screen off") || input.contains("sleep")) {
            return handleLockMobile()
        }

        if (input.contains("back") || input == "go back" || input == "previous") {
            val service = JarvisAccessibilityService.instance
            return if (service != null) {
                service.performSystemAction(JarvisAccessibilityService.SystemAction.BACK)
                JarvisCommandResult.Success("Going back, sir.", "Performed Back navigation.")
            } else {
                JarvisCommandResult.Error("Accessibility service is offline, sir. Please enable it in Settings.", "Service not active")
            }
        }

        if (input.contains("home") || input == "minimize" || input == "go home") {
            val service = JarvisAccessibilityService.instance
            return if (service != null) {
                service.performSystemAction(JarvisAccessibilityService.SystemAction.HOME)
                JarvisCommandResult.Success("Returning to home screen, sir.", "Performed Home navigation.")
            } else {
                JarvisCommandResult.Error("Accessibility service is required for home navigation.", "Service not active")
            }
        }

        if (input.contains("recent") || input.contains("switch app") || input.contains("multitask")) {
            val service = JarvisAccessibilityService.instance
            return if (service != null) {
                service.performSystemAction(JarvisAccessibilityService.SystemAction.RECENTS)
                JarvisCommandResult.Success("Opening recent applications, sir.", "Opened Recents.")
            } else {
                JarvisCommandResult.Error("Accessibility service is offline.", "Service not active")
            }
        }

        if (input.contains("notification")) {
            val service = JarvisAccessibilityService.instance
            return if (service != null) {
                service.performSystemAction(JarvisAccessibilityService.SystemAction.NOTIFICATIONS)
                JarvisCommandResult.Success("Expanding notifications panel, sir.", "Opened notifications.")
            } else {
                JarvisCommandResult.Error("Accessibility service is offline.", "Service not active")
            }
        }

        if (input.contains("quick settings") || input.contains("settings panel")) {
            val service = JarvisAccessibilityService.instance
            return if (service != null) {
                service.performSystemAction(JarvisAccessibilityService.SystemAction.QUICK_SETTINGS)
                JarvisCommandResult.Success("Accessing quick settings, sir.", "Opened Quick Settings.")
            } else {
                JarvisCommandResult.Error("Accessibility service is offline.", "Service not active")
            }
        }

        if (input.contains("screenshot") || input.contains("capture screen")) {
            val service = JarvisAccessibilityService.instance
            return if (service != null) {
                service.performSystemAction(JarvisAccessibilityService.SystemAction.TAKE_SCREENSHOT)
                JarvisCommandResult.Success("Capturing screen, sir.", "Captured screenshot.")
            } else {
                JarvisCommandResult.Error("Accessibility service is offline.", "Service not active")
            }
        }

        // 5. Volume controls
        if (input.contains("volume up") || input.contains("increase volume") || input.contains("louder")) {
            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            return JarvisCommandResult.Success("Increasing volume, sir.", "Volume raised.")
        }
        if (input.contains("volume down") || input.contains("decrease volume") || input.contains("lower volume")) {
            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            return JarvisCommandResult.Success("Decreasing volume, sir.", "Volume lowered.")
        }

        // 6. Conversational / Diagnostic Queries
        if (input.contains("who are you") || input.contains("your name")) {
            return JarvisCommandResult.Success(
                "I am JARVIS, your hands-free mobile automation system. I can execute slow swipes, tap elements, launch apps, and manage your device.",
                "Introduced Jarvis capabilities"
            )
        }
        if (input.contains("status") || input.contains("diagnostic") || input.contains("system")) {
            val accStatus = if (JarvisAccessibilityService.instance != null) "Online" else "Offline"
            return JarvisCommandResult.Success(
                "All systems nominal, sir. Accessibility protocol is $accStatus. Background core is active.",
                "System status report delivered"
            )
        }

        if (input.contains("hello") || input.contains("hey jarvis") || input == "jarvis") {
            return JarvisCommandResult.Success(
                "At your service, sir. How may I assist with your device?",
                "Acknowledged user call"
            )
        }

        // Fallback: If utterance contains an app name directly (e.g. "YouTube", "Chrome")
        val possibleApp = launchApplication(input)
        if (possibleApp is JarvisCommandResult.Success) {
            return possibleApp
        }

        return JarvisCommandResult.Error(
            "I heard '$rawInput', but did not recognize that command. Try 'swip right', 'click 2nd video', or 'open YouTube'.",
            "Unrecognized command pattern"
        )
    }

    private fun handleSwipeCommand(input: String): JarvisCommandResult {
        val service = JarvisAccessibilityService.instance
            ?: return JarvisCommandResult.Error("Accessibility Service is not enabled. Please enable Jarvis in Accessibility Settings.", "Accessibility offline")

        // User requested: "if I say swip right he slowly swip right"
        val isSlow = input.contains("slow") || !input.contains("fast") // Default to slow smooth swipe for high precision hands-free

        val direction = when {
            input.contains("right") -> JarvisAccessibilityService.SwipeDirection.RIGHT
            input.contains("left") -> JarvisAccessibilityService.SwipeDirection.LEFT
            input.contains("up") -> JarvisAccessibilityService.SwipeDirection.UP
            input.contains("down") -> JarvisAccessibilityService.SwipeDirection.DOWN
            else -> JarvisAccessibilityService.SwipeDirection.RIGHT
        }

        service.performSwipe(direction, isSlow = isSlow)

        val speedText = if (isSlow) "slowly" else "rapidly"
        val speech = "Swiping ${direction.name.lowercase()} $speedText, sir."
        return JarvisCommandResult.Success(speech, "Dispatched ${if (isSlow) "slow " else ""}${direction.name} swipe.")
    }

    private fun handleClickCommand(input: String): JarvisCommandResult {
        val service = JarvisAccessibilityService.instance
            ?: return JarvisCommandResult.Error("Accessibility Service is required to click screen elements. Please enable it in Settings.", "Accessibility offline")

        // Parse ordinal (e.g. "click the 2 video", "click 2nd video", "click second video", "click 3rd item")
        val ordinal = extractOrdinal(input)

        // Extract target query (e.g. "video", "subscribe", "search", "play")
        var query = input
            .replace("click", "")
            .replace("the", "")
            .replace("tap", "")
            .replace("press", "")
            .replace("interact with", "")
            .replace("interact", "")
            .replace("select", "")
            .trim()

        // Strip ordinal text from the query
        listOf("1st", "2nd", "3rd", "4th", "5th", "first", "second", "third", "fourth", "fifth", "1", "2", "3", "4", "5", "two", "three", "four")
            .forEach { word ->
                query = query.replace("\\b$word\\b".toRegex(), "").trim()
            }

        if (query.isBlank()) {
            query = if (input.contains("video")) "video" else "item"
        }

        val result = service.clickElementByOrdinalOrQuery(query, ordinal)
        return if (result.success) {
            JarvisCommandResult.Success("Interacting with item $ordinal, sir.", result.message)
        } else {
            JarvisCommandResult.Error("I could not locate element $ordinal on the current screen, sir.", result.message)
        }
    }

    private fun extractOrdinal(input: String): Int {
        val words = input.split(" ")
        for (w in words) {
            when (w) {
                "1", "1st", "first", "one" -> return 1
                "2", "2nd", "second", "two" -> return 2
                "3", "3rd", "third", "three" -> return 3
                "4", "4th", "fourth", "four" -> return 4
                "5", "5th", "fifth", "five" -> return 5
                "6", "6th", "sixth", "six" -> return 6
                "7", "7th", "seventh", "seven" -> return 7
                "8", "8th", "eighth", "eight" -> return 8
                "9", "9th", "ninth", "nine" -> return 9
            }
        }

        // Regex check for numbers (e.g. "2" in "2 video")
        val numberRegex = "\\b(\\d+)(?:st|nd|rd|th)?\\b".toRegex()
        val match = numberRegex.find(input)
        if (match != null) {
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..20) return num
        }

        return 1
    }

    private fun handleLockMobile(): JarvisCommandResult {
        val service = JarvisAccessibilityService.instance
            ?: return JarvisCommandResult.Error("Accessibility Service is offline. Cannot lock mobile without it.", "Accessibility offline")

        val success = service.performSystemAction(JarvisAccessibilityService.SystemAction.LOCK_SCREEN)
        return if (success) {
            JarvisCommandResult.Success("Locking device now, sir.", "Locked screen / device.")
        } else {
            JarvisCommandResult.Error("Unable to execute lock screen action on this Android version.", "Lock failed")
        }
    }

    private fun launchApplication(appName: String): JarvisCommandResult {
        val pm = context.packageManager
        val cleanName = appName.trim().lowercase()

        // Known common package mappings for direct launch
        val commonPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "camera" to "com.google.android.GoogleCamera",
            "whatsapp" to "com.whatsapp",
            "settings" to "com.android.settings",
            "calculator" to "com.google.android.calculator",
            "maps" to "com.google.android.apps.maps",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "spotify" to "com.spotify.music",
            "clock" to "com.google.android.deskclock",
            "calendar" to "com.google.android.calendar",
            "messages" to "com.google.android.apps.messaging",
            "phone" to "com.google.android.dialer"
        )

        for ((key, pkg) in commonPackages) {
            if (cleanName.contains(key)) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    JarvisAccessibilityService.logEvent("Launched application: $key ($pkg)")
                    return JarvisCommandResult.Success("Opening $key, sir.", "Launched $key")
                }
            }
        }

        // Search through all installed applications
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase()
                if (label.contains(cleanName) || cleanName.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        JarvisAccessibilityService.logEvent("Launched application: $label")
                        return JarvisCommandResult.Success("Launching $label, sir.", "Launched $label")
                    }
                }
            }
        } catch (e: Exception) {
            JarvisAccessibilityService.logEvent("App launch error: ${e.message}")
        }

        return JarvisCommandResult.Error(
            "I could not locate an installed application matching '$appName', sir.",
            "App not found"
        )
    }
}
