package mods.hexagon.thrusted;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.joml.Quaternionf;

public class Raptor3BlockEntityRenderer implements BlockEntityRenderer<Raptor3BlockEntity> {

    public Raptor3BlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(Raptor3BlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();
        if (state.getValue(Raptor3Block.HALF) != DoubleBlockHalf.LOWER) return;

        Direction facing = state.getValue(Raptor3Block.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // --- EXPLICIT AXIS CHECKS TO PREVENT INVERSION ---
        if (facing == Direction.NORTH) {
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(0)));
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(90)));
        } else if (facing == Direction.SOUTH) {
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180)));
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(90)));
        } else if (facing == Direction.EAST) {
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(270)));
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(90)));
        } else if (facing == Direction.WEST) {
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90)));
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(90)));
        } else if (facing == Direction.UP) {
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(180)));
        } else if (facing == Direction.DOWN) {
            // Default model is DOWN
        }

        double thrust = blockEntity.getThrust();
        float thrustRatio = (float) (thrust / Raptor3BlockEntity.MAX_THRUST_NEWTONS);

        if (thrustRatio > 0.01f) {
            poseStack.translate(0, -0.4, 0); 
            renderRocketPlume(poseStack, bufferSource, thrustRatio);
        }

        poseStack.popPose();
    }

    private void renderRocketPlume(PoseStack poseStack, MultiBufferSource bufferSource, float thrustRatio) {
        // Longer plume to ensure a sharp point
        float plumeLength = thrustRatio * 8.0f; 
        float baseRadius = 0.45f;

        VertexConsumer buffer = bufferSource.getBuffer(ThrustedRenderTypes.PLUME);
        long gameTime = Minecraft.getInstance().level.getGameTime();
        float textureScroll = (gameTime % 15) / 15.0f;

        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * (2 * Math.PI)) / segments;
            double angle2 = ((i + 1) * (2 * Math.PI)) / segments;

            float x1_b = (float) (Math.cos(angle1) * baseRadius);
            float z1_b = (float) (Math.sin(angle1) * baseRadius);
            float x2_b = (float) (Math.cos(angle2) * baseRadius);
            float z2_b = (float) (Math.sin(angle2) * baseRadius);

            float u1 = (float) i / segments;
            float u2 = (float) (i + 1) / segments;

            // Outer Plume - SHARP POINTED CONE
            // Base vertices
            addPlumeVertex(buffer, poseStack, x1_b, 0, z1_b, 255, 200, 50, 200, u1, textureScroll);
            addPlumeVertex(buffer, poseStack, x2_b, 0, z2_b, 255, 200, 50, 200, u2, textureScroll);
            // Tip vertex (All quads meet at a single point for sharp tip)
            addPlumeVertex(buffer, poseStack, 0, -plumeLength, 0, 255, 120, 0, 0, u2, textureScroll + 1.0f);
            addPlumeVertex(buffer, poseStack, 0, -plumeLength, 0, 255, 120, 0, 0, u1, textureScroll + 1.0f);

            // Core Glow - SHARP POINTED CONE
            float coreLen = plumeLength * 0.85f;
            addPlumeVertex(buffer, poseStack, x1_b * 0.35f, 0, z1_b * 0.35f, 255, 255, 255, 255, u1, textureScroll);
            addPlumeVertex(buffer, poseStack, x2_b * 0.35f, 0, z2_b * 0.35f, 255, 255, 255, 255, u2, textureScroll);
            addPlumeVertex(buffer, poseStack, 0, -coreLen, 0, 255, 255, 180, 0, u2, textureScroll + 0.5f);
            addPlumeVertex(buffer, poseStack, 0, -coreLen, 0, 255, 255, 180, 0, u1, textureScroll + 0.5f);
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
