package dev.kunet.skyheads

import kotlin.math.abs
import kotlin.math.sqrt

class SkyVector(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun minus(other: SkyVector) = SkyVector(x - other.x, y - other.y, z - other.z)
    operator fun plus(other: SkyVector) = SkyVector(x + other.x, y + other.y, z + other.z)
    operator fun times(scalar: Float) = SkyVector(x * scalar, y * scalar, z * scalar)

    fun lengthSquared() = x * x + y * y + z * z
    fun length() = sqrt(lengthSquared())

    fun distance(other: SkyVector) = (this - other).length()
    fun distanceSquared(other: SkyVector) = (this - other).lengthSquared()

    fun dimensionsWithinRange(range: Float) = abs(x) <= range && abs(y) <= range && abs(z) <= range

    fun normalize(): SkyVector {
        val length = length()
        return SkyVector(x / length, y / length, z / length)
    }
}
