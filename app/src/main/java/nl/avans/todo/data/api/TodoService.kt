package nl.avans.todo.data.api

import org.json.JSONObject

object TodoService {

    suspend fun getTasks(token: String): String? {
        val url = "${SupabaseConfig.BASE_URL}/rest/v1/todo"
        return SupabaseClient.makeRequest(url, "GET", token)
    }

    suspend fun addTask(token: String, title: String): String? {
        val url = "${SupabaseConfig.BASE_URL}/rest/v1/todo"
        val body = JSONObject().apply {
            put("title", title)
            put("completed", false)
        }.toString()
        return SupabaseClient.makeRequest(url, "POST", token, body)
    }

    suspend fun updateTask(token: String, id: Int, completed: Boolean): String? {
        val url = "${SupabaseConfig.BASE_URL}/rest/v1/todo?id=eq.$id"
        val body = JSONObject().apply {
            put("completed", completed)
        }.toString()
        return SupabaseClient.makeRequest(url, "PATCH", token, body)
    }

    suspend fun deleteTask(token: String, id: Int): String? {
        val url = "${SupabaseConfig.BASE_URL}/rest/v1/todo?id=eq.$id"
        return SupabaseClient.makeRequest(url, "DELETE", token)
    }
}