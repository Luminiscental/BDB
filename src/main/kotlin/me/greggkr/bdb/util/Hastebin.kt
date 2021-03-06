package me.greggkr.bdb.util

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

private const val URL = "https://hastebin.com"

object Hastebin {
    private val httpClient = OkHttpClient.Builder().build()

    fun hastebin(text: String): String? {
        val data = httpClient.newCall(
                Request.Builder()
                        .url("$URL/documents")
                        .post(RequestBody.create(MediaType.parse("text/plain"), text))
                        .build())
                .execute()
                .body()
                ?.string()

        return "$URL/${JSONObject(data)["key"]}"
    }
}