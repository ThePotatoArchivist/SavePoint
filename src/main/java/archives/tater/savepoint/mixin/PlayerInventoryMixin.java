package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @ModifyArg(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;"),
            index = 0
    )
    private ItemStack processSaved(ItemStack stack, @Share("keptItem") LocalRef<ItemStack> keptItem) {
        var savedDirty = player.getAttached(SavePoint.SAVED_INVENTORY_DIRTY);
        if (savedDirty == null) {
            keptItem.set(ItemStack.EMPTY);
            return stack;
        }
        keptItem.set(stack.split(SavePoint.getAmountKept(stack, savedDirty)));
        return stack;
    }

    @SuppressWarnings("unchecked")
    @ModifyArg(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Ljava/util/List;set(ILjava/lang/Object;)Ljava/lang/Object;"),
            index = 1
    )
    private <E> E setKept(E e, @Share("keptItem") LocalRef<ItemStack> keptItem) {
        return (E) keptItem.get();
    }
}
