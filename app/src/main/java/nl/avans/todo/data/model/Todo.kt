package nl.avans.todo.data.model

import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter

data class Todo(
    val id: Int,
    val name: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val dueDate: LocalDateTime? = null,
    val notificationEnabled: Boolean = false,
    val notificationMinutesBefore: Int = 30,  // Default 30 minutes before
    val addedToCalendar: Boolean = false,
    val userId: String? = null,
    val createdAt: LocalDateTime? = null
) {
    companion object {
        private val UTC = ZoneId.of("UTC")
        // Use ISO format without timezone bracket notation
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")

        fun fromJson(json: JSONObject): Todo {
            val dueDateStr = json.optString("due_date", "")
            val dueDate = if (dueDateStr.isNotEmpty()) {
                try {
                    // Parse the timestamp from Supabase (which is in UTC)
                    val instant = Instant.parse(dueDateStr)
                    // Convert to local date time
                    LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                } catch (e: Exception) {
                    null
                }
            } else null

            val createdAtStr = json.optString("created_at", "")
            val createdAt = if (createdAtStr.isNotEmpty()) {
                try {
                    // Parse the timestamp from Supabase (which is in UTC)
                    val instant = Instant.parse(createdAtStr)
                    // Convert to local date time
                    LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                } catch (e: Exception) {
                    null
                }
            } else null
            
            return Todo(
                id = json.optInt("id", 0),
                name = json.optString("name", ""),
                description = json.optString("description", ""),
                completed = json.optBoolean("completed", false),
                dueDate = dueDate,
                notificationEnabled = json.optBoolean("notification_enabled", true),
                notificationMinutesBefore = json.optInt("notification_minutes_before", 30),
                addedToCalendar = json.optBoolean("added_to_calendar", false),
                userId = json.optString("user_id", null),
                createdAt = createdAt
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            if (id > 0) put("id", id)
            put("name", name)
            put("description", description)
            put("completed", completed)
            dueDate?.let { 
                // Convert local time to UTC ISO string that Supabase expects
                val utcInstant = it.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(UTC)
                    .toInstant()
                put("due_date", utcInstant.toString())
            }
            put("notification_enabled", notificationEnabled)
            put("notification_minutes_before", notificationMinutesBefore)
            put("added_to_calendar", addedToCalendar)
            userId?.let { put("user_id", it) }
            createdAt?.let {
                val utcInstant = it.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(UTC)
                    .toInstant()
                put("created_at", utcInstant.toString())
            }
        }
    }
} 