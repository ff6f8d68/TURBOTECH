package mods.hexagon.thrusted;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

public class TurbofanBlockEntityRenderer implements BlockEntityRenderer<TurbofanBlockEntity> {

    public static final ResourceLocation FAN_MODEL_RL = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "block/turbofan_fan");
    public static final ModelResourceLocation FAN_MODEL_MRL = new ModelResourceLocation(FAN_MODEL_RL, "standalone");
    private static final ResourceLocation PLUME_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    private BakedModel cachedFanModel = null;
    private boolean loadAttempted = false;

    public TurbofanBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TurbofanBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (!loadAttempted) {
            loadAttempted = true;
            Minecraft mc = Minecraft.getInstance();
            cachedFanModel = mc.getModelManager().getModel(FAN_MODEL_MRL);
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(TurbofanBlock.FACING);
        float rotationAngle = blockEntity.getRotationAngle();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Base coordinate space mapping
        float yRot = 0f;
        switch (facing) {
            case NORTH -> yRot = 0f;
            case SOUTH -> yRot = 180f;
            case WEST -> yRot = 90f;
            case EAST -> yRot = 270f;
        }
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(yRot)));

        if (facing == Direction.UP) {
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-90)));
        } else if (facing == Direction.DOWN) {
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(90)));
        }

        // 1. RENDER TURBINE BLADES (Front / Local +Z)
        if (cachedFanModel != null) {
            poseStack.pushPose();
            poseStack.translate(0, 0, 0.005);
            poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(-rotationAngle)));
            poseStack.translate(-0.5, -0.5, -0.5);
            try {
                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());
                Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                        poseStack.last(), vertexConsumer, state, cachedFanModel,
                        1.0f, 1.0f, 1.0f, packedLight, packedOverlay, ModelData.EMPTY, RenderType.solid()
                );
            } catch (Exception e) {
                Thrusted.LOGGER.error("Error rendering fan model component", e);
            }
            poseStack.popPose();
        }

        // 2. RENDER EXHAUST JET PLUME (Back / Local -Z)
        double thrust = blockEntity.getThrust();
        float thrustRatio = (float) (thrust / TurbofanBlockEntity.MAX_THRUST_NEWTONS);

        if (thrustRatio > 0.02f) {
            poseStack.pushPose();
            poseStack.translate(0, 0, -0.505);
            renderExhaustPlume(poseStack, bufferSource, thrustRatio);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderExhaustPlume(PoseStack poseStack, MultiBufferSource bufferSource, float thrustRatio) {
        float plumeLength = thrustRatio * 5.0f * -1.0f; // Slightly longer for realistic bypass streams
        float baseRadius = 0.42f; // Wider plume for high-bypass look
        float tipRadius = 0.25f;  // Less tapered, more spread out stream

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(PLUME_TEXTURE, true));
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

            // FIX: Outer Air Blast Stream (Pure white with soft transparency)
            addPlumeVertex(buffer, poseStack, x1_b, y1_b, 0.0f, 240, 245, 255, 140, u1, textureScroll);
            addPlumeVertex(buffer, poseStack, x2_b, y2_b, 0.0f, 240, 245, 255, 140, u2, textureScroll);
            addPlumeVertex(buffer, poseStack, x2_t, y2_t, plumeLength, 220, 225, 240, 0, u2, textureScroll + 1.0f);
            addPlumeVertex(buffer, poseStack, x1_t, y1_t, plumeLength, 220, 225, 240, 0, u1, textureScroll + 1.0f);

            // FIX: Inner Core Flow (Thicker condense effect near exhaust nozzle)
            float innerLength = plumeLength * 0.6f;
            addPlumeVertex(buffer, poseStack, x1_b * 0.5f, y1_b * 0.5f, -0.001f, 255, 255, 255, 200, u1, textureScroll);
            addPlumeVertex(buffer, poseStack, x2_b * 0.5f, y2_b * 0.5f, -0.001f, 255, 255, 255, 200, u2, textureScroll);
            addPlumeVertex(buffer, poseStack, 0, 0, innerLength, 240, 245, 255, 0, u2, textureScroll + 0.5f);
            addPlumeVertex(buffer, poseStack, 0, 0, innerLength, 240, 245, 255, 0, u1, textureScroll + 0.5f);
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