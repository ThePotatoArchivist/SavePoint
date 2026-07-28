package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

@Mixin(EntityEquipment.class)
public abstract class EntityEquipmentMixin {

    @WrapOperation(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;")
    )
    private @Nullable ItemEntity processSaved(LivingEntity instance, ItemStack itemStack, boolean randomly, boolean thrownFromHand, Operation<@Nullable ItemEntity> original) {
        var savedDirty = instance.getAttached(SavePoint.SAVED_INVENTORY_DIRTY);
        if (savedDirty == null) return original.call(instance, itemStack, randomly, thrownFromHand);

        var dropped = SavePoint.processStack(itemStack, savedDirty, droppedStack -> original.call(instance, droppedStack, randomly, thrownFromHand));
        return original.call(instance, requireNonNullElse(dropped, itemStack), randomly, thrownFromHand);

    }

    @WrapWithCondition(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityEquipment;clear()V")
    )
    private boolean preventClear(EntityEquipment instance) {
        return false;
    }
}
