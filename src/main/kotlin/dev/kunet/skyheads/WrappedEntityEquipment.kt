package dev.kunet.skyheads

import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.PacketWrapper

class WrappedEntityEquipment(
    val id: Int,
    val stack: ItemStack,
) : PacketWrapper<WrappedEntityEquipment>(PacketType.Play.Server.ENTITY_EQUIPMENT) {
    override fun write() {
        writeVarInt(id)
        writeShort(4)
        writeItemStack(stack)
    }
}
