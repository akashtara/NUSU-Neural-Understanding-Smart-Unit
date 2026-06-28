package com.example.api

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlatformType {
    ANDROID_MOBILE,
    ANDROID_TABLET,
    ANDROID_FOLDABLE,
    ANDROID_TV,
    WINDOWS,
    MACOS,
    LINUX,
    DESKTOP_GENERIC,
    WEARABLE,
    SMART_DISPLAY,
    UNKNOWN
}

enum class FormFactor {
    MOBILE,
    TABLET,
    FOLDABLE,
    DESKTOP,
    TV,
    WEARABLE,
    EMBEDDED
}

data class PlatformConfiguration(
    val type: PlatformType,
    val formFactor: FormFactor,
    val osName: String,
    val osVersion: String,
    val isExternalDisplayConnected: Boolean,
    val totalMemoryBytes: Long,
    val availableProcessors: Int
)

object PlatformManager {
    private const val TAG = "PlatformManager"

    private val _currentPlatform = MutableStateFlow<PlatformConfiguration?>(null)
    val currentPlatform = _currentPlatform.asStateFlow()

    fun initialize(context: Context) {
        try {
            val osName = System.getProperty("os.name") ?: "Android"
            val osVersion = Build.VERSION.RELEASE ?: "Unknown"
            val processors = Runtime.getRuntime().availableProcessors()
            val totalMem = Runtime.getRuntime().totalMemory()

            // Detect external displays
            var extDisplayConnected = false
            try {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val displays = dm?.displays
                if (displays != null && displays.size > 1) {
                    extDisplayConnected = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read display manager properties: ${e.message}")
            }

            // Screen dimension detection for Mobile vs Tablet vs Foldable
            val resources = context.resources
            val config = resources.configuration
            val smallestWidthDp = config.smallestScreenWidthDp

            val formFactor = when {
                smallestWidthDp >= 600 -> FormFactor.TABLET
                smallestWidthDp in 400..599 -> FormFactor.FOLDABLE
                else -> FormFactor.MOBILE
            }

            val platformType = when (formFactor) {
                FormFactor.TABLET -> PlatformType.ANDROID_TABLET
                FormFactor.FOLDABLE -> PlatformType.ANDROID_FOLDABLE
                else -> PlatformType.ANDROID_MOBILE
            }

            val platformConfig = PlatformConfiguration(
                type = platformType,
                formFactor = formFactor,
                osName = "Android ($osName)",
                osVersion = osVersion,
                isExternalDisplayConnected = extDisplayConnected,
                totalMemoryBytes = totalMem,
                availableProcessors = processors
            )

            _currentPlatform.value = platformConfig
            
            SystemErrorState.log(
                level = LogLevel.INFO,
                tag = "PLATFORM",
                message = "Platform manager successfully resolved host configuration.",
                details = "OS: ${platformConfig.osName} | Form: ${platformConfig.formFactor} | Multi-Display: ${platformConfig.isExternalDisplayConnected}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PlatformManager: ${e.message}")
            // Fallback generic
            _currentPlatform.value = PlatformConfiguration(
                type = PlatformType.ANDROID_MOBILE,
                formFactor = FormFactor.MOBILE,
                osName = "Android (Fallback)",
                osVersion = "Unknown",
                isExternalDisplayConnected = false,
                totalMemoryBytes = 0L,
                availableProcessors = 1
            )
        }
    }

    /**
     * Platform agnostic helper to get JVM info. Used for cross-platform portability checks.
     */
    fun isDesktop(): Boolean {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        return os.contains("win") || os.contains("mac") || os.contains("nix") || os.contains("nux")
    }

    fun getPlatformAgnosticDetails(): Map<String, String> {
        return mapOf(
            "os.name" to (System.getProperty("os.name") ?: "N/A"),
            "os.version" to (System.getProperty("os.version") ?: "N/A"),
            "os.arch" to (System.getProperty("os.arch") ?: "N/A"),
            "java.vendor" to (System.getProperty("java.vendor") ?: "N/A"),
            "java.version" to (System.getProperty("java.version") ?: "N/A")
        )
    }
}
