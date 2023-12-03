package dev.kunet.skyheads

data class HeadData(
    val name: String,
    val uuid: String,
    val tags: String,
    val value: String,
) {
    fun toPacketEventsItem() = createSkull(value, uuid)
}
