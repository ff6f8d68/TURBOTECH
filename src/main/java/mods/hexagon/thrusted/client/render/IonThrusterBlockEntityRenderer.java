package mods.hexagon.thrusted.client.render;
import mods.hexagon.thrusted.Thrusted;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mods.hexagon.thrusted.block.IonThrusterBlock;
import mods.hexagon.thrusted.blockentity.IonThrusterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

public class IonThrusterBlockEntityRenderer implements BlockEntityRenderer<IonThrusterBlockEntity> {

    private static final ResourceLocation PLUME_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public IonThrusterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(IonThrusterBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(IonThrusterBlock.FACING);
        float rotationAngle = blockEntity.getRotationAngle();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // --- THE FIX: Replace the manual switch statement with Minecraft's built-in rotation helper ---
        float yRot = facing.toYRot();
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(yRot)));
        // ---------------------------------------------------------------------------------------------

        // Keep your vertical overrides exactly as they are, since facing.toYRot() returns 0f for UP/DOWN!
        if (facing == Direction.UP) {
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-90)));
        } else if (facing == Direction.DOWN) {
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(90)));
        }

        // RENDER BLUE EXHAUST PLUME
        double thrust = blockEntity.getThrust();
        float thrustRatio = (float) (thrust / IonThrusterBlockEntity.MAX_THRUST_NEWTONS);

        float emissionOffset = 0f;

        if (thrustRatio > 0.02f) {
            poseStack.pushPose();
            poseStack.translate(0, 0, emissionOffset);
            renderIonPlume(poseStack, bufferSource, thrustRatio);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderIonPlume(PoseStack poseStack, MultiBufferSource bufferSource, float thrustRatio) {
        // Shorter plume for ion thruster
        float plumeLength = thrustRatio * 3.0f * -1.0f;
        float baseRadius = 0.25f; // Smaller radius for ion thruster
        float tipRadius = 0.15f;  // Smaller tip

        // OLD: VertexConsumer buffer = bufferSource.getBuffer(ThrustedRenderTypes.PLUME(PLUME_TEXTURE));
// NEW: Just use the constant directly
        VertexConsumer buffer = bufferSource.getBuffer(ThrustedRenderTypes.PLUME);
        long gameTime = Minecraft.getInstance().level.getGameTime();
        float textureScroll = (gameTime % 20) / 20.0f;

        int segments = 12;
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * (2 * Math.PI)) / segments;
            double angle2 = ((i + 1) * (2 * Math.PI)) / segments;

            float x1_b = (float) (Math.cos(angle1) * baseRadius);
            float y1_b = (float) (Math.sin(angle1) * baseRadius);
            float x2_b = (float) (Math.cos(angle2) * baseRadius);
            float y2_b = (float) (Math.sin(angle2) * baseRadius);

            float x1_t = (float) (Math.cos(angle1) * tipRadius);
            float y1_t = (float) (Math.sin(angle1) * tipRadius);
            float x2_t = (float) (Math.cos(angle2) * tipRadius);
            float y2_t = (float) (Math.sin(angle2) * tipRadius);

            float u1 = (float) i / segments;
            float u2 = (float) (i + 1) / segments;

            // Blue inner flame only (ion thruster characteristic blue glow)
            float innerLength = plumeLength * 0.7f;
            addPlumeVertex(buffer, poseStack, x1_b * 0.6f, y1_b * 0.6f, -0.001f, 100, 150, 255, 220, u1, textureScroll);
            addPlumeVertex(buffer, poseStack, x2_b * 0.6f, y2_b * 0.6f, -0.001f, 100, 150, 255, 220, u2, textureScroll);
            addPlumeVertex(buffer, poseStack, 0, 0, innerLength, 80, 120, 255, 0, u2, textureScroll + 0.5f);
            addPlumeVertex(buffer, poseStack, 0, 0, innerLength, 80, 120, 255, 0, u1, textureScroll + 0.5f);

            // Add a brighter core
            addPlumeVertex(buffer, poseStack, x1_b * 0.3f, y1_b * 0.3f, -0.001f, 180, 200, 255, 240, u1, textureScroll);
            addPlumeVertex(buffer, poseStack, x2_b * 0.3f, y2_b * 0.3f, -0.001f, 180, 200, 255, 240, u2, textureScroll);
            addPlumeVertex(buffer, poseStack, 0, 0, innerLength * 0.8f, 150, 180, 255, 0, u2, textureScroll + 0.3f);
            addPlumeVertex(buffer, poseStack, 0, 0, innerLength * 0.8f, 150, 180, 255, 0, u1, textureScroll + 0.3f);


                   }
    }

    private void addPlumeVertex(VertexConsumer buffer, PoseStack poseStack, float x, float y, float z,
                                int r, int g, int b, int a, float u, float v) {
        buffer.addVertex(poseStack.last().pose(), x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(poseStack.last(), 0.0f, 1.0f, 0.0f);
    }
}
