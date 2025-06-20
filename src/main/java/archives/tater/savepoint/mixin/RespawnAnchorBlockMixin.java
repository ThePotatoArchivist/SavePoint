package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = RespawnAnchorBlock.class)
public class RespawnAnchorBlockMixin {
    @ModifyReturnValue(
            method = "onUse",
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/util/ActionResult;CONSUME:Lnet/minecraft/util/ActionResult;")
            ),
            at = @At(value = "RETURN", ordinal = 0)
    )
    private ActionResult saveInventory(ActionResult original, @Local(argsOnly = true) World world, @Local(argsOnly = true)BlockPos pos, @Local(argsOnly = true)PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer))
            return original;
        SavePoint.saveInventory(serverPlayer);
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return ActionResult.SUCCESS;
    }
}
