package me.greggkr.bdb.util.db

import me.greggkr.bdb.util.toHex
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color

class Data(private val db: Database) {
    private val defualtPrefix = "!"
    private val defaultColor = Color.decode("#934cff")

    private val prefixes = HashMap<String, String>()

    private val owners = listOf(
            184041169796333568, // Gregg
            184733130123378688, // Landon
            216596910792769537 // Yosh
    )

    fun isOwner(user: User) = owners.contains(user.idLong)

    fun getPrefix(guild: Guild): String {
        if (prefixes.contains(guild.id)) return prefixes[guild.id]!!

        val prefix = db.getPrefix(guild.id) ?: defualtPrefix

        prefixes[guild.id] = prefix
        return prefix
    }

    fun setPrefix(guild: Guild, prefix: String) {
        db.setPrefix(guild.id, prefix)
        prefixes[guild.id] = prefix
    }

    fun getModLogChannel(guild: Guild): TextChannel? {
        val ch = db.getModLogChannel(guild.id) ?: return null
        return guild.getTextChannelById(ch)
    }

    fun setModLogChannel(guild: Guild, channel: TextChannel) {
        db.setModLogChannel(guild.id, channel.idLong)
    }

    fun getColor(guild: Guild): Color {
        val raw = db.getColor(guild.id) ?: return defaultColor

        return Color.decode(raw) ?: defaultColor
    }

    fun setColor(guild: Guild, color: Color) {
        db.setColor(guild.id, color.toHex())
    }
}