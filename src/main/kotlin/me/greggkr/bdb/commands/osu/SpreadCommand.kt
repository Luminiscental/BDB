package me.greggkr.bdb.commands.osu

import com.oopsjpeg.osu4j.GameMode
import com.oopsjpeg.osu4j.backend.EndpointUserBests
import me.diax.comportment.jdacommand.Command
import me.diax.comportment.jdacommand.CommandDescription
import me.greggkr.bdb.data
import me.greggkr.bdb.osu
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import kotlin.math.sqrt

@CommandDescription(name = "spread", triggers = [
    "spread"
], description = "Displays the osu! pp spread of the user.")
class SpreadCommand : Command {
    override fun execute(message: Message, args: String) {
        val guild = message.guild
        val channel = message.channel

        val best = osu.userBests.getAsQuery(EndpointUserBests.ArgumentsBuilder(args)
                .setMode(GameMode.STANDARD)
                .setLimit(50)
                .build())
                .resolve()

        val sorted = best.asSequence().sortedByDescending { it.pp }.toList()

        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2].pp + sorted[(sorted.size / 2) - 1].pp) / 2
        } else {
            sorted[sorted.size / 2].pp
        }

        val mean = sorted.sumByDouble { it.pp.toDouble() }.div(sorted.size)
        val squared = sorted
                .asSequence()
                .map { Math.pow(it.pp - mean, 2.0) }
                .toList()
        val meanSquared = squared.sum().div(squared.size)
        val standardDeviation = sqrt(meanSquared)

        val skew = (6 * (mean - median)) / standardDeviation

        channel.sendMessage(EmbedBuilder()
                .setColor(data.getColor(guild))
                .setTitle("PP Spread for $args")
                .setDescription("Top play: [${sorted[0].pp}](https://osu.ppy.sh/b/${sorted[0].beatmapID})\n" +
                        "50th play: [${sorted[49]?.pp ?: "None"}](https://osu.ppy.sh/b/${sorted[49]?.beatmapID
                                ?: 0})\n" +
                        "Mean: $mean\n" +
                        "Median: $median\n" +
                        "Range: ${sorted[0].pp - sorted[49].pp}\n" +
                        "Standard Deviation: $standardDeviation\n" +
                        "Skew (coeff 6): $skew")
                .build())
                .queue()
    }
}