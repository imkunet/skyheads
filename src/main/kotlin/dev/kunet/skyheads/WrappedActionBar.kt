package dev.kunet.skyheads

import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import net.kyori.adventure.text.Component

class WrappedActionBar(private val text: String) : PacketWrapper<WrappedActionBar>(PacketType.Play.Server.CHAT_MESSAGE) {
    override fun write() {
        writeComponent(Component.text(text))
        writeByte(2)
    }
}