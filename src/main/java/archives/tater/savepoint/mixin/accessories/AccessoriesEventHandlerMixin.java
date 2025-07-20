package archives.tater.savepoint.mixin.accessories;

import archives.tater.savepoint.SavePoint;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Debug(export = true)
@SuppressWarnings("UnstableApiUsage")
@Mixin(AccessoriesEventHandler.class)
public class AccessoriesEventHandlerMixin {
    @ModifyReturnValue(
            method = "dropStack",
            at = @At(value = "RETURN", ordinal = 1)
    )
    private static ItemStack keepItem(ItemStack original, @Local ItemStack stack) {
        return SavePoint.accessoriesKept ? stack : original;
    }
}
