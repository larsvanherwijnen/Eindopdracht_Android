package nl.avans.todo.data.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume

object SupabaseClient {
    private var requestQueue: RequestQueue? = null

    fun initialize(context: Context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.applicationContext)
        }
    }

    suspend fun makeRequest(
        url: String,
        method: String,
        token: String? = null,
        body: String? = null
    ): String? = suspendCancellableCoroutine { continuation ->
        val request = object : StringRequest(
            getVolleyMethod(method),
            url,
            { response ->
                continuation.resume(response)
            },
            { error ->
                continuation.resume(null)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "apikey" to SupabaseConfig.API_KEY,
                    "Authorization" to "Bearer $token",
                    "Content-Type" to "application/json"
                )
            }

            override fun getBody(): ByteArray {
                return body?.toByteArray() ?: ByteArray(0)
            }
        }

        requestQueue?.add(request)
    }

    private fun getVolleyMethod(method: String): Int {
        return when (method.uppercase()) {
            "GET" -> Request.Method.GET
            "POST" -> Request.Method.POST
            "PUT" -> Request.Method.PUT
            "DELETE" -> Request.Method.DELETE
            "PATCH" -> Request.Method.PATCH
            else -> Request.Method.GET
        }
    }
}