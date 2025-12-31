package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

@SuppressWarnings("UnstableApiUsage")
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Shadow @Final public PlayerEntity player;

    @Inject(
            method = "dropAll",
            at = @At("HEAD")
    )
    private void copySavedItems(CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer)
            SavePoint.getDirtyOrSet(serverPlayer);
    }

    @WrapOperation(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;")
    )
    private ItemEntity processSaved(PlayerEntity instance, ItemStack stack, boolean throwRandomly, boolean retainOwnership, Operation<ItemEntity> original, @Share("keptItem") LocalRef<ItemStack> keptItem) {
        var savedDirty = player.getAttached(SavePoint.SAVED_INVENTORY_DIRTY);
        if (savedDirty == null) {
            keptItem.set(ItemStack.EMPTY);
            return original.call(instance, stack, throwRandomly, retainOwnership);
        }
        return SavePoint.processStackResult(stack, savedDirty, droppedStack -> original.call(instance, droppedStack, throwRandomly, retainOwnership), keptItem::set);
    }

    @SuppressWarnings("unchecked")
    @ModifyArg(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;set(ILjava/lang/Object;)Ljava/lang/Object;"),
            index = 1
    )
    private <E> E setKept(E e, @Share("keptItem") LocalRef<ItemStack> keptItem) {
        return (E) keptItem.get();
    }
}
