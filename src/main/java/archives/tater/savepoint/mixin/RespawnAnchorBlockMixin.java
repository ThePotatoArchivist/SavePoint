package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import org.objectweb.asm.Opcodes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = RespawnAnchorBlock.class)
public class RespawnAnchorBlockMixin {
    @ModifyReturnValue(
            method = "useWithoutItem",
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/world/InteractionResult;CONSUME:Lnet/minecraft/world/InteractionResult$Success;", opcode = Opcodes.GETSTATIC)
            ),
            at = @At(value = "RETURN", ordinal = 0)
    )
    private InteractionResult saveInventory(InteractionResult original, final BlockState state, final Level level, final BlockPos pos, final Player player) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return original;
        SavePoint.saveInventory(serverPlayer);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
