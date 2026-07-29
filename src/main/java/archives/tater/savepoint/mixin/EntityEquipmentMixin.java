package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

import static archives.tater.savepoint.UtilKt.getOrCreate;

@Mixin(EntityEquipment.class)
public abstract class EntityEquipmentMixin {
    @Unique
    private static final ScopedValue<Set<EquipmentSlot>> KEPT_SLOTS = ScopedValue.newInstance();

    @Shadow
    @Final
    private EnumMap<EquipmentSlot, ItemStack> items;

    @WrapOperation(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;")
    )
    private Iterator<ItemStack> saveKeys(Collection<ItemStack> instance, Operation<Iterator<ItemStack>> original, @Share("slot") LocalRef<@Nullable EquipmentSlot> slot) {
        var entryIterator = items.entrySet().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public ItemStack next() {
                var entry = entryIterator.next();
                slot.set(entry.getKey());
                return entry.getValue();
            }
        };
    }

    @WrapOperation(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"),
            order = 800
    )
    private @Nullable ItemEntity processSaved(LivingEntity instance, ItemStack itemStack, boolean randomly, boolean thrownFromHand, Operation<@Nullable ItemEntity> original, @Share("slot") LocalRef<@Nullable EquipmentSlot> slotRef, @Share("keptSlots") LocalRef<@Nullable Set<EquipmentSlot>> keptSlots) {
        var slot = slotRef.get();
        if (slot == null) return original.call(instance, itemStack, randomly, thrownFromHand);

        var savedDirty = instance.getAttached(SavePoint.SAVED_INVENTORY_DIRTY);
        if (savedDirty == null) return original.call(instance, itemStack, randomly, thrownFromHand);

        var dropped = SavePoint.processStack(itemStack, savedDirty, droppedStack -> original.call(instance, droppedStack, randomly, thrownFromHand));
        if (dropped == null) return original.call(instance, itemStack, randomly, thrownFromHand);

        getOrCreate(keptSlots, HashSet::new).add(slot);
        return original.call(instance, dropped, randomly, thrownFromHand);
    }

    @WrapOperation(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityEquipment;clear()V")
    )
    private void passKept(EntityEquipment instance, Operation<Void> original, @Share("keptSlots") LocalRef<@Nullable Set<EquipmentSlot>> keptSlots) {
        if (keptSlots.get() != null)
            ScopedValue.where(KEPT_SLOTS, keptSlots.get()).call(() -> original.call(instance));
        else
            original.call(instance);
    }

    @ModifyArg(
            method = "clear",
            at = @At(value = "INVOKE", target = "Ljava/util/EnumMap;replaceAll(Ljava/util/function/BiFunction;)V")
    )
    private BiFunction<EquipmentSlot, ItemStack, ItemStack> preventClear(BiFunction<EquipmentSlot, ItemStack, ItemStack> par1) {
        var keptSlots = KEPT_SLOTS.orElse(Set.of());
        return (slot, itemStack) -> keptSlots.contains(slot) ? itemStack : par1.apply(slot, itemStack);
    }
}
