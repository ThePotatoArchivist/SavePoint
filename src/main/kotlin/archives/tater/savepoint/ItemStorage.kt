package archives.tater.savepoint

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.BundleContents
import net.minecraft.world.item.component.ChargedProjectiles
import net.minecraft.world.item.component.ItemContainerContents
import java.util.stream.Stream

fun removeContents(stack: ItemStack): Stream<ItemStack>? =
    stack.reset(DataComponents.BUNDLE_CONTENTS)?.itemCopyStream()
        ?: stack.reset(DataComponents.CONTAINER)?.allItemsCopyStream()

fun flatContents(stack: ItemStack): Stream<ItemStack> = removeContents(stack)
    .let { it ?: return streamOf(stack) }
    .flatMap(::flatContents)
    .filter { !it.isEmpty }
    .let { Stream.concat(it, streamOf(stack)) }

fun modifyContents(stack: ItemStack, transform: (ItemStack) -> ItemStack) {
    if (stack.has(DataComponents.BUNDLE_CONTENTS))
        stack.update(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY) { contents ->
            BundleContents.Mutable(BundleContents.EMPTY).apply {
                for (stack in contents.itemCopyStream())
                    transform(stack)
                        .takeIf { !it.isEmpty }
                        ?.let { tryInsert(it) }
            }.toImmutable()
        }

    if (stack.has(DataComponents.CONTAINER))
        stack.update(DataComponents.CONTAINER, ItemContainerContents.EMPTY) { container ->
            ItemContainerContents.fromItems(container.allItemsCopyStream().map {
                if (it.isEmpty) it else transform(it)
            }.toList())
        }

    if (stack.has(DataComponents.CHARGED_PROJECTILES))
        stack.update(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY) { projectiles ->
            ChargedProjectiles.ofNonEmpty(projectiles.itemCopies().map {
                transform(it)
            }.filter { !it.isEmpty })
        }
}
