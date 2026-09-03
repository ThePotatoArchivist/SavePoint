package archives.tater.savepoint.mixin;

import archives.tater.savepoint.SavePoint;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
	@WrapMethod(
			method = "setSpawnPoint"
	)
	private void saveInventory(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, Operation<Void> original) {
		original.call(dimension, pos, angle, forced, sendMessage);
		if (pos != null && sendMessage)
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