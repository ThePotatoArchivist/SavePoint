package archives.tater.savepoint

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import kotlin.math.min

@Suppress("UnstableApiUsage")
object SavePoint : ModInitializer {
	const val MOD_ID = "savepoint"

	fun id(path: String): Identifier = Identifier.of(MOD_ID, path)

    private val logger = LoggerFactory.getLogger(MOD_ID)

	val ignoredComponents = setOf(
		DataComponentTypes.DAMAGE,
		DataComponentTypes.BUNDLE_CONTENTS,
		DataComponentTypes.CHARGED_PROJECTILES,
		DataComponentTypes.MAP_DECORATIONS,
		DataComponentTypes.ENCHANTMENTS,
		DataComponentTypes.CUSTOM_NAME,
		DataComponentTypes.REPAIR_COST,
		DataComponentTypes.DYED_COLOR,
		DataComponentTypes.TRIM,
		DataComponentTypes.WRITABLE_BOOK_CONTENT,
	)

	@JvmField
	val SAVED_INVENTORY: AttachmentType<List<ItemStack>> = createAttachment(id("saved_inventory")) {
		persistent(ItemStack.CODEC.listOf().xmap({ it.toMutableList() }, { it }))
		copyOnDeath()
	}

	const val INVENTORY_SAVED_TEXT = "savepoint.inventory_saved"

	@JvmStatic
	fun saveInventory(player: ServerPlayerEntity) {
		player[SAVED_INVENTORY] = player.inventory
			.toIterable()
			.toStream()
			.filter { !it.isEmpty }
			.map { it.copy() }
			.toList()
		player.sendMessage(Text.translatable(INVENTORY_SAVED_TEXT))
	}

	fun stacksMatch(first: ItemStack, second: ItemStack): Boolean =
		ItemStack.areItemsAndComponentsEqual(first, second) ||
		ItemStack.areItemsEqual(first, second) &&
				(first.components.types + second.components.types).all { it in ignoredComponents || first[it] == second[it] }

	/**
	 * The stacks in `savedItems` are mutated, as well as `stack`
	 * @return the stack to keep in the inventory
	 */
	@JvmStatic
	fun processKept(stack: ItemStack, savedItems: List<ItemStack>): ItemStack {
		var amountToSave = stack.count
		val amountSaved = savedItems.sumOf { savedStack ->
			if (amountToSave == 0 || !stacksMatch(stack, savedStack)) 0
			else min(amountToSave, savedStack.count).also {
				savedStack.decrement(it)
				amountToSave -= it
			}
		}
		if (amountSaved == 0) return ItemStack.EMPTY
		return stack.split(amountSaved)
	}

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, _ ->
			newPlayer.inventory.clone(oldPlayer.inventory) // Make sure this doesn't cause problems
		}
	}
}