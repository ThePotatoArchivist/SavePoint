package archives.tater.savepoint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.item.ItemStack
import net.minecraft.util.collection.DefaultedList

private data class SlotStack(val slot: Int, val stack: ItemStack) {
    companion object {
        val CODEC: Codec<SlotStack> = RecordCodecBuilder.create { it.group(
            Codec.intRange(0, 255).fieldOf("slot").forGetter(SlotStack::slot),
            ItemStack.CODEC.fieldOf("stack").forGetter(SlotStack::stack)
        ).apply(it, ::SlotStack) }
    }
}

val STACK_LIST_CODEC: Codec<DefaultedList<ItemStack>> = SlotStack.CODEC.sizeLimitedListOf(256).xmap(
    { slots -> DefaultedList.ofSize(256, ItemStack.EMPTY).apply {
        for ((slot, stack) in slots)
            this[slot] = stack
    } },
    { list -> list.mapIndexedNotNull {
        index, stack -> if (stack.isEmpty) null else SlotStack(index, stack)
    } }
)