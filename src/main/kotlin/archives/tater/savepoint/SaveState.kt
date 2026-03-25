package archives.tater.savepoint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.item.ItemStackTemplate

@JvmRecord
data class SaveState(
    val items: List<ItemStackTemplate>,
    val experienceLevel: Int,
    val experienceProgress: Float,
) {
    companion object {
        val CODEC: Codec<SaveState> = RecordCodecBuilder.create { it.group(
            ItemStackTemplate.CODEC.listOf().fieldOf("items").forGetter(SaveState::items),
            Codec.INT.fieldOf("experience_level").forGetter(SaveState::experienceLevel),
            Codec.FLOAT.fieldOf("experience_progress").forGetter(SaveState::experienceProgress),
        ).apply(it, ::SaveState) }
    }
}