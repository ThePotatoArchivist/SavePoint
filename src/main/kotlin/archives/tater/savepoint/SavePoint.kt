package archives.tater.savepoint

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.portal.TeleportTransition
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import kotlin.math.min

object SavePoint : ModInitializer {
	const val MOD_ID = "savepoint"

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

    private val logger = LoggerFactory.getLogger(MOD_ID)

	val RESTORE_IGNORED_TAG: TagKey<DataComponentType<*>> = TagKey.create(Registries.DATA_COMPONENT_TYPE, id("restore_ignored"))

	@JvmField
	val SAVE_STATE: AttachmentType<SaveState> = createAttachment(id("save_state")) {
		persistent(SaveState.CODEC)
		copyOnDeath()
	}

	@JvmField
	val SAVED_INVENTORY_DIRTY: AttachmentType<List<ItemStack>> = AttachmentRegistry.create(id("saved_inventory_dirty"))

	const val INVENTORY_SAVED_TEXT = "savepoint.inventory_saved"

	val ACCESSORIES_INSTALLED = FabricLoader.getInstance().isModLoaded("accessories")

	@JvmStatic
	fun saveInventory(player: ServerPlayer) {
		player[SAVE_STATE] = SaveState(
			/*Stream.concat(*/
				player.inventory.toStream()/*,
				(if (!ACCESSORIES_INSTALLED) null else AccessoriesCapability.get(player)?.run { allEquipped.stream().map { it.stack } }) ?: Stream.empty()
			)*/
				.filter { !it.isEmpty }
                .map { it.copy() }
                .flatMap(::flatContents)
				.toList(),
			player.experienceLevel,
			player.experienceProgress,
		)

		player.sendSystemMessage(Component.translatableWithFallback(INVENTORY_SAVED_TEXT, "Inventory Saved"))
	}

	@JvmStatic
	fun getDirtyOrSet(player: ServerPlayer): List<ItemStack>? {
		player[SAVED_INVENTORY_DIRTY]?.let { return it }
		return player[SAVE_STATE]
			?.items
			?.takeUnless { it.isEmpty() }
			?.map(ItemStack::copy)
			?.also {
				player[SAVED_INVENTORY_DIRTY] = it
			}
	}

	fun stacksMatch(first: ItemStack, second: ItemStack): Boolean =
		ItemStack.isSameItemSameComponents(first, second) ||
		ItemStack.isSameItem(first, second) &&
				(first.components.keySet() + second.components.keySet()).all { it isIn RESTORE_IGNORED_TAG || first[it] == second[it] }

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
				savedStack.shrink(it)
				amountDropped -= it
			}
		}
	}

    @JvmStatic
    fun processStackResult(stack: ItemStack, savedDirty: List<ItemStack>, drop: (ItemStack) -> ItemEntity, result: Consumer<ItemStack>): ItemEntity {
        modifyContents(stack) { containedStack ->
            processStack(containedStack, savedDirty, drop)
        }
        result.accept(stack.split(getAmountKept(stack, savedDirty)))
        return drop(stack)
    }

    /**
     * @return the kept stack
     */
    @JvmStatic
    fun processStack(stack: ItemStack, savedDirty: List<ItemStack>, drop: (ItemStack) -> Any?): ItemStack {
        modifyContents(stack) { containedStack ->
            processStack(containedStack, savedDirty, drop)
        }
        return stack.split(getAmountKept(stack, savedDirty)).also {
            drop(stack)
        }
    }

	@JvmStatic
	fun getKeptXpLevels(player: Player) =
		player[SAVE_STATE]?.experienceLevel?.coerceIn(0, player.experienceLevel) ?: 0 // Player cannot gain xp by dying

	@JvmStatic
	fun checkSpawnpointMissing(player: ServerPlayer) {
		if (player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING).missingRespawnBlock)
			player.removeAttached(SAVE_STATE)
	}

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, _ ->
			newPlayer.inventory.replaceWith(oldPlayer.inventory) // Make sure this doesn't cause problems
			newPlayer.experienceLevel = getKeptXpLevels(oldPlayer)
			newPlayer.experienceProgress = oldPlayer[SAVE_STATE]?.experienceProgress?.coerceIn(0f, oldPlayer.experienceProgress) ?: 0f
		}
//		if (ACCESSORIES_INSTALLED) {
//			OnDropCallback.EVENT.register { rule, stack, slotRef, _ ->
//				if (rule != DropRule.DEFAULT) return@register rule
//				val player = slotRef.entity() as? ServerPlayer ?: return@register rule
//				val savedDirty = getDirtyOrSet(player) ?: return@register rule
//                slotRef.stack = processStack(stack, savedDirty) { stack -> player.drop(stack, true, false) }
//				DropRule.KEEP
//			}
//		}
	}
}
