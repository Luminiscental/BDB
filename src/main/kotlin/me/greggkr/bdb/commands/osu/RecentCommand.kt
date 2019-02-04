package me.greggkr.bdb.commands.osu

import com.oopsjpeg.osu4j.GameMode
import com.oopsjpeg.osu4j.backend.EndpointUserRecents
import me.diax.comportment.jdacommand.Command
import me.diax.comportment.jdacommand.CommandDescription
import me.greggkr.bdb.*
import me.greggkr.bdb.analysis.analyse
import me.greggkr.bdb.analysis.getTaikoAcc
import me.greggkr.bdb.osu.Osu
import me.greggkr.bdb.util.Emoji
import me.greggkr.bdb.util.argsFromString
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message

@CommandDescription(name = "recent", triggers = [
    "recent"
], description = "Displays the most recent play for a user.")
class RecentCommand : Command {
    override fun execute(message: Message, args: String) {
        val guild = message.guild
        val channel = message.channel
        val p = Osu.getUserAndMode(message, argsFromString(args))

        val user = p.user ?: return

        if (p.mode != GameMode.STANDARD && p.mode != GameMode.TAIKO) {
            channel.sendMessage("Only std/taiko are supported atm sorry.").queue()
            return
        }

        val recent = osu.userRecents.getAsQuery(EndpointUserRecents.ArgumentsBuilder(user)
                .setMode(p.mode)
                .setLimit(1)
                .build())
                .resolve()

        if (recent.isNullOrEmpty()) {
            channel.sendMessage("${Emoji.X} That user does not have a recent play.").queue()
            return
        }

        val play = recent[0]
        val map = play.beatmap.get()

        val rank = Osu.prettyRank(play.rank)
        val comboInfo = "${play.maxCombo}x/${map.maxCombo}x"
        val playTitle = Osu.prettyMode(p.mode) + " " + Osu.playTitle(play)

        var description = ""
        val embed = EmbedBuilder()
                .setColor(data.getColor(guild))
                .setTitle(playTitle)

        if (p.mode == GameMode.STANDARD) {

            val analytics = analyse(map.id, play.maxCombo, play.hit300, play.hit100, play.hit50, play.misses, play.enabledMods)

            if (rank.contains("F")) {
                val mapCompletion = analytics.mapCompletion
                description = "Map completion: ${percentFormat.format(mapCompletion)}"
            }

            val acc = analytics.accuracy
            val pp = analytics.performance

            embed
                    .addField("Rank, Combo, Acc", rank + ", $comboInfo, ${accuracyFormat.format(acc)}", false)
                    .addField("PP", ppFormat.format(pp), false)
                    .addField("PP Breakdown", "Aim: ${ppFormat.format(analytics.aimPerformance)}\n" +
                            "Speed: ${ppFormat.format(analytics.speedPerformance)}\n" +
                            "Acc: ${ppFormat.format(analytics.accuracyPerformance)}", false)
                    .addField("300/100/50/miss", "${play.hit300}/${play.hit100}/${play.hit50}/${play.misses}", false)
        } else {

            val acc = getTaikoAcc(play.gekis, play.katus, play.misses)
            val pp = play.pp

            // No completion on fails because maxCombo isn't provided in taiko
            // No pp breakdown because analytics only supports std atm

            embed
                    .addField("Rank, Combo, Acc", rank + "$comboInfo, ${accuracyFormat.format(acc)}", false)
                    .addField("PP", ppFormat.format(pp), false)
        }

        channel.sendMessage(embed
                .setDescription(description)
                .build())
                .queue()
    }
}
