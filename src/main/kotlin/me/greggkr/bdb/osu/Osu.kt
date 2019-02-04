package me.greggkr.bdb.osu

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.oopsjpeg.osu4j.GameMod
import com.oopsjpeg.osu4j.GameMode
import com.oopsjpeg.osu4j.OsuScore
import me.greggkr.bdb.config
import me.greggkr.bdb.data
import me.greggkr.bdb.starFormat
import me.greggkr.bdb.util.*
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.LocalDateTime

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

enum class UserType(val apiName: String) {
    ID("id"), USERNAME("string")
}

enum class OsuMod(val mod: String) {
    SO("SO"),
    EZ("EZ"),
    HT("HT"),
    HD("HD"),
    NC("HC"),
    DT("DT"),
    HR("HR"),
    NF("NF"),
    SD("SD"),
    PF("PF"),
    FL("FL")
}

data class OsuParams(val user: String?,
                     val mode: GameMode,
                     val params: List<String>
)

class Osu {
    companion object {

        fun prettyMods(mods: Array<GameMod>): String {
            return if (mods.isEmpty()) {
                ""
            } else {
                val ordered = mods.map { OsuMod.valueOf(it.shortName) }.sorted()
                "+" + ordered.joinToString(separator = "") { it.mod }
            }
        }

        fun prettyRank(rank: String): String {
            return rank.replace("X", "SS")
        }

        fun prettyMode(mode: GameMode): String {
            return when(mode) {
                GameMode.STANDARD -> "osu!"
                GameMode.TAIKO -> "osu!taiko"
                GameMode.MANIA -> "osu!mania"
                GameMode.CATCH_THE_BEAT -> "osu!catch"
                else -> ""
            }
        }

        private fun prettyTime(score: OsuScore): String {
            val duration = Duration.between(score.date.toOffsetDateTime().toLocalDateTime(), LocalDateTime.now())

            val days = duration.toDays()
            val hours = duration.minusDays(days).toHours()
            val minutes = duration.minusHours(hours).toMinutes()
            val seconds = duration.minusMinutes(minutes).seconds

            var timeInfo = ""

            if (days > 0) {
                timeInfo += if (days > 1) "$days days "
                else "one day "
            }

            timeInfo += if (hours > 0) {
                if (hours > 1) "$hours hours "
                else "one hour "
            } else if (minutes > 0 ) {
                if (minutes > 1) "$minutes minutes "
                else "one minute "
            } else {
                if (seconds > 1) "$seconds seconds "
                else "one second "
            }

            return timeInfo + "ago"
        }

        fun playTitle(score: OsuScore): String {
            val map = score.beatmap.get()
            val mods = Osu.prettyMods(score.enabledMods)
            val timeInfo = Osu.prettyTime(score)

            return "${map.title} [${map.version}] $mods - ${starFormat.format(map.difficulty)}* ($timeInfo)"
        }

        fun getUserAndMode(message: Message, args: List<String>, requireUser: Boolean = true): OsuParams {
            val params = mutableListOf<String>()
            val guild = message.guild
            val channel = message.channel

            var user: String? = null
            var mode: GameMode? = null

            for (arg in args) {
                // Interpreted as mode before user
                // e.g. "profile taiko" gives your taiko profile rather than the profile for user taiko
                // Also ordered, so "profile taiko taiko" gives the taiko profile for user taiko
                if (mode == null) {
                    val asMode = gameModeFromName(arg)
                    if (asMode != null) {
                        mode = asMode
                        continue
                    }
                }
                if (requireUser && user.isNullOrEmpty()) {
                    val asUser = userFromString(message, arg)
                    if (asUser != null) {
                        user = asUser
                        continue
                    }
                }
                params.add(arg)
            }

            if (user.isNullOrEmpty()) {
                val authorUser = data.getOsuUser(guild, message.author)
                if (!authorUser.isNullOrEmpty()) {
                    user = authorUser
                }
            }

            if (requireUser && user.isNullOrEmpty()) {
                channel.sendMessage("${Emoji.X} You must supply a valid user. Either the person you mentioned or you do not have a linked user. Use ${data.getPrefix(guild)}user <username>.").queue()
            }

            // TODO: Per-user default mode
            return OsuParams(user, mode?: GameMode.STANDARD, params)
        }

        fun getNumberArgument(params: List<String>, default: Int, min: Int, max: Int, index: Int = 0): Int {
            if (params.size <= index) return default
            var tmp = params[index].toIntOrNull() ?: default

            if (tmp < min) {
                tmp = min
            }

            if (tmp > max) {
                tmp = max
            }

            return tmp
        }

        private fun makeRequest(req: Request): JsonElement? {
            val res = client.newCall(req).execute()
            val body = res.body() ?: return null

            return gson.fromJson(body.string(), JsonElement::class.java)
        }
    }
}
