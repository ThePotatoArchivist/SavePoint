@file:Suppress("UnstableApiUsage")

package archives.tater.savepoint

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.component.Component
import net.minecraft.component.ComponentType
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.stream.Stream
import java.util.stream.StreamSupport

fun <T> createAttachment(id: Identifier, init: AttachmentRegistry.Builder<T>.() -> Unit): AttachmentType<T> =
    AttachmentRegistry.create(id) { it.init() }

operator fun <T> Component<T>.component1(): ComponentType<T> = type
operator fun <T> Component<T>.component2(): T = value

fun <T> Iterable<T>.toStream(parallel: Boolean = false): Stream<T> = StreamSupport.stream(spliterator(), parallel)

fun <T> streamOf(): Stream<T> = Stream.empty()
fun <T> streamOf(value: T): Stream<T> = Stream.of(value)
fun <T> streamOf(vararg values: T): Stream<T> = Stream.of(*values)

operator fun <T: Any> AttachmentTarget.get(type: AttachmentType<T>) = getAttached(type)
operator fun <T: Any> AttachmentTarget.set(type: AttachmentType<T>, value: T?) = setAttached(type, value)

infix fun ComponentType<*>.isIn(tag: TagKey<ComponentType<*>>) =
    Registries.DATA_COMPONENT_TYPE.getEntry(this).isIn(tag)

fun <T> ItemStack.reset(componentType: ComponentType<T>): T? = set(componentType, item.components[componentType])