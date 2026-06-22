package mods.hexagon.thrusted.mixin;

import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import mods.hexagon.thrusted.space.SpaceDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityPropeller.class)
public interface PropellerSpaceMixin {

    @Inject(method = "getCurrentAirPressure", at = @At("HEAD"), cancellable = true, remap = false)
    default void onGetCurrentAirPressure(CallbackInfoReturnable<Double> cir) {
        BlockEntity self = (BlockEntity) this;
        Level level = self.getLevel();
        if (level != null) {
            var dim = level.dimension();
            if (SpaceDimensions.isOrbitDimension(dim) || SpaceDimensions.isPlanetDimension(dim)) {
                cir.setReturnValue(0.0);
            }
        }
    }
}
