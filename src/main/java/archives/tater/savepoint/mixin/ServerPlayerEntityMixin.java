package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity.Respawn;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
	@Inject(
			method = "setSpawnPoint",
			at = @At("TAIL")
	)
	private void saveInventory(@Nullable Respawn respawn, boolean sendMessage, CallbackInfo ci) {
		if (respawn != null && sendMessage)
			SavePoint.saveInventory((ServerPlayerEntity) (Object) this);
	}


	@Inject(
			method = "onDeath",
			at = @At("HEAD")
	)
	private void clearIfSpawnpointMissing(DamageSource damageSource, CallbackInfo ci) {
		SavePoint.checkSpawnpointMissing((ServerPlayerEntity) (Object) this);
	}
}