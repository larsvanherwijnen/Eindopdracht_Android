package nl.avans.todo.data.api

import android.util.Log
import nl.avans.todo.data.model.User
import org.json.JSONObject

object AuthService {
    private const val TAG = "AuthService"

    suspend fun register(email: String, password: String): String? {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/signup"
        Log.d(TAG, "Registering new user with email: $email")
        
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        
        try {
            val response = SupabaseClient.makeRequest(url, "POST", body = body)
            
            if (response != null) {
                Log.d(TAG, "Registration successful")
                return response
            } else {
                Log.e(TAG, "Registration failed: null response")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during registration: ${e.message}", e)
            throw e
        }
    }

    suspend fun login(email: String, password: String): Pair<String, User>? {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/token?grant_type=password"
        Log.d(TAG, "Logging in user with email: $email")
        
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        
        try {
            val response = SupabaseClient.makeRequest(url, "POST", body = body)
            
            if (response != null) {
                val jsonObject = JSONObject(response)
                Log.d(TAG, "Login successful. Access token received.")
                
                val accessToken = jsonObject.getString("access_token")
                val userJson = jsonObject.getJSONObject("user")
                val user = User.fromJson(userJson)
                
                return Pair(accessToken, user)
            } else {
                Log.e(TAG, "Login failed: null response")
                throw Exception("Invalid credentials")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during login: ${e.message}", e)
            when {
                e.message?.contains("400") == true -> throw Exception("Invalid email or password")
                e.message?.contains("401") == true -> throw Exception("Invalid credentials")
                e.message?.contains("403") == true -> throw Exception("Access denied")
                else -> throw Exception("Login failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    suspend fun logout(token: String): Boolean {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/logout"
        Log.d(TAG, "Logging out user")
        
        try {
            val response = SupabaseClient.makeRequest(url, "POST", token)
            val success = response != null
            
            if (success) {
                Log.d(TAG, "Logout successful")
            } else {
                Log.e(TAG, "Logout failed: null response")
            }
            
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}", e)
            throw e
        }
    }

    suspend fun getUser(token: String): User? {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/user"
        Log.d(TAG, "Getting user information")
        
        try {
            val response = SupabaseClient.makeRequest(url, "GET", token)
            
            if (response != null) {
                val jsonObject = JSONObject(response)
                val user = User.fromJson(jsonObject)
                Log.d(TAG, "Retrieved user info for ID: ${user.id}")
                return user
            } else {
                Log.e(TAG, "Failed to get user info: null response")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user info: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateUser(token: String, email: String? = null, password: String? = null): User? {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/user"
        Log.d(TAG, "Updating user information")
        
        val body = JSONObject().apply {
            email?.let { put("email", it) }
            password?.let { put("password", it) }
        }.toString()
        
        try {
            val response = SupabaseClient.makeRequest(url, "PUT", token, body)
            
            if (response != null) {
                val jsonObject = JSONObject(response)
                val user = User.fromJson(jsonObject)
                Log.d(TAG, "User information updated successfully")
                return user
            } else {
                Log.e(TAG, "Failed to update user info: null response")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user info: ${e.message}", e)
            throw e
        }
    }
}