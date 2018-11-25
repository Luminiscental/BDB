package me.greggkr.bdb.osu

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import me.greggkr.bdb.config
import me.greggkr.bdb.util.Config
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

private val API_KEY = config[Config.Osu.apiKey]

private val gson = GsonBuilder().create()
private val client = OkHttpClient.Builder()
        .authenticator { _, response ->
            response
                    .request()
                    .newBuilder()
                    .url(response
                            .request()
                            .url()
                            .newBuilder()
                            .addQueryParameter("k", API_KEY)
                            .build()
                    ).build()
        }
        .build()

data class RecentScore(
        @SerializedName("beatmap_id")
        val beatmapId: String,
        @SerializedName("count100")
        val count100: String,
        @SerializedName("count300")
        val count300: String,
        @SerializedName("count50")
        val count50: String,
        @SerializedName("countgeki")
        val countgeki: String,
        @SerializedName("countkatu")
        val countkatu: String,
        @SerializedName("countmiss")
        val countmiss: String,
        @SerializedName("date")
        val date: String,
        @SerializedName("enabled_mods")
        val enabledMods: String,
        @SerializedName("maxcombo")
        val maxcombo: String,
        @SerializedName("perfect")
        val perfect: String,
        @SerializedName("rank")
        val rank: String,
        @SerializedName("score")
        val score: String,
        @SerializedName("user_id")
        val userId: String
)

enum class UserType(val apiName: String) {
    ID("id"), USERNAME("string")
}

class Osu {
    companion object {
        fun getUserRecent(user: String, mode: String = "0", amount: Int = 1, userType: UserType = UserType.USERNAME): Array<RecentScore>? {
            val ret = makeRequest(Request.Builder()
                    .url(HttpUrl.parse("https://osu.ppy.sh/api/get_user_recent")!!
                            .newBuilder()
                            .addQueryParameter("u", user)
                            .addQueryParameter("m", mode)
                            .addQueryParameter("type", userType.apiName)
                            .addQueryParameter("limit", amount.toString())
                            .build())
                    .build())

            return gson.fromJson(ret, Array<RecentScore>::class.java)
        }

        private fun makeRequest(req: Request): JsonElement? {
            val res = client.newCall(req).execute()
            val body = res.body() ?: return null

            return gson.fromJson(body.string(), JsonElement::class.java)
        }
    }
}