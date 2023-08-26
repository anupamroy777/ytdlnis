package com.deniscerri.ytdlnis.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavDeepLinkBuilder
import com.deniscerri.ytdlnis.R
import com.deniscerri.ytdlnis.receiver.CancelDownloadNotificationReceiver
import com.deniscerri.ytdlnis.receiver.CancelWorkReceiver
import com.deniscerri.ytdlnis.receiver.PauseDownloadNotificationReceiver
import com.deniscerri.ytdlnis.receiver.ResumeActivity
import com.deniscerri.ytdlnis.receiver.ShareFileActivity
import java.io.File


class NotificationUtil(var context: Context) {
    private val downloadNotificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, DOWNLOAD_SERVICE_CHANNEL_ID)
    private val commandDownloadNotificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, COMMAND_DOWNLOAD_SERVICE_CHANNEL_ID)
    private val finishedDownloadNotificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, DOWNLOAD_FINISHED_CHANNEL_ID)
    private val notificationManager: NotificationManager = context.getSystemService(NotificationManager::class.java)

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                NotificationManager::class.java
            )

            //gui downloads
            var name: CharSequence = context.getString(R.string.download_notification_channel_name)
            var description = context.getString(R.string.download_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            var channel = NotificationChannel(DOWNLOAD_SERVICE_CHANNEL_ID, name, importance)
            channel.description = description
            notificationManager.createNotificationChannel(channel)

            //command downloads
            name = context.getString(R.string.command_download_notification_channel_name)
            description =
                context.getString(R.string.command_download_notification_channel_description)
            channel = NotificationChannel(COMMAND_DOWNLOAD_SERVICE_CHANNEL_ID, name, importance)
            channel.description = description
            notificationManager.createNotificationChannel(channel)

            //finished or errored downloads
            name = context.getString(R.string.finished_download_notification_channel_name)
            description =
                context.getString(R.string.finished_download_notification_channel_description)
            channel = NotificationChannel(DOWNLOAD_FINISHED_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH)
            channel.description = description
            notificationManager.createNotificationChannel(channel)

            //misc
            name = context.getString(R.string.misc)
            description = ""
            channel = NotificationChannel(DOWNLOAD_MISC_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH)
            channel.description = description
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getBuilder(channel: String) : NotificationCompat.Builder {
        when(channel) {
            DOWNLOAD_SERVICE_CHANNEL_ID -> { return downloadNotificationBuilder}
            COMMAND_DOWNLOAD_SERVICE_CHANNEL_ID -> { return commandDownloadNotificationBuilder }
            DOWNLOAD_FINISHED_CHANNEL_ID -> { return finishedDownloadNotificationBuilder }
        }
        return downloadNotificationBuilder
    }

    fun createDefaultWorkerNotification() : Notification {
        val notificationBuilder = getBuilder(DOWNLOAD_SERVICE_CHANNEL_ID)

        return notificationBuilder
            .setContentTitle(context.getString(R.string.downloading))
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground_large)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_launcher_foreground_large
                )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .clearActions()
            .build()
    }

    fun createDownloadServiceNotification(
        pendingIntent: PendingIntent?,
        title: String?,
        itemID: Int,
    ): Notification {
        val notificationBuilder = getBuilder(DOWNLOAD_SERVICE_CHANNEL_ID)


        return notificationBuilder
            .setContentTitle(title)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    android.R.drawable.stat_sys_download
                )
            )
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(PROGRESS_MAX, PROGRESS_CURR, false)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .clearActions()
            .build()
    }

    fun createResumeDownload(itemID: Int, title: String?){
        val notificationBuilder = getBuilder(DOWNLOAD_SERVICE_CHANNEL_ID)

        notificationBuilder
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground_large)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_launcher_foreground_large
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .clearActions()

        val intent = Intent(context, ResumeActivity::class.java)
        intent.putExtra("itemID", itemID)
        val resumeNotificationPendingIntent = PendingIntent.getActivity(
            context,
            itemID,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.addAction(0, context.getString(R.string.resume), resumeNotificationPendingIntent)
        notificationManager.notify(DOWNLOAD_RESUME_NOTIFICATION_ID + itemID, notificationBuilder.build())
    }

    fun createUpdatingItemNotification(channel: String){
        val notificationBuilder = getBuilder(channel)

        notificationBuilder
            .setContentTitle(context.getString(R.string.updating_download_data))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    android.R.drawable.stat_sys_download
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .clearActions()
        notificationManager.notify(DOWNLOAD_UPDATING_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun createDownloadFinished(title: String?,
        filepath: List<String>?,
        channel: String
    ) {
        val notificationBuilder = getBuilder(channel)

        notificationBuilder
            .setContentTitle("${context.getString(R.string.downloaded)} $title")
            .setSmallIcon(R.drawable.ic_launcher_foreground_large)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_launcher_foreground_large
                )
            )
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .clearActions()
        if (filepath != null){
            try{
                val file = File(filepath.first())
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.deniscerri.ytdl.fileprovider",
                    file
                )

                //open intent
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(uri, "*/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val openNotificationPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(intent)
                    getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }

                //share intent
                val shareIntent = Intent(context, ShareFileActivity::class.java)
                shareIntent.putExtra("path", filepath.toTypedArray())
                shareIntent.putExtra("notificationID", DOWNLOAD_FINISHED_NOTIFICATION_ID)
                val shareNotificationPendingIntent: PendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    shareIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )

                notificationBuilder.addAction(0, context.getString(R.string.Open_File), openNotificationPendingIntent)
                notificationBuilder.addAction(0, context.getString(R.string.share), shareNotificationPendingIntent)
            }catch (_: Exception){}
        }
        notificationManager.notify(DOWNLOAD_FINISHED_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun createDownloadErrored(title: String?,
                               error: String?,
                               logID: Long?,
                               channel: String
    ) {
        val notificationBuilder = getBuilder(channel)

        val bundle = Bundle()
        if (logID != null){
            bundle.putLong("logID", logID)
        }

        val errorPendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.downloadLogFragment)
            .setArguments(bundle)
            .createPendingIntent()

        val tabBundle = Bundle()
        tabBundle.putString("tab", "error")

        val errorTabPendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.downloadQueueMainFragment)
            .setArguments(tabBundle)
            .createPendingIntent()

        notificationBuilder
            .setContentTitle("${context.getString(R.string.failed_download)}: $title")
            .setContentText(error)
            .setSmallIcon(R.drawable.ic_launcher_foreground_large)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_launcher_foreground_large
                )
            )
            .setContentIntent(errorTabPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .clearActions()
        if (logID != null){
            notificationBuilder.setContentIntent(errorPendingIntent)
            notificationBuilder.addAction(0, context.getString(R.string.logs), errorPendingIntent)
        }
        notificationManager.notify(DOWNLOAD_FINISHED_NOTIFICATION_ID, notificationBuilder.build())
    }


    fun notify(id: Int, notification: Notification){
        notificationManager.notify(id, notification)
    }

    fun updateDownloadNotification(
        id: Int,
        desc: String,
        progress: Int,
        queue: Int,
        title: String?,
        channel : String
    ) {

        val notificationBuilder = getBuilder(channel)
        var contentText = ""
        if (queue > 1) contentText += """${queue - 1} ${context.getString(R.string.items_left)}""" + "\n"
        contentText += desc.replace("\\[.*?\\] ".toRegex(), "")

        val pauseIntent = Intent(context, PauseDownloadNotificationReceiver::class.java)
        pauseIntent.putExtra("itemID", id)
        pauseIntent.putExtra("title", title)
        val pauseNotificationPendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, CancelDownloadNotificationReceiver::class.java)
        cancelIntent.putExtra("itemID", id)
        val cancelNotificationPendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        try {
            notificationBuilder.setProgress(100, progress, false)
                .setContentTitle(title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .clearActions()
                .addAction(0, context.getString(R.string.pause), pauseNotificationPendingIntent)
                .addAction(0, context.getString(R.string.cancel), cancelNotificationPendingIntent)
            notificationManager.notify(id, notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelDownloadNotification(id: Int) {
        notificationManager.cancel(id)
    }

    fun createMoveCacheFilesNotification(pendingIntent: PendingIntent?, downloadMiscChannelId: String): Notification {
        val notificationBuilder = getBuilder(downloadMiscChannelId)

        return notificationBuilder
            .setContentTitle(context.getString(R.string.move_temporary_files))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    android.R.drawable.stat_sys_download
                )
            )
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(PROGRESS_MAX, PROGRESS_CURR, false)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .clearActions()
            .build()
    }

    fun updateCacheMovingNotification(id: Int, progress: Int, totalFiles: Int) {
        val notificationBuilder = getBuilder(DOWNLOAD_MISC_CHANNEL_ID)
        val contentText = "${progress}/${totalFiles}"
        try {
            notificationBuilder.setProgress(100, progress, false)
                .setContentTitle(context.getString(R.string.move_temporary_files))
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            notificationManager.notify(id, notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createYTDLUpdateNotification() : Notification{
        val notificationBuilder = getBuilder(DOWNLOAD_MISC_CHANNEL_ID)
        notificationBuilder.setContentTitle("Updating YT-DLP...")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_EVENT)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    android.R.drawable.stat_sys_download
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .clearActions()
        return notificationBuilder.build()
    }

    fun createFormatsUpdateNotification(workID: Int): Notification {
        val notificationBuilder = getBuilder(DOWNLOAD_MISC_CHANNEL_ID)

        val cancelIntent = Intent(context, CancelWorkReceiver::class.java)
        cancelIntent.putExtra("workID", workID)
        val cancelNotificationPendingIntent = PendingIntent.getBroadcast(
            context,
            workID,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return notificationBuilder
            .setContentTitle(context.getString(R.string.update_formats_background))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    android.R.drawable.stat_sys_download
                )
            )
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(PROGRESS_MAX, PROGRESS_CURR, false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .clearActions()
            .addAction(0, context.getString(R.string.cancel), cancelNotificationPendingIntent)
            .build()
    }

    fun updateFormatUpdateNotification(
        id: Int,
        progress: Int,
        queue: Int,
    ) {

        val notificationBuilder = getBuilder(DOWNLOAD_MISC_CHANNEL_ID)
        val contentText = """${queue - progress} ${context.getString(R.string.items_left)}"""
        try {
            notificationBuilder.setProgress(queue, progress, false)
                .setContentTitle(context.getString(R.string.update_formats_background))
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            notificationManager.notify(id, notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun showFormatsUpdatedNotification(downloadIds: List<Long>) {
        val notificationBuilder = getBuilder(DOWNLOAD_FINISHED_CHANNEL_ID)

        val bundle = Bundle()
        bundle.putBoolean("showDownloadsWithUpdatedFormats", true)
        bundle.putLongArray("downloadIds", downloadIds.toLongArray())

        val openMultipleDownloads = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.homeFragment)
            .setArguments(bundle)
            .createPendingIntent()

        notificationBuilder
            .setContentTitle(context.getString(R.string.finished_download_notification_channel_name))
            .setSmallIcon(R.drawable.ic_launcher_foreground_large)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_launcher_foreground_large
                )
            )
            .setContentIntent(openMultipleDownloads)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .clearActions()

        notificationManager.notify(FORMAT_UPDATING_FINISHED_NOTIFICATION_ID, notificationBuilder.build())
    }

    companion object {
        const val DOWNLOAD_SERVICE_CHANNEL_ID = "1"
        const val COMMAND_DOWNLOAD_SERVICE_CHANNEL_ID = "2"
        const val DOWNLOAD_FINISHED_CHANNEL_ID = "3"
        const val DOWNLOAD_FINISHED_NOTIFICATION_ID = 3
        const val DOWNLOAD_RESUME_NOTIFICATION_ID = 40000
        const val DOWNLOAD_UPDATING_NOTIFICATION_ID = 5
        const val FORMAT_UPDATING_NOTIFICATION_ID = 6
        const val FORMAT_UPDATING_FINISHED_NOTIFICATION_ID = 7
        const val DOWNLOAD_MISC_CHANNEL_ID = "4"
        const val DOWNLOAD_MISC_NOTIFICATION_ID = 4
        private const val PROGRESS_MAX = 100
        private const val PROGRESS_CURR = 0
    }
}