package mods.hexagon.thrusted.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientHooks.class)
public class ClientHooksMixin {

    @Inject(method = "isBlockEntityRendererVisible", at = @At("HEAD"), cancellable = true, remap = false)
    private static <T extends BlockEntity> void onIsBlockEntityRendererVisible(
            BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, Frustum frustum,
            CallbackInfoReturnable<Boolean> cir) {
        BlockEntityRenderer<T> renderer = (BlockEntityRenderer<T>) dispatcher.getRenderer(blockEntity);
        if (renderer != null && renderer.shouldRenderOffScreen((T) blockEntity)) {
            cir.setReturnValue(true);
        }
    }
}
