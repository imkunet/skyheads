package dev.kunet.skyheads

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTList
import com.github.retrooper.packetevents.protocol.nbt.NBTString
import com.github.retrooper.packetevents.protocol.nbt.NBTType
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.google.gson.JsonElement
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.io.StringReader
import java.util.*
import kotlin.math.absoluteValue

fun String.parseJson(): JsonElement {
    val jsonReader = JsonReader(StringReader(this))
    jsonReader.isLenient = true
    return Streams.parse(jsonReader)
}

fun String.colorCode(): String = ChatColor.translateAlternateColorCodes('&', this)

val colorCodes = "c6eab9d".toCharArray().map { "&$it".colorCode() }
fun getRainbowColor(i: Int) = colorCodes[i.absoluteValue % colorCodes.size]

fun Player.sendPacket(packet: PacketWrapper<*>) = PacketEvents.getAPI().playerManager.sendPacket(this, packet)

fun Player.sendActionBar(bar: String) = player.sendPacket(WrappedActionBar(bar.colorCode()))

fun createSkull(skin: String, uuid: String = UUID.randomUUID().toString()): ItemStack {
    val value = NBTCompound()
    value.setTag("Value", NBTString(skin))

    val textures = NBTList(NBTType.COMPOUND)
    textures.addTag(value)

    val properties = NBTCompound()
    properties.setTag("textures", textures)

    val skullOwner = NBTCompound()
    skullOwner.setTag("Id", NBTString(uuid))
    skullOwner.setTag("Properties", properties)

    val itemCompound = NBTCompound()
    itemCompound.setTag("SkullOwner", skullOwner)

    return ItemStack.Builder().type(ItemTypes.SKELETON_SKULL).legacyData(3).nbt(itemCompound).build()
}
