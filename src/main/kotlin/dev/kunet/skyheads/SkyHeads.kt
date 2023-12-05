package dev.kunet.skyheads

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.settings.PacketEventsSettings
import com.github.retrooper.packetevents.util.TimeStampMode
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.system.measureTimeMillis

class SkyHeads : JavaPlugin() {
    val headData = IdentityHashMap<Category, MutableList<HeadData>>()
    val tags = mutableSetOf<String>()
    val activePlayers = hashMapOf<Player, SkyData>()

    override fun onEnable() {
        val packetEvents = SpigotPacketEventsBuilder.build(
            this,
            PacketEventsSettings()
                .debug(false)
                .bStats(false)
                .checkForUpdates(false)
                .timeStampMode(TimeStampMode.MILLIS)
                .reEncodeByDefault(false)
        )
        PacketEvents.setAPI(packetEvents)
        packetEvents.init()

        val fetchTime = measureTimeMillis {
            Category.entries.forEach { fetch(it) }
        }
        logger.info("${headData.values.sumOf { it.size }} heads loaded with ${tags.size} tags")
        logger.info("finished loading in ${fetchTime}ms")
        val skyStateManager = SkyStateManager(this)

        getCommand("skyhead").executor = skyStateManager
        server.pluginManager.registerEvents(skyStateManager, this)

        packetEvents.eventManager.registerListener(skyStateManager)

        server.scheduler.scheduleSyncRepeatingTask(this, {
            for (activePlayer in activePlayers) {
                if (activePlayer.key.isFlying) continue
                activePlayers.remove(activePlayer.key)?.resetBack(activePlayer.key)
            }
        }, 0L, 1L)
    }

    override fun onDisable() {
        for (activePlayer in activePlayers) activePlayer.value.resetBack(activePlayer.key)
        activePlayers.clear()
    }

    private fun fetch(category: Category) {
        val timeTaken = measureTimeMillis {
            val name = category.data
            dataFolder.mkdirs()

            val file = File(dataFolder, "$name.json")
            if (!file.exists()) {
                logger.info("downloading ${category.data}.json...")
                val connection = URL("https://heads.pages.dev/archive/$name.json").openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "HeadDB/5.0.0-rc.8")
                connection.setRequestProperty("Accept", "application/json")

                val out = file.outputStream()
                val inputStream = connection.inputStream
                inputStream.copyTo(out)
                inputStream.close()
                out.close()
            }

            val jsonInput = file.readText()
            val heads = mutableListOf<HeadData>()
            jsonInput.parseJson().asJsonArray.map { it.asJsonObject }.forEach {
                val head = HeadData(
                    it.get("name").asString.replace('&', '+'),
                    it.get("uuid").asString,
                    category,
                    it.get("tags").asString.split(',').map { s -> s.lowercase().replace(' ', '_') },
                    it.get("value").asString,
                )

                tags.addAll(head.tags)

                heads += head
            }

            headData[category] = heads
        }

        logger.info("fetched ${category.data}.json in ${timeTaken}ms")
    }
}
