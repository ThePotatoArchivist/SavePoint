package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayer.RespawnConfig;
import net.minecraft.world.damagesource.DamageSource;

import org.jetbrains.annotations.Nullable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(
			method = "setRespawnPosition",
			at = @At("TAIL")
	)
	private void saveInventory(@Nullable RespawnConfig respawnConfig, boolean showMessage, CallbackInfo ci) {
		if (respawnConfig != null && showMessage)
			SavePoint.saveInventory((ServerPlayer) (Object) this);
	}


	@Inject(
			method = "die",
			at = @At("HEAD")
	)
	private void clearIfSpawnpointMissing(DamageSource source, CallbackInfo ci) {
		SavePoint.checkSpawnpointMissing((ServerPlayer) (Object) this);
	}
}