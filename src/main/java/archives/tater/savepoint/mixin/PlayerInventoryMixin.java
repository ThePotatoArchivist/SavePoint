package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Shadow @Final public PlayerEntity player;

    @Inject(
            method = "dropAll",
            at = @At("HEAD")
    )
    private void copySavedItems(CallbackInfo ci, @Share("savedItems") LocalRef<List<ItemStack>> savedItems) {
        var saved = player.getAttached(SavePoint.SAVED_INVENTORY);
        if (saved == null || saved.isEmpty()) {
            savedItems.set(List.of());
            return;
        }
        savedItems.set(saved.stream().map(ItemStack::copy).toList());
    }

    @ModifyArg(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;"),
            index = 0
    )
    private ItemStack processSaved(ItemStack stack, @Share("savedItems") LocalRef<List<ItemStack>> savedItems, @Share("keptItem") LocalRef<ItemStack> keptItem) {
        keptItem.set(SavePoint.processKept(stack, savedItems.get()));
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
