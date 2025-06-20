@file:Suppress("UnstableApiUsage")

package archives.tater.savepoint

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.component.Component
import net.minecraft.component.ComponentType
import net.minecraft.util.Identifier

fun <T> createAttachment(id: Identifier, init: AttachmentRegistry.Builder<T>.() -> Unit): AttachmentType<T> =
    AttachmentRegistry.create(id) { it.init() }

operator fun <T> Component<T>.component1(): ComponentType<T> = type
operator fun <T> Component<T>.component2(): T = value