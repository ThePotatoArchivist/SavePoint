@file:Suppress("UnstableApiUsage")

package archives.tater.savepoint

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.component.Component
import net.minecraft.component.ComponentType
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

fun <T> createAttachment(id: Identifier, init: AttachmentRegistry.Builder<T>.() -> Unit): AttachmentType<T> =
    AttachmentRegistry.create(id) { it.init() }

operator fun <T> Component<T>.component1(): ComponentType<T> = type
operator fun <T> Component<T>.component2(): T = value

operator fun Inventory.iterator() = object : Iterator<ItemStack> {
    var slot = 0
    override fun hasNext(): Boolean = slot < this@iterator.size()
    override fun next(): ItemStack = getStack(slot++)
}

fun Inventory.toIterable() = object : Iterable<ItemStack> {
    override fun iterator(): Iterator<ItemStack> = this@toIterable.iterator()
}