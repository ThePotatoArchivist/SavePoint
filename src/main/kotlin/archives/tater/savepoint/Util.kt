@file:Suppress("UnstableApiUsage")

package archives.tater.savepoint

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import com.llamalad7.mixinextras.sugar.ref.LocalRef
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import java.util.stream.Stream
import java.util.stream.StreamSupport

fun <T: Any> createAttachment(id: Identifier, init: AttachmentRegistry.Builder<T>.() -> Unit): AttachmentType<T> =
    AttachmentRegistry.create(id) { it.init() }

operator fun <T: Any> TypedDataComponent<T>.component1(): DataComponentType<T> = type
operator fun <T: Any> TypedDataComponent<T>.component2(): T = value

fun <T> Iterable<T>.toStream(parallel: Boolean = false): Stream<T> = StreamSupport.stream(spliterator(), parallel)

fun <T> streamOf(): Stream<T> = Stream.empty()
fun <T> streamOf(value: T): Stream<T> = Stream.of(value)
fun <T> streamOf(vararg values: T): Stream<T> = Stream.of(*values)

operator fun <T: Any> AttachmentTarget.get(type: AttachmentType<T>) = getAttached(type)
operator fun <T: Any> AttachmentTarget.set(type: AttachmentType<T>, value: T?) = setAttached(type, value)

infix fun DataComponentType<*>.isIn(tag: TagKey<DataComponentType<*>>) =
    BuiltInRegistries.DATA_COMPONENT_TYPE.wrapAsHolder(this).`is`(tag)

fun <T: Any> ItemStack.reset(componentType: DataComponentType<T>): T? = set(componentType, item.components()[componentType])

fun <T: Any> LocalRef<T?>.getOrCreate(create: () -> T): T = get() ?: create().also { set(it) }