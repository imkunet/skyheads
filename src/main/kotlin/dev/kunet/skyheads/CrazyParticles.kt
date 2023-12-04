package dev.kunet.skyheads

import org.bukkit.entity.Player

data class BaseColor(val r: Boolean, val g: Boolean, val b: Boolean)

val baseColors = arrayOf(
    BaseColor(r = true, g = false, b = false),
    BaseColor(r = true, g = true, b = false),
    BaseColor(r = false, g = true, b = false),
    BaseColor(r = false, g = true, b = true),
    BaseColor(r = false, g = false, b = true),
    BaseColor(r = true, g = false, b = true),
)

private const val bigColor = 100000000f
private fun color(dimension: Boolean, variation: Float) = if (dimension) bigColor else variation

fun Player.sendColoredParticle(location: SkyVector, baseColor: BaseColor, variation: Float) {
    sendPacket(
        WrappedParticle(
            30,
            location,
            SkyVector(color(baseColor.r, variation), color(baseColor.g, variation), color(baseColor.b, variation)),
        )
    )
}
