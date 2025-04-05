package nl.avans.todo.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var sortOrder: SortOrder
        get() = SortOrder.valueOf(prefs.getString(KEY_SORT_ORDER, SortOrder.DUE_DATE_ASC.name)!!)
        set(value) = prefs.edit { putString(KEY_SORT_ORDER, value.name) }

    companion object {
        private const val PREF_NAME = "user_preferences"
        private const val KEY_SORT_ORDER = "sort_order"
    }
}

enum class SortOrder(val apiValue: String) {
    CREATED_ASC("created_at.asc"),
    CREATED_DESC("created_at.desc"),
    NAME_ASC("name.asc"),
    NAME_DESC("name.desc"),
    DUE_DATE_ASC("due_date.asc,name.asc"),  // Sort by due date, then by name
    DUE_DATE_DESC("due_date.desc,name.asc"), // Sort by due date descending, then by name
    DUE_SOON_FIRST("CASE WHEN due_date IS NULL THEN 2 " +
                   "WHEN due_date < NOW() THEN 0 " +
                   "ELSE 1 END, due_date.asc,name.asc") // Overdue first, then upcoming, then no due date
} 