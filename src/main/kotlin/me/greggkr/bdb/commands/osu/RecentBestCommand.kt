package me.greggkr.bdb.commands.osu

import com.oopsjpeg.osu4j.GameMode
import com.oopsjpeg.osu4j.OsuScore
import com.oopsjpeg.osu4j.backend.EndpointUserBests
import me.diax.comportment.jdacommand.Command
import me.diax.comportment.jdacommand.CommandDescription
import me.greggkr.bdb.*
import me.greggkr.bdb.analysis.Analytics
import me.greggkr.bdb.analysis.analyse
import me.greggkr.bdb.analysis.getTaikoAcc
import me.greggkr.bdb.osu.Osu
import me.greggkr.bdb.util.Emoji
import me.greggkr.bdb.util.argsFromString
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed

data class ScoreInfo(val pp: Analytics,
                     val complete: Boolean,
                     val score: OsuScore
)

@CommandDescription(name = "recentbest", triggers = [
    "recentbest"
], description = "Displays the most recent plays from a users top 100.")
class RecentBestCommand : Command {
    override fun execute(message: Message, args: String) {
        val guild = message.guild
        val channel = message.channel
        val p = Osu.getUserAndMode(message, argsFromString(args))

        val user = p.user ?: return
        val amount = Osu.getNumberArgument(p.params, 1, 1, 10)

        if (p.mode != GameMode.STANDARD && p.mode != GameMode.TAIKO) {
            channel.sendMessage("Only std/taiko are supported atm sorry.").queue()
            return
        }

        val best = osu.userBests.getAsQuery(EndpointUserBests.ArgumentsBuilder(user)
                .setMode(p.mode)
                .setLimit(100)
                .build())
                .resolve()

        if (best.isNullOrEmpty()) {
            channel.sendMessage("${Emoji.X} Please provide a valid user; $user has no best plays in ${Osu.prettyMode(p.mode)}").queue()
            return
        }

        best.sortByDescending { it.date }

        val recentBest = if (best.size > amount) {
            best.subList(0, amount)
        } else {
            best
        }

        val fields = if (p.mode == GameMode.STANDARD) {
            recentBest
                    .map { ScoreInfo(analyse(it.beatmapID, it.maxCombo, it.hit300, it.hit100, it.hit50, it.misses, it.enabledMods), !it.rank.contains("F"), it) }
                    .filter { it.complete }
                    .map {
                        val map = it.score.beatmap.get()
                        val rank = Osu.prettyRank(it.score.rank)
                        val pp = it.pp.performance
                        val acc = it.pp.accuracy

                        val comboInfo = "${it.score.maxCombo}x/${map.maxCombo}x"
                        val hitInfo = "${it.score.hit300}/${it.score.hit100}/${it.score.hit50}/${it.score.misses}"
                        val playTitle = Osu.playTitle(it.score)

                        val title = "$rank | $playTitle"
                        val content = "${ppFormat.format(pp)} | ${accuracyFormat.format(acc)}, $comboInfo, $hitInfo"

                        MessageEmbed.Field(title, content, false)
                    }
        } else {
            recentBest
                    .map {
                        val rank = Osu.prettyRank(it.rank)
                        val pp = it.pp
                        val acc = getTaikoAcc(it.gekis, it.katus, it.misses)

                        val playTitle = Osu.playTitle(it)
                        val comboInfo = "${it.maxCombo}x"
                        val hitInfo = "${it.gekis}/${it.katus}/${it.misses}"

                        val title = "$rank | $playTitle"
                        val content = "${ppFormat.format(pp)} | ${accuracyFormat.format(acc)}, $comboInfo, $hitInfo"

                        MessageEmbed.Field(title, content, false)
                    }
        }

        val builder = EmbedBuilder()
                .setColor(data.getColor(guild))
                .setTitle("Best recent plays for $user in ${Osu.prettyMode(p.mode)}")

        fields.forEach { builder.addField(it) }

        channel.sendMessage(builder.build()).queue()
    }
}
