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
import kotlin.coroutines.resumeWithException

object SupabaseClient {
    private lateinit var requestQueue: RequestQueue

    fun initialize(context: Context) {
        requestQueue = Volley.newRequestQueue(context.applicationContext)
    }

    suspend fun makeRequest(urlString: String, method: String, token: String? = null, body: String? = null): String? {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val requestMethod = when (method) {
                    "GET" -> Request.Method.GET
                    "POST" -> Request.Method.POST
                    "PATCH" -> Request.Method.PATCH
                    "DELETE" -> Request.Method.DELETE
                    else -> Request.Method.GET
                }

                val headers = HashMap<String, String>().apply {
                    put("apikey", SupabaseConfig.API_KEY)
                    put("Content-Type", "application/json")
                    token?.let { put("Authorization", "Bearer $it") }
                }

                val jsonBody = body?.let { JSONObject(it) }

                val request = if (jsonBody != null) {
                    JsonObjectRequest(requestMethod, urlString, jsonBody,
                        { response ->
                            continuation.resume(response.toString())
                        },
                        { error ->
                            continuation.resume(null)
                        }
                    ).apply {
                        headers.forEach { (key, value) ->
                            this.headers[key] = value
                        }
                    }
                } else {
                    object : StringRequest(requestMethod, urlString,
                        { response ->
                            continuation.resume(response)
                        },
                        { error ->
                            continuation.resume(null)
                        }
                    ) {
                        override fun getHeaders(): MutableMap<String, String> {
                            return headers
                        }
                    }
                }

                requestQueue.add(request)

                continuation.invokeOnCancellation {
                    request.cancel()
                }
            }
        }
    }
}