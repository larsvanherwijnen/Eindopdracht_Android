package nl.avans.todo.data.api

import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

object SupabaseClient {

    fun makeRequest(urlString: String, method: String, token: String? = null, body: String? = null): String? {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("apikey", SupabaseConfig.API_KEY)
        connection.setRequestProperty("Content-Type", "application/json")
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }

        if (body != null) {
            connection.doOutput = true
            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.writeBytes(body)
            outputStream.flush()
            outputStream.close()
        }

        val responseCode = connection.responseCode
        return if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }
        }
    }
}

