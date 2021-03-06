package me.greggkr.bdb.handlers

import me.diax.comportment.jdacommand.CommandHandler
import me.greggkr.bdb.data
import me.greggkr.bdb.util.Emoji
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class CommandHandler(private val handler: CommandHandler) : ListenerAdapter() {
    override fun onMessageReceived(e: MessageReceivedEvent) {
        if (e.author.isBot || e.channelType != ChannelType.TEXT) return

        val channel = e.channel
        val msg = e.message
        val content = msg.contentRaw
        val guild = e.guild

        val prefix = data.getPrefix(guild) // TODO: replace with config value
        if (content == guild.selfMember.asMention) {
            channel.sendMessage("My prefix here is: `$prefix`.").queue()
            return
        }

        if (!content.startsWith(prefix)) return

        val args = content.split(Regex("\\s+"), 2)
        val trigger = args[0].substring(prefix.length)

        val cmd = handler.findCommand(trigger.toLowerCase()) ?: return

        val user = e.author
        val member = e.member

        if (data.isBlacklisted(user.id)) return

        if (cmd.hasAttribute("botOwnerOnly")) {
            if (!data.isOwner(user)) return
        }

        if (cmd.hasAttribute("adminOnly")) {
            if (!member.isOwner && !data.isOwner(user) && !member.hasPermission(Permission.MANAGE_SERVER)) return
        }

        if (cmd.hasAttribute("modOnly")) {
            val role = data.getModRole(guild)
            if (role != null) {
                if (!member.isOwner && !data.isOwner(user) && !member.roles.contains(role)) return
            } else {
                if (!member.isOwner && !data.isOwner(user)) return
            }
        }

        if (cmd.hasAttribute("nsfw") && !(channel as TextChannel).isNSFW) {
            channel.sendMessage("${Emoji.X} This command must be used from a NSFW channel.")
            return
        }

        handler.execute(cmd, e.message, if (args.size > 1) args[1] else "")
    }
}