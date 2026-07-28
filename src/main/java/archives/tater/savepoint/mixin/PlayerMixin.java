package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import org.objectweb.asm.Opcodes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
public abstract class PlayerMixin extends Entity {
    public PlayerMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(
            method = "getBaseExperienceReward",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;experienceLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int onlyDropRemainder(int original) {
        var saveState = getAttached(SavePoint.SAVE_STATE);
        if (saveState == null) return original;
        return original - SavePoint.getKeptXpLevels((Player) (Object) this);
    }
}
