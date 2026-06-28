package com.example.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _incomingNotifications = MutableSharedFlow<NotificationSummary>(extraBufferCapacity = 10)
    val incomingNotifications = _incomingNotifications.asSharedFlow()

    fun emitNotification(summary: NotificationSummary) {
        scope.launch {
            _incomingNotifications.emit(summary)
        }
    }
}

data class NotificationSummary(
    val packageName: String,
    val title: String,
    val text: String,
    val appName: String
)

class NusuNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        val packageName = sbn.packageName
        
        // Filter for specific important apps
        val isImportantApp = packageName.contains("whatsapp") || 
                             packageName.contains("telegram") ||
                             packageName.contains("message") || 
                             packageName.contains("instagram") ||
                             packageName.contains("dialer") ||
                             packageName.contains("com.google.android.calendar")

        if (isImportantApp) {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            val appName = when {
                packageName.contains("whatsapp") -> "WhatsApp"
                packageName.contains("telegram") -> "Telegram"
                packageName.contains("message") -> "Messages"
                packageName.contains("instagram") -> "Instagram"
                else -> "an App"
            }

            if (title.isNotEmpty() && text.isNotEmpty() && !title.contains("Checking for new messages") && !text.contains("Checking for new messages")) {
                Log.d("NusuNotification", "New $appName: $title - $text")
                NotificationEngine.emitNotification(
                    NotificationSummary(packageName, title, text, appName)
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
