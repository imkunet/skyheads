package dev.kunet.skyheads

data class HeadData(
    val name: String,
    val uuid: String,
    val category: Category,
    val tags: List<String>,
    val value: String,
) {
    fun toPacketEventsItem() = createPESkull(value, uuid)
}
