package dev.kunet.skyheads

import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.particle.data.LegacyParticleData
import com.github.retrooper.packetevents.wrapper.PacketWrapper

class WrappedParticle(
    val id: Int,
    val position: SkyVector,
    val offset: SkyVector,
) : PacketWrapper<WrappedParticle>(PacketType.Play.Server.PARTICLE) {
    override fun write() {
        writeInt(id)
        writeBoolean(false)
        writeFloat(position.x)
        writeFloat(position.y)
        writeFloat(position.z)
        writeFloat(offset.x)
        writeFloat(offset.y)
        writeFloat(offset.z)
        writeFloat(1f)
        writeInt(0)
        LegacyParticleData.write(this, id, LegacyParticleData.zero())
    }
}
