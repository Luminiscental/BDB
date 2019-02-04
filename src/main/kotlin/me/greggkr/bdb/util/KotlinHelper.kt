package me.greggkr.bdb.util

import com.oopsjpeg.osu4j.GameMode
import me.greggkr.bdb.data
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.text.DecimalFormat

fun Color.toHex(): String {
    return "#${Integer.toHexString(rgb).substring(2)}"
}

fun User.prettyString(): String {
    return "${this.name}#${this.discriminator}"
}

fun Double.toPercent(multiply: Boolean = true): String {
    return DecimalFormat("#.##").format(this * if (multiply) 100 else 1) + "%"
}

fun EmbedBuilder.addInlineField(title: String, description: String): EmbedBuilder {
    this.addField(title, description, true)
    return this
}

fun EmbedBuilder.addInlineField(title: String, description: Int): EmbedBuilder {
    this.addInlineField(title, description.toString())
    return this
}

fun EmbedBuilder.addInlineField(title: String, description: Double): EmbedBuilder {
    this.addInlineField(title, description.toString())
    return this
}

fun EmbedBuilder.addInlineField(title: String, description: Float): EmbedBuilder {
    this.addInlineField(title, description.toString())
    return this
}

fun gameModeFromName(name: String): GameMode? {
    return when (name.toLowerCase()) {
        "std", "standard", "osu!standard", "m0" -> GameMode.STANDARD
        "taiko", "tai", "tak", "m1" -> GameMode.TAIKO
        "ctb", "catchthebeat", "beat", "m2" -> GameMode.CATCH_THE_BEAT
        "mania", "man", "osu!mania", "m3" -> GameMode.MANIA
        else -> null
    }
}

fun userFromString(message: Message, arg: String): String? {
    val guild = message.guild
    if (!arg.isEmpty()) {
        if (arg.contains("@") && !message.mentionedUsers.isEmpty()) {
            val mentionedUser = data.getOsuUser(guild, message.mentionedUsers[0])
            if (!mentionedUser.isNullOrEmpty()) {
                return mentionedUser
            }
        }
        // If it's a number assume it's a parameter not a username
        if (!arg.matches(Regex("\\d+"))) {
            return arg
        }
    }
    return null
}

fun argsFromString(args: String): List<String> {
    return args.split(Regex("\\s+\\|\\s+"))
}