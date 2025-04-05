package nl.avans.todo.data.api

import android.content.Context
import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private var requestQueue: RequestQueue? = null

    fun initialize(context: Context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.applicationContext)
            Log.d(TAG, "SupabaseClient initialized")
        }
    }

    data class ApiResponse(
        val data: String? = null,
        val statusCode: Int = 0,
        val errorMessage: String? = null,
        val headers: Map<String, String>? = null
    )

    suspend fun makeRequest(
        url: String,
        method: String,
        token: String? = null,
        body: String? = null
    ): String? = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "Making $method request to: $url")
        if (body != null) {
            Log.d(TAG, "Request body: $body")
        }
        
        val request = object : StringRequest(
            getVolleyMethod(method),
            url,
            { response ->
                Log.d(TAG, "Request successful: ${response.take(200)}${if (response.length > 200) "..." else ""}")
                continuation.resume(response)
            },
            { error ->
                val statusCode = error.networkResponse?.statusCode ?: 0
                val responseData = error.networkResponse?.data?.let { String(it) }
                val headers = error.networkResponse?.headers?.toMap()
                
                Log.e(TAG, "Request failed with status code: $statusCode")
                Log.e(TAG, "URL: $url, Method: $method")
                
                if (token != null) {
                    Log.d(TAG, "Using auth token: ${token.take(10)}...")
                } else {
                    Log.d(TAG, "No auth token provided")
                }
                
                if (statusCode == 401) {
                    Log.e(TAG, "401 Unauthorized: Authentication failed or token expired")
                } else if (statusCode == 403) {
                    Log.e(TAG, "403 Forbidden: Insufficient permissions for this request")
                }
                
                responseData?.let {
                    Log.e(TAG, "Response data: $it")
                }
                
                headers?.let {
                    Log.d(TAG, "Response headers: $it")
                }
                
                // Log specific error details
                error.printStackTrace()
                Log.e(TAG, "Error: ${error.message ?: "Unknown error"}")
                
                continuation.resume(null)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = mutableMapOf(
                    "apikey" to SupabaseConfig.API_KEY,
                    "Content-Type" to "application/json"
                )
                
                if (token != null) {
                    headers["Authorization"] = "Bearer $token"
                } else {
                    headers["Authorization"] = "Bearer ${SupabaseConfig.API_KEY}"
                }
                
                Log.d(TAG, "Request headers: $headers")
                return headers
            }

            override fun getBody(): ByteArray {
                return body?.toByteArray() ?: ByteArray(0)
            }
            
            override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                val parsed = super.parseNetworkResponse(response)
                
                Log.d(TAG, "Response status code: ${response.statusCode}")
                Log.d(TAG, "Response headers: ${response.headers}")
                
                return parsed
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