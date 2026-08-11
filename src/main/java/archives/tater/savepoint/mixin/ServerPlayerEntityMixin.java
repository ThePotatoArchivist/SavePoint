package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
	@Inject(
			method = "setSpawnPoint",
			at = @At("TAIL")
	)
	private void saveInventory(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, CallbackInfo ci) {
		if (pos != null && !SavePoint.NO_SAVE.get())
			SavePoint.saveInventory((ServerPlayerEntity) (Object) this);
	}

	@WrapOperation(
			method = "setSpawnPointFrom",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSpawnPoint(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/util/math/BlockPos;FZZ)V")
	)
	private void noSaveSetFrom(ServerPlayerEntity instance, RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, Operation<Void> original) {
		SavePoint.NO_SAVE.set(true);
		try {
			original.call(instance, dimension, pos, angle, forced, sendMessage);
		} finally {
			SavePoint.NO_SAVE.set(false);
		}
	}


	@Inject(
			method = "onDeath",
			at = @At("HEAD")
	)
	private void clearIfSpawnpointMissing(DamageSource damageSource, CallbackInfo ci) {
		SavePoint.checkSpawnpointMissing((ServerPlayerEntity) (Object) this);
	}
}