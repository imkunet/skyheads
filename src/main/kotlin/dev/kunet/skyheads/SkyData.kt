package dev.kunet.skyheads

import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.particle.Particle
import com.github.retrooper.packetevents.protocol.particle.data.LegacyParticleData
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete.CommandMatch
import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.*

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
    val height = 7
    val dimensions = width * height

    val depth = 4.0f

    val gapX = 0.75f
    val gapY = 0.75f

    private val correspondingHeads = mutableListOf<SkyVector>()

    private fun createArmorStand(
        player: Player,
        id: Int,
        offset: SkyVector,
        item: ItemStack? = null
    ) {
        val wrapperPlayServerSpawnEntity = WrapperPlayServerSpawnEntity(
            id,
            Optional.empty(),
            EntityTypes.ARMOR_STAND,
            Vector3d(location.x + offset.x, 256.0 + 1.62 - 1 + offset.y, location.z + offset.z),
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
                EntityData(10, EntityDataTypes.BYTE, 17.toByte()),
                EntityData(
                    11,
                    EntityDataTypes.VECTOR3F,
                    Vector3f(atan2(offset.y, offset.z) * RAD_TO_DEG, atan2(offset.x, -offset.z) * RAD_TO_DEG, 0f)
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
        sendPrompt(player)

        var id = 0
        repeat(height) { y ->
            repeat(width) { x ->
                val offset = SkyVector(
                    (width / 2f) * gapX - (gapX / 2) - x * gapX,
                    (height / 2f) * gapY + (gapY / 2) - y * gapY - 1,
                    depth
                )
                correspondingHeads += offset + SkyVector(location.x.toFloat(), 256f + 1.62f, location.z.toFloat())
                createArmorStand(
                    player,
                    gridOffset + id++,
                    offset,
                    createSkull(QuestionHead)
                )
            }
        }
    }

    private fun sendPrompt(player: Player) {
        player.sendMessage("&e&oEntering sky mode &7&o(TIP: Sneak to exit!)".colorCode())
        var selection = " ".comp()
        var n = 0
        for (entry in Category.entries) {
            selection += "${getRainbowColor(n++)}[${entry.displayName}] ".comp()
                .setAction(ClickEvent.Action.SUGGEST_COMMAND, "!category ${entry.data}")
        }

        player.send(selection)
        player.send(
            " ".comp() + "&a[\u00AB PREV]".comp().setAction(ClickEvent.Action.RUN_COMMAND, "!prev") +
                    "  &a[NEXT \u00BB]  ".comp().setAction(ClickEvent.Action.RUN_COMMAND, "!next") +
                    "&c[EXIT]".comp().setAction(ClickEvent.Action.RUN_COMMAND, "!exit")
        )
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
        player.sendActionBar("&7Disengaged")
        player.playSound(player.location, Sound.HORSE_SADDLE, 1f, 0.75f)
    }

    var results = emptyList<HeadData>()
    var page = 0
    var query = ""

    private fun nextPage(player: Player) {
        if (dimensions >= results.size) {
            showingWhich(player, true)
            return
        }

        player.playSound(player.location, Sound.IRONGOLEM_THROW, 1f, 1.1f)
        if ((page + 1) * dimensions > results.size) {
            player.playSound(player.location, Sound.ENDERMAN_TELEPORT, 1f, 1.1f)
            page = 0
            showingWhich(player, false, " &b\u00BB &d\u267A")
        } else {
            ++page
            showingWhich(player, false, " &b\u00BB")
        }
    }

    private fun previousPage(player: Player) {
        if (dimensions >= results.size) {
            showingWhich(player, true)
            return
        }

        player.playSound(player.location, Sound.IRONGOLEM_THROW, 1f, 0.9f)
        if (page == 0) {
            player.playSound(player.location, Sound.ENDERMAN_TELEPORT, 1f, 0.9f)
            page = results.size / dimensions
            showingWhich(player, false, " &b\u00AB &d\u267A")
        } else {
            --page
            showingWhich(player, false, " &b\u00AB")
        }
    }

    fun updateQueryResults(player: Player, query: String) {
        val q = query.trim().lowercase()

        if (q == this.query || q == "" || q.endsWith('>')) {
            nextPage(player)
            return
        }

        if (q.endsWith('<')) {
            val originalQ = q.substring(0, q.length - 1)
            if (originalQ.trim() == this.query || q == "<") {
                previousPage(player)
                return
            }
        }

        if (q.startsWith("!")) {
            when (q) {
                "!next" -> return nextPage(player)
                "!prev" -> return previousPage(player)
                "!exit" -> {
                    resetBack(player)
                    skyHeads.activePlayers.remove(player)
                    return
                }
            }

            if (q.startsWith("!category ")) {
                val substring = q.substring("!category ".length)
                val category = Category.entries.firstOrNull { it.data.equals(substring, true) }
                if (category == null) {
                    player.sendActionBar("&cInvalid category.")
                    player.playSound(player.location, Sound.NOTE_BASS, 1f, 0.75f)
                    return
                }

                val results = skyHeads.headData[category] ?: return
                showResults(player, q, results)
                return
            }

            if (q.startsWith("!tag ")) {
                val substring = q.substring("!tag ".length)

                if (skyHeads.tags.contains(substring)) {
                    val results = skyHeads.headData.values.flatten().filter { it.tags.contains(substring) }
                    showResults(player, "!tag $substring", results)
                    return
                }

                val matches = skyHeads.tags.filter { it.startsWith(substring) }.toMutableList()
                skyHeads.tags.filter { it.contains(substring) }.forEach {
                    if (!matches.contains(it)) matches += it
                }
                if (matches.size == 1) {
                    val results = skyHeads.headData.values.flatten().filter { it.tags.contains(matches[0]) }
                    if (matches[0].length != substring.length) player.sendPacket(
                        WrapperPlayServerTabComplete(
                            0,
                            WrapperPlayServerTabComplete.CommandRange(0, 0),
                            listOf(CommandMatch(matches[0]))
                        )
                    )
                    showResults(player, "!tag " + matches[0], results)
                    return
                }

                player.sendPacket(
                    WrapperPlayServerTabComplete(
                        0,
                        WrapperPlayServerTabComplete.CommandRange(0, 0),
                        matches.map { CommandMatch(it) }
                    )
                )
                return
            }

            return
        }

        val results = skyHeads.headData.values.flatten().filter {
            val lower = it.name.lowercase()
            lower == q || lower.startsWith(q) || lower.contains(q)
        }

        showResults(player, q, results)
    }

    private fun showResults(player: Player, query: String, results: List<HeadData>) {
        player.sendActionBar("&b${results.size} &fresults for \"&b${query}&f\".")
        player.playSound(player.location, Sound.NOTE_STICKS, 1f, 0.75f)

        this.results = results
        this.page = 0
        this.query = query

        refreshScreen(player)
    }

    private fun showingWhich(player: Player, error: Boolean, extra: String? = null) {
        val pxd = page * dimensions
        player.sendActionBar(
            "Showing &b${pxd}&f-&b${
                min(
                    pxd + dimensions,
                    results.size
                )
            }&f for \"&b$query&f\" out of &b${results.size}&f results." + if (error) " &c\u26A0" else extra?.colorCode()
                ?: ""
        )

        if (error) return player.playSound(player.location, Sound.NOTE_BASS, 1f, 0.75f)
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

    private val flameParticle = Particle(ParticleTypes.FLAME, LegacyParticleData.zero())

    fun onPan(player: Player, rotation: SkyVector) {
        val minDepth = depth - 1.5f
        val longestSide = max(width, height) / 2 * max(gapX, gapY)
        val maxDepth = sqrt(minDepth * minDepth + longestSide * longestSide) + 1.5f

        var d = minDepth
        val hits = mutableMapOf<Int, Float>()
        val eyeVector =
            SkyVector(player.location.x.toFloat(), player.location.y.toFloat() + 1.62f, player.location.z.toFloat())
        while (d < maxDepth) {
            val test = eyeVector + rotation * d
            correspondingHeads.forEachIndexed { index, skyVector ->
                val difference = skyVector - test
                if (!difference.dimensionsWithinRange(0.3f)) return@forEachIndexed

                /*player.sendPacket(
                    WrapperPlayServerParticle(
                        Particle(ParticleTypes.FLAME, LegacyParticleData.zero()),
                        false,
                        Vector3d(test.x.toDouble(), test.y.toDouble(), test.z.toDouble()),
                        Vector3f.zero(),
                        0f,
                        1
                    )
                )*/

                val hit = hits[index]

                val length = difference.lengthSquared()
                if (hit != null) {
                    if (hit > length) hits[index] = length
                } else hits[index] = length
            }

            d += 0.05f
        }

        if (hits.isEmpty()) {
            pickingHead = -1
            return
        }

        var distance = Float.MAX_VALUE
        var key = 0

        for (hit in hits) {
            if (hit.value > distance) continue
            key = hit.key
            distance = hit.value
        }

        if (isHeadPick && pickingHead == key) {
            return
        }

        isHeadPick = true
        pickingHead = key

        val skyVector = correspondingHeads[key]

        val t = 8
        val radius = max(gapX, gapY) / 2
        repeat(2) {
            repeat(t) {
                val theta = 2 * Math.PI * (it / t.toDouble())
                val x = cos(theta) * radius
                val y = sin(theta) * radius

                player.sendPacket(
                    WrapperPlayServerParticle(
                        flameParticle,
                        false,
                        Vector3d(skyVector.x.toDouble() + x, skyVector.y.toDouble() + y, skyVector.z.toDouble()),
                        Vector3f.zero(),
                        0f,
                        0
                    )
                )
            }
        }
    }

    private var isHeadPick = false
    private var pickingHead = -1

    fun onClick(player: Player) {
        if (pickingHead == -1) return

        if (isHeadPick) {
            val skyVector = correspondingHeads[pickingHead]

            repeat(5) {
                repeat(24) { out ->
                    val radius = 0.4 + out / 8.0
                    repeat(baseColors.size) { baseColorIndex ->
                        val theta = 2 * Math.PI * (baseColorIndex / baseColors.size.toDouble()) + out * (Math.PI / 24.0)
                        val x = cos(theta) * radius
                        val y = sin(theta) * radius

                        player.sendColoredParticle(
                            skyVector + SkyVector(x.toFloat(), y.toFloat(), 0f),
                            baseColors[baseColorIndex],
                            1f
                        )
                    }
                }
            }

            return
        }
    }
}
