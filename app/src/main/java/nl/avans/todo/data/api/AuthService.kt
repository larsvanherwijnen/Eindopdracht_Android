package nl.avans.todo.data.api

import org.json.JSONObject

object AuthService {

    fun register(email: String, password: String): String? {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/signup"
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        return SupabaseClient.makeRequest(url, "POST", body = body)
    }

    fun login(email: String, password: String): String? {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/token?grant_type=password"
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        return SupabaseClient.makeRequest(url, "POST", body = body)
    }

    fun logout(token: String): Boolean {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/logout"
        return SupabaseClient.makeRequest(url, "POST", token) != null
    }

    fun getUser(token: String): String? {
        val url = "${SupabaseConfig.BASE_URL}/auth/v1/user"
        return SupabaseClient.makeRequest(url, "GET", token)
    }
}