package mods.hexagon.thrusted.space.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public class SpaceRenderHandler {

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        String dimPath = mc.level.dimension().location().getPath();

        if (dimPath.equals("space") || dimPath.startsWith("planet_")) {
            var stack = event.getPoseStack();
            var buffer = mc.renderBuffers().bufferSource();
            var camera = mc.gameRenderer.getMainCamera();
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

            if (dimPath.equals("space")) {
                SpaceSkyRenderer.renderSpaceSky(stack, buffer, camera, partialTick);
            } else {
                DimensionSunRenderer.renderDimensionSun(mc.level, stack, buffer, camera);
            }
        }
    }

    public static void onViewportRender(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        String dimPath = mc.level.dimension().location().getPath();

        if (dimPath.equals("space")) {
            event.setCanceled(true);
            event.setNearPlaneDistance(0.1f);
            event.setFarPlaneDistance(512f);
        } else if (dimPath.startsWith("planet_")) {
            event.setNearPlaneDistance(0.1f);
            event.setFarPlaneDistance(256f);
        }
    }
}
