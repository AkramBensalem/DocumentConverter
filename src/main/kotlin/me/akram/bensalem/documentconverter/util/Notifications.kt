package me.akram.bensalem.documentconverter.util

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import me.akram.bensalem.documentconverter.settings.DocumentConverterConfigurable

object Notifications {
    private const val GROUP_ID = "DocumentConverter"

    fun info(project: Project?, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(project)
    }

    fun warn(project: Project?, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, NotificationType.WARNING)
            .notify(project)
    }

//    fun error(project: Project?, title: String, content: String) {
//        NotificationGroupManager.getInstance()
//            .getNotificationGroup(GROUP_ID)
//            .createNotification(title, content, NotificationType.ERROR)
//            .notify(project)
//    }

    fun error(project: Project?, title: String, content: String) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, NotificationType.ERROR)

        // Check if the content suggests an API key issue to add the action
        if (content.contains("API key", ignoreCase = true)) {
            notification.addAction(NotificationAction.createSimple("Open Settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, DocumentConverterConfigurable::class.java)
            })
        }

        notification.notify(project)
    }
}
