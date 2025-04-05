package nl.avans.todo.data.api

import android.util.Log
import nl.avans.todo.data.model.Todo
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList

object TodoService {
    private const val TAG = "TodoService"

    suspend fun getTasks(token: String, userId: String?, sortOrder: String? = null): List<Todo>? {
        val baseUrl = "${SupabaseConfig.BASE_URL}/rest/v1/tasks"
        val url = buildString {
            append(baseUrl)
            if (userId != null) {
                Log.d(TAG, "Fetching tasks for user ID: $userId")
                append("?user_id=eq.$userId")
                if (sortOrder != null) {
                    append("&order=$sortOrder")
                }
            } else {
                Log.d(TAG, "Fetching all tasks (no user ID provided)")
                append("?select=*")
                if (sortOrder != null) {
                    append("&order=$sortOrder")
                }
            }
        }
        
        try {
            val response = SupabaseClient.makeRequest(url, "GET", token)
            
            if (response != null) {
                Log.d(TAG, "Successfully retrieved tasks")
                return parseTasksResponse(response)
            } else {
                Log.e(TAG, "Failed to retrieve tasks: null response")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving tasks: ${e.message}", e)
            throw e
        }
    }

    suspend fun addTask(token: String, todo: Todo, userId: String?): Todo? {
        val url = "${SupabaseConfig.BASE_URL}/rest/v1/tasks"
        val jsonObject = todo.toJson()
        
        // Add user_id to the task if available
        if (userId != null) {
            jsonObject.put("user_id", userId)
            Log.d(TAG, "Adding task with user ID: $userId")
        } else {
            Log.w(TAG, "Adding task without user ID")
        }
        
        val body = jsonObject.toString()
        
        try {
            val response = SupabaseClient.makeRequest(url, "POST", token, body)
            
            // POST operations may return empty responses with 201 Created or 204 No Content
            if (response != null) {
                if (response.isEmpty()) {
                    Log.d(TAG, "Task added successfully (empty response)")
                    return todo.copy() // Return a copy of the original todo object
                } else {
                    Log.d(TAG, "Successfully added task: $response")
                    return JSONObject(response).let { Todo.fromJson(it) }
                }
            } else {
                Log.e(TAG, "Failed to add task: null response")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding task: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateTask(token: String, todo: Todo): Todo? {
        val url = "${SupabaseConfig.BASE_URL}/rest/v1/tasks?id=eq.${todo.id}"
        val body = todo.toJson().toString()
        
        Log.d(TAG, "Updating task with ID: ${todo.id}")
        
        try {
            val response = SupabaseClient.makeRequest(url, "PATCH", token, body)
            
            // PATCH operations may return empty responses with 204 No Content
            if (response != null) {
                if (response.isEmpty()) {
                    Log.d(TAG, "Task updated successfully (empty response)")
                    return todo // Return the original todo object that was passed in
                } else {
                    Log.d(TAG, "Successfully updated task: $response")
                    return JSONObject(response).let { Todo.fromJson(it) }
                }
            } else {
                Log.e(TAG, "Failed to update task: null response")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteTask(token: String, id: Int): Boolean {
        val url = "${SupabaseConfig.BASE_URL}/rest/v1/tasks?id=eq.$id"
        
        Log.d(TAG, "Deleting task with ID: $id")
        
        try {
            val response = SupabaseClient.makeRequest(url, "DELETE", token)
            
            // DELETE operations often return empty responses with 204 No Content
            // We consider a non-null response (even if empty) as success
            val success = response != null
            
            if (success) {
                Log.d(TAG, "Successfully deleted task with ID: $id")
                if (response?.isEmpty() == true) {
                    Log.d(TAG, "Empty response, this is normal for DELETE operations")
                }
            } else {
                Log.e(TAG, "Failed to delete task with ID: $id")
            }
            
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task: ${e.message}", e)
            throw e
        }
    }
    
    private fun parseTasksResponse(response: String): List<Todo> {
        try {
            val todos = ArrayList<Todo>()
            val jsonArray = JSONArray(response)
            
            Log.d(TAG, "Parsing ${jsonArray.length()} tasks")
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                todos.add(Todo.fromJson(jsonObject))
            }
            
            return todos
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tasks response: ${e.message}", e)
            throw e
        }
    }
}