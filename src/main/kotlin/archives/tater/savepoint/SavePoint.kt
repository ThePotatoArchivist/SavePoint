package archives.tater.savepoint

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.component.ComponentType
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.world.TeleportTarget
import io.wispforest.accessories.api.AccessoriesCapability
import io.wispforest.accessories.api.DropRule
import io.wispforest.accessories.api.events.OnDropCallback
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.math.min

@Suppress("UnstableApiUsage")
object SavePoint : ModInitializer {
	const val MOD_ID = "savepoint"

	fun id(path: String): Identifier = Identifier.of(MOD_ID, path)

    private val logger = LoggerFactory.getLogger(MOD_ID)

	val RESTORE_IGNORED_TAG: TagKey<ComponentType<*>> = TagKey.of(RegistryKeys.DATA_COMPONENT_TYPE, id("restore_ignored"))

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
	fun saveInventory(player: ServerPlayerEntity) {
		player[SAVE_STATE] = SaveState(
			Stream.concat(
				player.inventory.toIterable().toStream(),
				(if (!ACCESSORIES_INSTALLED) null else AccessoriesCapability.get(player)?.run { allEquipped.stream().map { it.stack } }) ?: Stream.empty()
			)
				.filter { !it.isEmpty }
                .map { it.copy() }
                .flatMap(::flatContents)
				.toList(),
			player.experienceLevel,
			player.experienceProgress,
		)

		player.sendMessage(Text.translatableWithFallback(INVENTORY_SAVED_TEXT, "Inventory Saved"))
	}

	@JvmStatic
	fun getDirtyOrSet(player: ServerPlayerEntity): List<ItemStack>? {
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
		ItemStack.areItemsAndComponentsEqual(first, second) ||
		ItemStack.areItemsEqual(first, second) &&
				(first.components.types + second.components.types).all { it isIn RESTORE_IGNORED_TAG || Registries.DATA_COMPONENT_TYPE.getId(it)!!.namespace != Identifier.DEFAULT_NAMESPACE || first[it] == second[it] }

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
	fun getKeptXpLevels(player: PlayerEntity) =
		player[SAVE_STATE]?.experienceLevel?.coerceIn(0, player.experienceLevel) ?: 0 // Player cannot gain xp by dying

	@JvmStatic
	fun checkSpawnpointMissing(player: ServerPlayerEntity) {
		if (player.getRespawnTarget(true /*don't consume respawn anchor charge*/, TeleportTarget.NO_OP).missingRespawnBlock)
			player.removeAttached(SAVE_STATE)
	}

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, _ ->
			newPlayer.inventory.clone(oldPlayer.inventory) // Make sure this doesn't cause problems
			newPlayer.experienceLevel = getKeptXpLevels(oldPlayer)
			newPlayer.experienceProgress = oldPlayer[SAVE_STATE]?.experienceProgress?.coerceIn(0f, oldPlayer.experienceProgress) ?: 0f
		}
		if (ACCESSORIES_INSTALLED) {
			OnDropCallback.EVENT.register { rule, stack, slotRef, _ ->
				if (rule != DropRule.DEFAULT) return@register rule
				val player = slotRef.entity() as? ServerPlayerEntity ?: return@register rule
				val savedDirty = getDirtyOrSet(player) ?: return@register rule
                slotRef.stack = processStack(stack, savedDirty) { stack -> player.dropItem(stack, true, false) }
				DropRule.KEEP
			}
		}
	}
}