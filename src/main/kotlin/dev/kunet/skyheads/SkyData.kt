package dev.kunet.skyheads

import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.atan2
import kotlin.math.min

const val RAD_TO_DEG = 180f / Math.PI.toFloat()

data class SkyData(
    val skyHeads: SkyHeads,

    val gameMode: GameMode,
    val location: Location,
    val flyAllowed: Boolean,
    val flying: Boolean,
    val flyingSpeed: Float
) {
    val gridOffset = 100000
    val guiOffset = 110000

    val width = 12
    val height = 9
    val dimensions = width * height

    val gapX = 0.7f
    val gapY = 0.7f

    private fun createArmorStand(
        player: Player,
        id: Int,
        offsetX: Float,
        offsetY: Float,
        offsetZ: Float,
        item: ItemStack? = null
    ) {
        val wrapperPlayServerSpawnEntity = WrapperPlayServerSpawnEntity(
            id,
            Optional.empty(),
            EntityTypes.ARMOR_STAND,
            Vector3d(location.x - offsetX, 256.0 + 1.62 - 1 + offsetY, location.z + offsetZ),
            0f,
            0f,
            0f,
            0,
            Optional.empty()
        )

        player.sendPacket(wrapperPlayServerSpawnEntity)

        val wrapperPlayServerEntityMetadata = WrapperPlayServerEntityMetadata(
            id, listOf(
                EntityData(0, EntityDataTypes.BYTE, 32.toByte()),
                //EntityData(2, EntityDataTypes.STRING, "hello world"),
                //EntityData(3, EntityDataTypes.BYTE, 1.toByte()),
                EntityData(10, EntityDataTypes.BYTE, 1.toByte()),
                EntityData(
                    11,
                    EntityDataTypes.VECTOR3F,
                    Vector3f(atan2(offsetY, offsetZ) * RAD_TO_DEG, atan2(-offsetX, -offsetZ) * RAD_TO_DEG, 0f)
                )
            )
        )

        player.sendPacket(wrapperPlayServerEntityMetadata)

        if (item != null) sendHead(player, id, item)
    }

    private fun sendHead(player: Player, id: Int, item: ItemStack? = null) {
        val entityEquipment = WrappedEntityEquipment(id, item ?: ItemStack.EMPTY)
        player.sendPacket(entityEquipment)
    }

    fun start(player: Player) {
        var id = gridOffset
        repeat(height) { y ->
            repeat(width) { x ->
                createArmorStand(
                    player,
                    id++,
                    (-width / 2f) * gapX + (gapX / 2) + x * gapX,
                    (height / 2f) * gapY + (gapY / 2) - y * gapY,
                    5.5f,
                    createSkull(QuestionHead)
                )
            }
        }
    }

    fun resetBack(player: Player) {
        if (Bukkit.isPrimaryThread()) resetBackReal(player)
        else skyHeads.server.scheduler.runTask(skyHeads) { resetBackReal(player) }
    }

    private fun resetBackReal(player: Player) {
        player.gameMode = gameMode
        player.teleport(location)
        player.allowFlight = flyAllowed
        player.isFlying = flying
        player.flySpeed = flyingSpeed

        player.sendMessage("&e&oExiting sky mode...".colorCode())
        player.playSound(player.location, Sound.HORSE_SADDLE, 1f, 0.75f)
    }

    var results = emptyList<HeadData>()
    var page = 0
    var query = ""

    private fun nextPage(player: Player) {
        if ((page + 1) * dimensions > results.size) {
            showingWhich(player, true)
            return
        }

        ++page
        showingWhich(player, false)
    }

    fun updateQueryResults(player: Player, query: String) {
        val q = query.trim().lowercase()

        if (q.startsWith("!")) {
            when (q) {
                "!next" -> {
                    nextPage(player)
                    return
                }

                "!prev" -> {
                    if (page == 0) {
                        showingWhich(player, true)
                        return
                    }

                    --page
                    showingWhich(player, false)
                    return
                }

                "!exit" -> {
                    resetBack(player)
                    skyHeads.activePlayers.remove(player)
                    return
                }
            }

            return
        }

        if (q == this.query) {
            nextPage(player)
            return
        }

        val results = skyHeads.headData.values.flatten().filter {
            val lower = it.name.lowercase()
            lower == q || lower.startsWith(q) || lower.contains(q)
        }

        player.sendActionBar("&b${results.size} &fresults for \"&b${q}&f\".")
        player.playSound(player.location, Sound.NOTE_STICKS, 1f, 0.75f)

        this.results = results
        this.page = 0
        this.query = q

        refreshScreen(player)
    }

    private fun showingWhich(player: Player, error: Boolean) {
        val pxd = page * dimensions
        player.sendActionBar(
            "Showing &b${pxd}&f-&b${
                min(
                    pxd + dimensions,
                    results.size
                )
            }&f for \"&b$query&f\" out of &b${results.size}&f results." + if (error) " &c\u26A0" else ""
        )

        player.playSound(
            player.location,
            if (error) Sound.NOTE_BASS else Sound.IRONGOLEM_THROW,
            1f,
            if (error) 0.75f else 1f
        )

        if (error) return

        refreshScreen(player)
    }

    private fun refreshScreen(player: Player) {
        val pxd = page * dimensions
        var id = gridOffset
        var d = 0

        repeat(dimensions) {
            sendHead(player, id++, results.getOrNull(pxd + d++)?.toPacketEventsItem() ?: createSkull(QuestionHead))
        }
    }
}
