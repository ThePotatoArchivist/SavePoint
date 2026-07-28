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
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.portal.TeleportTransition
import eu.pb4.trinkets.api.TrinketDropRule
import eu.pb4.trinkets.api.event.TrinketDropCallback
import org.slf4j.LoggerFactory
import kotlin.math.min

object SavePoint : ModInitializer {
	const val MOD_ID = "savepoint"

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

    private val logger = LoggerFactory.getLogger(MOD_ID)

	val RESTORE_COMPARE_TAG: TagKey<DataComponentType<*>> = TagKey.create(Registries.DATA_COMPONENT_TYPE, id("restore_compare"))

	@JvmField
	val SAVE_STATE: AttachmentType<SaveState> = createAttachment(id("save_state")) {
		persistent(SaveState.CODEC)
		copyOnDeath()
	}

	@JvmField
	val SAVED_INVENTORY_DIRTY: AttachmentType<List<ItemStack>> = AttachmentRegistry.create(id("saved_inventory_dirty"))

	const val INVENTORY_SAVED_TEXT = "savepoint.inventory_saved"

	val TRINKETS_INSTALLED = FabricLoader.getInstance().isModLoaded("trinkets_updated")

	@JvmStatic
	fun saveInventory(player: ServerPlayer) {
		player[SAVE_STATE] = SaveState(
			player.inventory.toStream()
				.filter { !it.isEmpty }
                .map { it.copy() }
                .flatMap(::flatContents)
				.map { ItemStackTemplate.fromNonEmptyStack(it) }
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
			?.map { it.create() }
			?.also {
				player[SAVED_INVENTORY_DIRTY] = it
			}
	}

	fun stacksMatch(first: ItemStack, second: ItemStack): Boolean =
		ItemStack.isSameItemSameComponents(first, second) ||
		ItemStack.isSameItem(first, second) &&
				(first.components.keySet() + second.components.keySet()).all { !(it isIn RESTORE_COMPARE_TAG) || first[it] == second[it] }

	/**
	 * The stacks in `savedDirty` are mutated
	 * @return the amount dropped from the stack
	 */
	@JvmStatic
	fun getAmountDropped(stack: ItemStack, savedDirty: List<ItemStack>): Int {
		var amountDropped = stack.count
        for (savedStack in savedDirty) {
			if (amountDropped <= 0) break

			if (stacksMatch(stack, savedStack)) {
				val change = min(amountDropped, savedStack.count)
				savedStack.shrink(change)
				amountDropped -= change
			}
		}
		return amountDropped
	}

    /**
	 * Modifies stack
	 * @return the dropped stack, or null if the whole stack should be dropped
     */
    @JvmStatic
    fun processStack(stack: ItemStack, savedDirty: List<ItemStack>, drop: (ItemStack) -> ItemEntity?): ItemStack? {
		val amountDropped = getAmountDropped(stack, savedDirty)
		if (amountDropped >= stack.count) return null

		modifyContents(stack) { containedStack ->
			processAndDropStack(containedStack, savedDirty, drop)
			containedStack
		}

		return stack.split(amountDropped)
    }

	/**
	 * Modifies stack
	 * @return true if the stack was split, false if the whole stack should be dropped
	 */
	fun processAndDropStack(stack: ItemStack, savedDirty: List<ItemStack>, drop: (ItemStack) -> ItemEntity?): Boolean {
		val dropped = processStack(stack, savedDirty, drop) ?: return false
		drop(dropped)
		return true
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
		ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, alive ->
			if (alive || oldPlayer.level().gameRules.get(GameRules.KEEP_INVENTORY)) return@register

			newPlayer.inventory.replaceWith(oldPlayer.inventory)
			newPlayer.experienceLevel = getKeptXpLevels(oldPlayer)
			newPlayer.experienceProgress = oldPlayer[SAVE_STATE]?.experienceProgress?.coerceIn(0f, oldPlayer.experienceProgress) ?: 0f
		}
		if (TRINKETS_INSTALLED) {
			TrinketDropCallback.EVENT.register { rule, stack, _, entity ->
				if (entity !is ServerPlayer || rule != TrinketDropRule.DEFAULT) return@register rule
				val savedDirty = getDirtyOrSet(entity) ?: return@register TrinketDropRule.DEFAULT
				if (processAndDropStack(stack, savedDirty) { entity.drop(it, true, false) })
					TrinketDropRule.KEEP
				else
					TrinketDropRule.DEFAULT
			}
		}
	}
}
