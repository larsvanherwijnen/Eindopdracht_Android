package nl.avans.todo.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import nl.avans.todo.MainActivity
import nl.avans.todo.R
import nl.avans.todo.data.model.Todo
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationService(private val context: Context) {
    private val TAG = "NotificationService"
    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationScope = CoroutineScope(Dispatchers.Default)
    private val activeNotifications = mutableMapOf<Int, Job>()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Todo Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for todo tasks"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleTodoNotification(todo: Todo) {
        if (!hasNotificationPermission()) {
            Log.e(TAG, "Notification permission not granted")
            return
        }

        if (todo.dueDate == null) {
            Log.e(TAG, "Todo has no due date")
            return
        }

        // Cancel any existing notification for this todo
        cancelNotification(todo.id)

        val now = LocalDateTime.now()
        val dueDate = todo.dueDate
        val minutesBefore = todo.notificationMinutesBefore

        // Calculate the delay until the notification should be shown
        val notificationTime = dueDate.minusMinutes(minutesBefore.toLong())
        val delayMillis = Duration.between(now, notificationTime).toMillis()

        // Only schedule if the notification time is in the future
        if (delayMillis > 0) {
            val job = notificationScope.launch {
                try {
                    delay(delayMillis)
                    showNotification(todo, minutesBefore)
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing notification", e)
                }
            }
            activeNotifications[todo.id] = job
            Log.d(TAG, "Scheduled notification for todo ${todo.id} in ${delayMillis / 1000} seconds")
        } else {
            Log.d(TAG, "Notification time has already passed for todo ${todo.id}")
        }
    }

    fun showNotification(todo: Todo, minutesBefore: Int) {
        if (!hasNotificationPermission()) {
            Log.e(TAG, "Notification permission not granted")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            todo.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Reminder")
            .setContentText("${todo.name} is due in $minutesBefore minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(todo.id, builder.build())
            Log.d(TAG, "Test notification shown for todo ${todo.id}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while showing notification", e)
        }
    }

    fun cancelNotification(todoId: Int) {
        activeNotifications[todoId]?.cancel()
        activeNotifications.remove(todoId)
        notificationManager.cancel(todoId)
        Log.d(TAG, "Cancelled notification for todo $todoId")
    }

    fun cancelAllNotifications() {
        activeNotifications.values.forEach { it.cancel() }
        activeNotifications.clear()
        notificationManager.cancelAll()
        Log.d(TAG, "Cancelled all notifications")
    }

    companion object {
        const val CHANNEL_ID = "todo_notifications"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
    }
} 