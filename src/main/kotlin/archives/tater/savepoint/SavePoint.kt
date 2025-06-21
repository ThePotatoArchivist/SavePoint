package archives.tater.savepoint

import io.wispforest.accessories.api.AccessoriesCapability
import io.wispforest.accessories.api.DropRule
import io.wispforest.accessories.api.events.OnDropCallback
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.util.stream.Stream
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
		persistent(ItemStack.CODEC.listOf())
		copyOnDeath()
	}

	@JvmField
	val SAVED_INVENTORY_DIRTY: AttachmentType<List<ItemStack>> = AttachmentRegistry.create(id("saved_inventory_dirty"))

	const val INVENTORY_SAVED_TEXT = "savepoint.inventory_saved"

	val ACCESSORIES_INSTALLED = FabricLoader.getInstance().isModLoaded("accessories")

	@JvmStatic
	fun saveInventory(player: ServerPlayerEntity) {
		player[SAVED_INVENTORY] = Stream.concat(
			player.inventory.toIterable().toStream(),
			(if (!ACCESSORIES_INSTALLED) null else AccessoriesCapability.get(player)?.run { allEquipped.stream().map { it.stack } }) ?: Stream.empty()
		)
			.filter { !it.isEmpty }
			.map { it.copy() }
			.toList()

		player.sendMessage(Text.translatable(INVENTORY_SAVED_TEXT))
	}

	@JvmStatic
	fun getDirtyOrSet(player: ServerPlayerEntity): List<ItemStack>? {
		player[SAVED_INVENTORY_DIRTY]?.let { return it }
		return player[SAVED_INVENTORY]
			.takeUnless { it.isNullOrEmpty() }
			?.map(ItemStack::copy)
			?.also {
				player[SAVED_INVENTORY_DIRTY] = it
			}
	}

	fun stacksMatch(first: ItemStack, second: ItemStack): Boolean =
		ItemStack.areItemsAndComponentsEqual(first, second) ||
		ItemStack.areItemsEqual(first, second) &&
				(first.components.types + second.components.types).all { it in ignoredComponents || first[it] == second[it] }

	/**
	 * The stacks in `savedDirty` are mutated
	 * @return the amount kept
	 */
	@JvmStatic
	fun getAmountKept(stack: ItemStack, savedDirty: List<ItemStack>): Int {
		var amountDropped = stack.count
		return savedDirty.sumOf { savedStack ->
			if (amountDropped == 0 || !stacksMatch(stack, savedStack)) 0
			else min(amountDropped, savedStack.count).also {
				savedStack.decrement(it)
				amountDropped -= it
			}
		}
	}

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, _ ->
			newPlayer.inventory.clone(oldPlayer.inventory) // Make sure this doesn't cause problems
		}
		if (ACCESSORIES_INSTALLED) {
			OnDropCallback.EVENT.register { rule, _, slotRef, _ ->
				if (rule != DropRule.DEFAULT) return@register rule
				val stack = slotRef.stack ?: return@register rule
				val player = slotRef.entity() as? ServerPlayerEntity ?: return@register rule
				val savedDirty = getDirtyOrSet(player) ?: return@register rule
				val kept = getAmountKept(stack, savedDirty)
				if (kept == 0) return@register rule
				if (kept != stack.count)
					player.dropItem(stack.split(stack.count - kept), true, false)
				DropRule.KEEP
			}
		}
	}
}