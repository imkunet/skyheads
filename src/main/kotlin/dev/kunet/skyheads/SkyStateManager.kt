package dev.kunet.skyheads

import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete
import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class SkyStateManager(val skyHeads: SkyHeads) : Listener, CommandExecutor, SimplePacketListenerAbstract() {
    override fun onCommand(
        sender: CommandSender,
        ignored: Command?,
        command: String?,
        args: Array<out String>?
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("you are the console")
            return true
        }

        if (!sender.isOp) {
            sender.sendMessage("&cNo permission!".colorCode())
            return true
        }

        if (skyHeads.activePlayers.contains(sender)) {
            sender.sendMessage("&cAlready in sky mode &7&o(TIP: Sneak to exit!)".colorCode())
            return true
        }

        val skyData = SkyData(
            skyHeads,
            sender.gameMode,
            sender.location,
            sender.allowFlight,
            sender.isFlying,
            sender.flySpeed
        )
        skyHeads.activePlayers[sender] = skyData
        skyData.start(sender)

        sender.sendMessage("&e&oEntering sky mode &7&o(TIP: Sneak to exit!)".colorCode())
        sender.send(
            " ".comp() + "&a[\u00AB PREV]".comp().setAction(ClickEvent.Action.RUN_COMMAND, "!prev") +
                    "  &a[NEXT \u00BB]  ".comp().setAction(ClickEvent.Action.RUN_COMMAND, "!next") +
                    "&c[EXIT]".comp().setAction(ClickEvent.Action.RUN_COMMAND, "!exit")
        )
        val location = sender.location
        location.y = 256.0
        location.yaw = 0f
        location.pitch = 0f

        sender.teleport(location)
        sender.gameMode = GameMode.CREATIVE
        sender.flySpeed = 0.001f
        sender.allowFlight = true
        sender.isFlying = true

        sender.playSound(sender.location, Sound.HORSE_SADDLE, 1f, 1.25f)

        return true
    }

    @EventHandler
    private fun onLeave(event: PlayerQuitEvent) {
        val data = skyHeads.activePlayers[event.player] ?: return
        data.resetBack(event.player)
    }

    override fun onPacketPlayReceive(event: PacketPlayReceiveEvent) {
        val data = skyHeads.activePlayers[event.player] ?: return
        val player = event.player as Player

        if (event.packetType == PacketType.Play.Client.ENTITY_ACTION) {
            val action = WrapperPlayClientEntityAction(event)
            if (action.action != WrapperPlayClientEntityAction.Action.START_SNEAKING) return

            skyHeads.activePlayers.remove(event.player) ?: return
            skyHeads.server.scheduler.runTask(skyHeads) { data.resetBack(player) }
            return
        }

        if (event.packetType == PacketType.Play.Client.TAB_COMPLETE) {
            val tabComplete = WrapperPlayClientTabComplete(event)
            if (tabComplete.text.startsWith('/')) return
            event.isCancelled = true

            data.updateQueryResults(player, tabComplete.text)
            return
        }

        if (event.packetType == PacketType.Play.Client.CHAT_MESSAGE) {
            val chatMessage = WrapperPlayClientChatMessage(event)
            if (chatMessage.message.startsWith('/')) return
            event.isCancelled = true

            data.updateQueryResults(player, chatMessage.message)
            return
        }
    }
}
