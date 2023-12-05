package dev.kunet.skyheads
import net.minecraft.server.v1_8_R3.Item
import net.minecraft.server.v1_8_R3.ItemStack
import net.minecraft.server.v1_8_R3.NBTTagCompound
import net.minecraft.server.v1_8_R3.NBTTagList
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack
import java.util.*

private fun createBaseSkull() = ItemStack(Item.getById(397), 1, 3)
fun createNMSSkull(skin: String, uuid: String = UUID.randomUUID().toString()): org.bukkit.inventory.ItemStack {
    val value = NBTTagCompound()
    value.setString("Value", skin)

    val textures = NBTTagList()
    textures.add(value)

    val properties = NBTTagCompound()
    properties.set("textures", textures)

    val skullOwner = NBTTagCompound()
    skullOwner.setString("Id", uuid)
    skullOwner.set("Properties", properties)

    val itemCompound = NBTTagCompound()
    itemCompound.set("SkullOwner", skullOwner)

    val skull = createBaseSkull()
    skull.tag = itemCompound

    return CraftItemStack.asBukkitCopy(skull)
}
