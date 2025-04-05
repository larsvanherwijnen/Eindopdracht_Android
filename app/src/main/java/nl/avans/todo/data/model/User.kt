package nl.avans.todo.data.model

import org.json.JSONObject

data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val createdAt: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): User {
            return User(
                id = json.getString("id"),
                email = json.getString("email"),
                name = json.optString("name"),
                createdAt = json.optString("created_at")
            )
        }
    }
} 