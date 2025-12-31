package archives.tater.savepoint

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.BundleContentsComponent
import net.minecraft.component.type.ContainerComponent
import net.minecraft.item.ItemStack
import java.util.stream.Stream

fun removeContents(stack: ItemStack): Stream<ItemStack>? =
    stack.reset(DataComponentTypes.BUNDLE_CONTENTS)?.stream()
        ?: stack.reset(DataComponentTypes.CONTAINER)?.stream()

fun flatContents(stack: ItemStack): Stream<ItemStack> = removeContents(stack)
    .let { it ?: return streamOf(stack) }
    .flatMap(::flatContents)
    .filter { !it.isEmpty }
    .let { Stream.concat(it, streamOf(stack)) }

fun modifyContents(stack: ItemStack, transform: (ItemStack) -> ItemStack) {
    when {
        DataComponentTypes.BUNDLE_CONTENTS in stack ->
            stack.apply(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT) {
                BundleContentsComponent.Builder(BundleContentsComponent.DEFAULT).apply {
                    for (stack in it.iterate())
                        add(transform(stack))
                }.build()
            }
        DataComponentTypes.CONTAINER in stack ->
            stack.apply(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT) { container ->
                ContainerComponent.fromStacks(container.stream().map {
                    if (it.isEmpty) it else transform(it)
                }.toList())
            }
    }
}
