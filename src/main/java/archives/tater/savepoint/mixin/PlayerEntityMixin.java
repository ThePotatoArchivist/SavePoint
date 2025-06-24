package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("UnstableApiUsage")
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends Entity {
    public PlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyExpressionValue(
            method = "getXpToDrop",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;experienceLevel:I")
    )
    private int onlyDropRemainder(int original) {
        var saveState = getAttached(SavePoint.SAVE_STATE);
        if (saveState == null) return original;
        return original - SavePoint.getKeptXpLevels((PlayerEntity) (Object) this);
    }
}
