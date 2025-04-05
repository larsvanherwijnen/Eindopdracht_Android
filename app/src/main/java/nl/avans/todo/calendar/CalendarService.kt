package nl.avans.todo.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import nl.avans.todo.data.model.Todo
import java.time.ZoneId

class CalendarService(private val context: Context) {
    private val TAG = "CalendarService"

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getDefaultCalendarId(): Long? {
        if (!hasCalendarPermission()) {
            Log.e(TAG, "Calendar permissions not granted")
            return null
        }

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"
        
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    cursor.getLong(idIndex)
                } else {
                    Log.e(TAG, "No suitable calendar found")
                    null
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while accessing calendar", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing calendar", e)
            null
        }
    }

    fun addTodoToCalendar(todo: Todo): Boolean {
        if (!hasCalendarPermission()) {
            Log.e(TAG, "Calendar permission not granted")
            return false
        }

        if (todo.dueDate == null) {
            Log.e(TAG, "Todo has no due date")
            return false
        }

        val calendarId = getDefaultCalendarId() ?: return false

        val event = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, todo.name)
            put(CalendarContract.Events.DESCRIPTION, todo.description)
            put(CalendarContract.Events.DTSTART, todo.dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND, todo.dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, event)
            if (uri != null) {
                val eventId = ContentUris.parseId(uri)
                Log.d(TAG, "Event added to calendar with ID: $eventId")
                true
            } else {
                Log.e(TAG, "Failed to add event to calendar")
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while adding event to calendar", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event to calendar", e)
            false
        }
    }

    fun removeTodoFromCalendar(todo: Todo): Boolean {
        if (!hasCalendarPermission()) {
            Log.e(TAG, "Calendar permission not granted")
            return false
        }

        // Note: This is a simplified implementation. In a real app, you would need to store
        // the calendar event ID when creating the event and use it here to delete the specific event.
        // For now, we'll just return true to indicate the operation was attempted.
        return true
    }
} 