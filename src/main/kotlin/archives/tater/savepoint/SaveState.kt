package archives.tater.savepoint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.item.ItemStack

@JvmRecord
data class SaveState(
    val items: List<ItemStack>,
) {
    companion object {
        val CODEC: Codec<SaveState> = RecordCodecBuilder.create { it.group(
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(SaveState::items),
        ).apply(it, ::SaveState) }
    }
}