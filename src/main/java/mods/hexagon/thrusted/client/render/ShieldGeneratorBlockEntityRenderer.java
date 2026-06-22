package mods.hexagon.thrusted.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.blockentity.ShieldGeneratorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

import java.util.ArrayDeque;

public class ShieldGeneratorBlockEntityRenderer implements BlockEntityRenderer<ShieldGeneratorBlockEntity> {

    public static final ResourceLocation TOP_MODEL_RL = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "block/shield_generator_top");
    public static final ModelResourceLocation TOP_MODEL_MRL = new ModelResourceLocation(TOP_MODEL_RL, "standalone");
    public static final ResourceLocation BASE_MODEL_RL = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "block/shield_generator_base");
    public static final ModelResourceLocation BASE_MODEL_MRL = new ModelResourceLocation(BASE_MODEL_RL, "standalone");
    public static final ResourceLocation TOP_EMISSIVE_MODEL_RL = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "block/shield_generator_top_emissive");
    public static final ModelResourceLocation TOP_EMISSIVE_MODEL_MRL = new ModelResourceLocation(TOP_EMISSIVE_MODEL_RL, "standalone");
    public static final ResourceLocation BASE_EMISSIVE_MODEL_RL = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "block/shield_generator_base_emissive");
    public static final ModelResourceLocation BASE_EMISSIVE_MODEL_MRL = new ModelResourceLocation(BASE_EMISSIVE_MODEL_RL, "standalone");

    private BakedModel cachedTopModel = null;
    private BakedModel cachedBaseModel = null;
    private BakedModel cachedTopEmissiveModel = null;
    private BakedModel cachedBaseEmissiveModel = null;
    private boolean loadAttempted = false;

    private static final int RIPPLE_INTERVAL = 3;
    private static final float TRI_GAP = 0.85f;

    private int prevGlowFace = -1;
    private int prevGlowTimer = 0;
    private int[] rippleTimers = null;
    private int rippleFaceCount = 0;
    private int[] bootupFadeTimers = null;
    private int prevBootupFaces = 0;
    private static final int BOOTUP_FADE_DURATION = 40;

    public ShieldGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public boolean shouldRenderOffScreen(ShieldGeneratorBlockEntity blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(ShieldGeneratorBlockEntity blockEntity, net.minecraft.world.phys.Vec3 cameraPos) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ShieldGeneratorBlockEntity blockEntity) {
        return blockEntity.getRenderBoundingBox();
    }

    @Override
    public void render(ShieldGeneratorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // Load both models on first render
        if (!loadAttempted) {
            loadAttempted = true;
            var mc = Minecraft.getInstance();
            var missing = mc.getModelManager().getMissingModel();
            cachedTopModel = mc.getModelManager().getModel(TOP_MODEL_MRL);
            if (cachedTopModel == null || cachedTopModel == missing) {
                Thrusted.LOGGER.warn("Shield generator top model not found: {}", TOP_MODEL_MRL);
            } else {
                Thrusted.LOGGER.debug("Shield generator top model loaded successfully");
            }
            cachedBaseModel = mc.getModelManager().getModel(BASE_MODEL_MRL);
            if (cachedBaseModel == null || cachedBaseModel == missing) {
                Thrusted.LOGGER.warn("Shield generator base model not found: {}", BASE_MODEL_MRL);
            } else {
                Thrusted.LOGGER.debug("Shield generator base model loaded successfully");
            }
            cachedBaseEmissiveModel = mc.getModelManager().getModel(BASE_EMISSIVE_MODEL_MRL);
            if (cachedBaseEmissiveModel == null || cachedBaseEmissiveModel == missing) {
                Thrusted.LOGGER.warn("Shield generator base emissive model not found: {}", BASE_EMISSIVE_MODEL_MRL);
            } else {
                Thrusted.LOGGER.debug("Shield generator base emissive model loaded successfully");
            }
            cachedTopEmissiveModel = mc.getModelManager().getModel(TOP_EMISSIVE_MODEL_MRL);
            if (cachedTopEmissiveModel == null || cachedTopEmissiveModel == missing) {
                Thrusted.LOGGER.warn("Shield generator top emissive model not found: {}", TOP_EMISSIVE_MODEL_MRL);
            } else {
                Thrusted.LOGGER.debug("Shield generator top emissive model loaded successfully");
            }
        }

        boolean active = blockEntity.isActive();
        int shieldColor = blockEntity.getShieldColor();
        float tintR = ((shieldColor >> 16) & 0xFF) / 255f;
        float tintG = ((shieldColor >> 8) & 0xFF) / 255f;
        float tintB = (shieldColor & 0xFF) / 255f;

        // Multiblock: non-controller parts are invisible; controller renders scaled-up model
        float mbSize = blockEntity.getMultiblockSize();
        boolean isMultiblock = mbSize > 1;
        boolean isPart = isMultiblock && !blockEntity.isController();

        // Pass 1: Normal render (packedLight, white tint)
        var mc = Minecraft.getInstance();
        var miss = mc.getModelManager().getMissingModel();
        if (!isPart && cachedBaseModel != null && cachedBaseModel != miss) {
            poseStack.pushPose();
            if (isMultiblock) poseStack.scale(mbSize, mbSize, mbSize);
            renderQuadsDirect(poseStack, bufferSource, cachedBaseModel, blockEntity, packedOverlay, packedLight, 1f, 1f, 1f, RenderType.solid());
            poseStack.popPose();
        }
        if (!isPart && cachedTopModel != null && cachedTopModel != miss) {
            poseStack.pushPose();
            if (isMultiblock) {
                poseStack.translate(mbSize * 0.5f, mbSize * 0.5f, mbSize * 0.5f);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(blockEntity.getTopRotation())));
                poseStack.translate(-mbSize * 0.5f, -mbSize * 0.5f, -mbSize * 0.5f);
                poseStack.scale(mbSize, mbSize, mbSize);
            } else {
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(blockEntity.getTopRotation())));
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }
            renderQuadsDirect(poseStack, bufferSource, cachedTopModel, blockEntity, packedOverlay, packedLight, 1f, 1f, 1f, RenderType.solid());
            poseStack.popPose();
        }
        // Pass 2: Additive glow overlay (only when glowing)
        if (!isPart && blockEntity.isGlowing()) {
            if (cachedBaseEmissiveModel != null && cachedBaseEmissiveModel != miss) {
                poseStack.pushPose();
                if (isMultiblock) poseStack.scale(mbSize, mbSize, mbSize);
                renderQuadsDirect(poseStack, bufferSource, cachedBaseEmissiveModel, blockEntity, packedOverlay, 0xF000F0, tintR, tintG, tintB, ThrustedRenderTypes.SOLID_ADDITIVE);
                poseStack.popPose();
            }
            if (cachedTopEmissiveModel != null && cachedTopEmissiveModel != miss) {
                poseStack.pushPose();
                if (isMultiblock) {
                    poseStack.translate(mbSize * 0.5f, mbSize * 0.5f, mbSize * 0.5f);
                    poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(blockEntity.getTopRotation())));
                    poseStack.translate(-mbSize * 0.5f, -mbSize * 0.5f, -mbSize * 0.5f);
                    poseStack.scale(mbSize, mbSize, mbSize);
                } else {
                    poseStack.translate(0.5f, 0.5f, 0.5f);
                    poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(blockEntity.getTopRotation())));
                    poseStack.translate(-0.5f, -0.5f, -0.5f);
                }
                renderQuadsDirect(poseStack, bufferSource, cachedTopEmissiveModel, blockEntity, packedOverlay, 0xF000F0, tintR, tintG, tintB, ThrustedRenderTypes.SOLID_ADDITIVE);
                poseStack.popPose();
            }
        }

        // 2. RENDER POWER BEAM (only controller, and only if centered on ship and beam enabled)
        if (active && blockEntity.isController() && blockEntity.isBeamEnabled() && beamShouldRender(blockEntity)) {
            renderBeam(poseStack, bufferSource, blockEntity, partialTick);
        }

        // 3. RENDER SHIELD DOME (only controller)
        if (!blockEntity.isController()) return;
        float alpha = blockEntity.getShieldAlpha();
        float bootup = blockEntity.getBootupProgress();
        if (alpha < 0.01f || bootup < 0.01f) return;

        float rx = (float) blockEntity.getEffectiveRadiusX();
        float ry = (float) blockEntity.getEffectiveRadiusY();
        float rz = (float) blockEntity.getEffectiveRadiusZ();

        var mesh = ThrustedIcosphere.getMesh(blockEntity.getSubdivLevel());

        poseStack.pushPose();

        float ox = (float) (blockEntity.getShieldCenterX() - blockEntity.getBlockPos().getX());
        float oy = (float) (blockEntity.getShieldCenterY() - blockEntity.getBlockPos().getY());
        float oz = (float) (blockEntity.getShieldCenterZ() - blockEntity.getBlockPos().getZ());
        poseStack.translate(ox, oy, oz);

        poseStack.translate(blockEntity.getOffX(), blockEntity.getOffY(), blockEntity.getOffZ());

        int activeFaces = (int) (bootup * mesh.faces.length);
        if (activeFaces < 1) {
            poseStack.popPose();
            return;
        }

        // Per-face glow ripple
        int glowFace = blockEntity.getGlowFaceIndex();
        int glowTimer = blockEntity.getGlowTimer();
        if (glowFace != prevGlowFace || glowTimer != prevGlowTimer) {
            prevGlowFace = glowFace;
            prevGlowTimer = glowTimer;
            rippleFaceCount = mesh.faces.length;
            rippleTimers = new int[rippleFaceCount];
            if (glowFace >= 0 && glowFace < rippleFaceCount && glowTimer > 0) {
                computeRipple(mesh, glowFace, glowTimer);
            }
        }
        if (rippleTimers != null && glowTimer <= 0 && prevGlowTimer <= 0) {
            boolean any = false;
            for (int i = 0; i < rippleFaceCount; i++) {
                if (rippleTimers[i] > 0) {
                    rippleTimers[i]--;
                    if (rippleTimers[i] > 0) any = true;
                }
            }
            if (!any) prevGlowFace = -1;
        }

        // Initialize fade timers
        if (bootupFadeTimers == null || bootupFadeTimers.length != mesh.faces.length) {
            bootupFadeTimers = new int[mesh.faces.length];
            prevBootupFaces = 0;
        }
        // Set fade timer for newly activated faces (fade in from 0 to BOOTUP_FADE_DURATION)
        if (activeFaces > prevBootupFaces) {
            for (int fi = prevBootupFaces; fi < activeFaces && fi < bootupFadeTimers.length; fi++) {
                bootupFadeTimers[fi] = 1; // start at 1 (just appeared)
            }
        }
        prevBootupFaces = activeFaces;
        // Advance all active fade timers each frame (cap at BOOTUP_FADE_DURATION)
        for (int fi = 0; fi < activeFaces && fi < bootupFadeTimers.length; fi++) {
            if (bootupFadeTimers[fi] < BOOTUP_FADE_DURATION) {
                bootupFadeTimers[fi]++;
            }
        }

        int color = blockEntity.getShieldColor();
        float cr = ((color >> 16) & 0xFF) / 255f;
        float cg = ((color >> 8) & 0xFF) / 255f;
        float cb = (color & 0xFF) / 255f;

        float brightness = 0.90f;
        float baseR = cr * brightness;
        float baseG = cg * brightness;
        float baseB = cb * brightness;
        float faceAlpha = alpha;

        float baseAlphaMul = blockEntity.getBaseAlpha() / 255f;
        faceAlpha *= baseAlphaMul;
        float edgeA = faceAlpha * 1.2f;

        var pose = poseStack.last();

        boolean whole = blockEntity.isWhole();
        float arcDegrees = blockEntity.getArcDegrees();
        float rotX = blockEntity.getRotX();
        float rotY = blockEntity.getRotY();
        float rotZ = blockEntity.getRotZ();
        boolean useRotation = rotX != 0 || rotY != 0 || rotZ != 0;

        float[] rotMat = useRotation ? buildRotationMatrix(rotX, rotY, rotZ) : null;

        // Precompute face data (skip culled faces)
        int count = activeFaces;
        float[] fx0 = new float[count], fy0 = new float[count], fz0 = new float[count];
        float[] fx1 = new float[count], fy1 = new float[count], fz1 = new float[count];
        float[] fx2 = new float[count], fy2 = new float[count], fz2 = new float[count];
        float[] n0x = new float[count], n0y = new float[count], n0z = new float[count];
        float[] n1x = new float[count], n1y = new float[count], n1z = new float[count];
        float[] n2x = new float[count], n2y = new float[count], n2z = new float[count];
        float[] rippleValues = new float[count];
        float[] fadeValues = new float[count];
        int validCount = 0;

        for (int fi = 0; fi < count; fi++) {
            int[] f = mesh.faces[fi];

            float uax = mesh.verts[f[0] * 3], uay = mesh.verts[f[0] * 3 + 1], uaz = mesh.verts[f[0] * 3 + 2];
            float ubx = mesh.verts[f[1] * 3], uby = mesh.verts[f[1] * 3 + 1], ubz = mesh.verts[f[1] * 3 + 2];
            float ucx = mesh.verts[f[2] * 3], ucy = mesh.verts[f[2] * 3 + 1], ucz = mesh.verts[f[2] * 3 + 2];

            // Arc culling: check angle from local +Y (accounting for rotation)
            if (!whole) {
                // Get face center direction in local unit space
                float rawX = (mesh.verts[f[0] * 3] + mesh.verts[f[1] * 3] + mesh.verts[f[2] * 3]) / 3f;
                float rawY = (mesh.verts[f[0] * 3 + 1] + mesh.verts[f[1] * 3 + 1] + mesh.verts[f[2] * 3 + 1]) / 3f;
                float rawZ = (mesh.verts[f[0] * 3 + 2] + mesh.verts[f[1] * 3 + 2] + mesh.verts[f[2] * 3 + 2]) / 3f;
                float len = (float) Math.sqrt(rawX * rawX + rawY * rawY + rawZ * rawZ);
                if (len > 0.0001f) {
                    rawX /= len; rawY /= len; rawZ /= len;
                }
                // Apply rotation to get the Y component in rotated space
                float yDir;
                if (useRotation) {
                    yDir = rawX * rotMat[3] + rawY * rotMat[4] + rawZ * rotMat[5];
                } else {
                    yDir = rawY;
                }
                float angle = (float) Math.toDegrees(Math.acos(Mth.clamp(yDir, -1f, 1f)));
                if (angle > arcDegrees / 2f) {
                    continue;
                }
            }

            // Rotate unit-sphere vertices
            float rax, ray, raz, rbx, rby, rbz, rcx, rcy, rcz;
            if (useRotation) {
                rax = uax * rotMat[0] + uay * rotMat[1] + uaz * rotMat[2];
                ray = uax * rotMat[3] + uay * rotMat[4] + uaz * rotMat[5];
                raz = uax * rotMat[6] + uay * rotMat[7] + uaz * rotMat[8];
                rbx = ubx * rotMat[0] + uby * rotMat[1] + ubz * rotMat[2];
                rby = ubx * rotMat[3] + uby * rotMat[4] + ubz * rotMat[5];
                rbz = ubx * rotMat[6] + uby * rotMat[7] + ubz * rotMat[8];
                rcx = ucx * rotMat[0] + ucy * rotMat[1] + ucz * rotMat[2];
                rcy = ucx * rotMat[3] + ucy * rotMat[4] + ucz * rotMat[5];
                rcz = ucx * rotMat[6] + ucy * rotMat[7] + ucz * rotMat[8];
            } else {
                rax = uax; ray = uay; raz = uaz;
                rbx = ubx; rby = uby; rbz = ubz;
                rcx = ucx; rcy = ucy; rcz = ucz;
            }

            // Ellipsoid surface positions
            float x0 = rax * rx, y0 = ray * ry, z0 = raz * rz;
            float x1 = rbx * rx, y1 = rby * ry, z1 = rbz * rz;
            float x2 = rcx * rx, y2 = rcy * ry, z2 = rcz * rz;

            // Shrink triangle toward its center to leave gaps between faces
            float cx = (x0 + x1 + x2) / 3f, cy = (y0 + y1 + y2) / 3f, cz = (z0 + z1 + z2) / 3f;
            x0 = cx + (x0 - cx) * TRI_GAP;
            y0 = cy + (y0 - cy) * TRI_GAP;
            z0 = cz + (z0 - cz) * TRI_GAP;
            x1 = cx + (x1 - cx) * TRI_GAP;
            y1 = cy + (y1 - cy) * TRI_GAP;
            z1 = cz + (z1 - cz) * TRI_GAP;
            x2 = cx + (x2 - cx) * TRI_GAP;
            y2 = cy + (y2 - cy) * TRI_GAP;
            z2 = cz + (z2 - cz) * TRI_GAP;

            int idx = validCount;
            fx0[idx] = x0; fy0[idx] = y0; fz0[idx] = z0;
            fx1[idx] = x1; fy1[idx] = y1; fz1[idx] = z1;
            fx2[idx] = x2; fy2[idx] = y2; fz2[idx] = z2;
            n0x[idx] = rax; n0y[idx] = ray; n0z[idx] = raz;
            n1x[idx] = rbx; n1y[idx] = rby; n1z[idx] = rbz;
            n2x[idx] = rcx; n2y[idx] = rcy; n2z[idx] = rcz;
            rippleValues[idx] = rippleTimers != null && fi < rippleFaceCount ? rippleTimers[fi] / 24f : 0f;
            fadeValues[idx] = bootupFadeTimers != null && fi < bootupFadeTimers.length ? Math.min(1f, bootupFadeTimers[fi] / (float) BOOTUP_FADE_DURATION) : 1f;
            validCount++;
        }

        // ... (Keep your precomputation loop where fx, fy, fz, n, etc. are calculated) ...

        // Pass 1: SHIELD (translucent base)
        VertexConsumer buf = bufferSource.getBuffer(ThrustedRenderTypes.SHIELD);
        for (int i = 0; i < validCount; i++) {
            float ripple = rippleValues[i];
            float fade = fadeValues[i];
            float baseFactor = 0.70f + ripple * 2.30f;
            float br = Math.min(1f, baseR * baseFactor);
            float bg = Math.min(1f, baseG * baseFactor);
            float bb = Math.min(1f, baseB * baseFactor);
            float ba = Math.min(1f, faceAlpha * (0.70f + ripple * 1.30f) * fade);
            buf.addVertex(pose, fx0[i], fy0[i], fz0[i]).setColor(br, bg, bb, ba);
            buf.addVertex(pose, fx1[i], fy1[i], fz1[i]).setColor(br, bg, bb, ba);
            buf.addVertex(pose, fx2[i], fy2[i], fz2[i]).setColor(br, bg, bb, ba);
        }

        // Pass 2: SHIELD_LINES — Fixed Normal Calculation
        VertexConsumer lineBuf = bufferSource.getBuffer(ThrustedRenderTypes.SHIELD_LINES);
        float lineEps = 0.003f;
        for (int i = 0; i < validCount; i++) {
            float ripple = rippleValues[i];
            float fade = fadeValues[i];
            float lr = Math.min(1f, baseR * (0.40f + ripple * 0.20f));
            float lg = Math.min(1f, baseG * (0.40f + ripple * 0.20f));
            float lb = Math.min(1f, baseB * (0.40f + ripple * 0.20f));
            float la = Math.min(1f, edgeA * (0.80f + ripple * 0.40f) * fade);

            // Edge Vectors for Line Normals (Fixes invisible lines)
            float dx01 = fx1[i] - fx0[i], dy01 = fy1[i] - fy0[i], dz01 = fz1[i] - fz0[i];
            float len01 = (float) Math.sqrt(dx01 * dx01 + dy01 * dy01 + dz01 * dz01);
            float nx01 = dx01 / len01, ny01 = dy01 / len01, nz01 = dz01 / len01;

            float dx12 = fx2[i] - fx1[i], dy12 = fy2[i] - fy1[i], dz12 = fz2[i] - fz1[i];
            float len12 = (float) Math.sqrt(dx12 * dx12 + dy12 * dy12 + dz12 * dz12);
            float nx12 = dx12 / len12, ny12 = dy12 / len12, nz12 = dz12 / len12;

            float dx20 = fx0[i] - fx2[i], dy20 = fy0[i] - fy2[i], dz20 = fz0[i] - fz2[i];
            float len20 = (float) Math.sqrt(dx20 * dx20 + dy20 * dy20 + dz20 * dz20);
            float nx20 = dx20 / len20, ny20 = dy20 / len20, nz20 = dz20 / len20;

            // Render Edges
            lineBuf.addVertex(pose, fx0[i] + n0x[i]*lineEps, fy0[i] + n0y[i]*lineEps, fz0[i] + n0z[i]*lineEps).setColor(lr, lg, lb, la).setNormal(pose, nx01, ny01, nz01);
            lineBuf.addVertex(pose, fx1[i] + n1x[i]*lineEps, fy1[i] + n1y[i]*lineEps, fz1[i] + n1z[i]*lineEps).setColor(lr, lg, lb, la).setNormal(pose, nx01, ny01, nz01);
            lineBuf.addVertex(pose, fx1[i] + n1x[i]*lineEps, fy1[i] + n1y[i]*lineEps, fz1[i] + n1z[i]*lineEps).setColor(lr, lg, lb, la).setNormal(pose, nx12, ny12, nz12);
            lineBuf.addVertex(pose, fx2[i] + n2x[i]*lineEps, fy2[i] + n2y[i]*lineEps, fz2[i] + n2z[i]*lineEps).setColor(lr, lg, lb, la).setNormal(pose, nx12, ny12, nz12);
            lineBuf.addVertex(pose, fx2[i] + n2x[i]*lineEps, fy2[i] + n2y[i]*lineEps, fz2[i] + n2z[i]*lineEps).setColor(lr, lg, lb, la).setNormal(pose, nx20, ny20, nz20);
            lineBuf.addVertex(pose, fx0[i] + n0x[i]*lineEps, fy0[i] + n0y[i]*lineEps, fz0[i] + n0z[i]*lineEps).setColor(lr, lg, lb, la).setNormal(pose, nx20, ny20, nz20);
        }

        // 4. FRIENDLY PLAYER NAMES (rendered in-world above players)
        if (blockEntity.showFriendlyNames() && blockEntity.isActive() && blockEntity.getShieldAlpha() > 0.01f) {
            mc = Minecraft.getInstance();
            var font = mc.font;
            var cam = mc.gameRenderer.getMainCamera();
            var level = blockEntity.getLevel();
            if (level != null) {
                double cx = blockEntity.getShieldCenterX() + blockEntity.getOffX();
                double cy = blockEntity.getShieldCenterY() + blockEntity.getOffY();
                double cz = blockEntity.getShieldCenterZ() + blockEntity.getOffZ();
                double srX = blockEntity.getEffectiveRadiusX();
                double srY = blockEntity.getEffectiveRadiusY();
                double srZ = blockEntity.getEffectiveRadiusZ();
                AABB nameArea = new AABB(
                    cx - srX - 2, cy - srY - 2, cz - srZ - 2,
                    cx + srX + 2, cy + srY + 2, cz + srZ + 2);
                for (var player : level.players()) {
                    if (!nameArea.contains(player.position())) continue;
                    if (player == mc.player) continue;
                    var nameComp = player.getDisplayName();
                    if (nameComp == null) nameComp = player.getName();
                    String name = nameComp.getString();
                    int halfWidth = font.width(name) / 2;
                    float scale = 0.025f;
                    poseStack.pushPose();
                    Vec3 playerPos = player.getPosition(partialTick);
                    poseStack.translate(
                        playerPos.x - blockEntity.getShieldCenterX() - blockEntity.getOffX(),
                        playerPos.y - blockEntity.getShieldCenterY() - blockEntity.getOffY() + player.getBbHeight() + 0.5,
                        playerPos.z - blockEntity.getShieldCenterZ() - blockEntity.getOffZ());
                    poseStack.mulPose(cam.rotation());
                    poseStack.scale(-scale, -scale, scale);
                    font.drawInBatch(nameComp, -halfWidth, 0, 0xFFFFFFFF, false,
                        poseStack.last().pose(), bufferSource,
                        net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0x0, packedLight);
                    font.drawInBatch(nameComp, -halfWidth + 1, 1, 0xAA3366D9, false,
                        poseStack.last().pose(), bufferSource,
                        net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0x0, packedLight);
                    poseStack.popPose();
                }
            }
        }

        // Pass 3: SHIELD_GLOW — steady ambient glow + hit ripple
        if (blockEntity.isGlowing()) {
            VertexConsumer glowBuf = bufferSource.getBuffer(ThrustedRenderTypes.SHIELD_GLOW);
            float subdivScale = 1.0f / (float)Math.sqrt(Math.max(1, blockEntity.getSubdivLevel()));
            float baseGlow = 0.25f * faceAlpha * subdivScale;
            for (int i = 0; i < validCount; i++) {
                float ripple = rippleValues[i];
                float fade = fadeValues[i];
                float glowIntensity = (baseGlow + ripple * 0.25f) * fade;
                if (glowIntensity > 0.01f) {
                    float gr = Math.min(1f, baseR * glowIntensity * 2.5f);
                    float gg = Math.min(1f, baseG * glowIntensity * 2.5f);
                    float gb = Math.min(1f, baseB * glowIntensity * 2.5f);
                    float eps = 0.001f;
                    glowBuf.addVertex(pose, fx0[i] + n0x[i]*eps, fy0[i] + n0y[i]*eps, fz0[i] + n0z[i]*eps).setColor(gr, gg, gb, glowIntensity);
                    glowBuf.addVertex(pose, fx1[i] + n1x[i]*eps, fy1[i] + n1y[i]*eps, fz1[i] + n1z[i]*eps).setColor(gr, gg, gb, glowIntensity);
                    glowBuf.addVertex(pose, fx2[i] + n2x[i]*eps, fy2[i] + n2y[i]*eps, fz2[i] + n2z[i]*eps).setColor(gr, gg, gb, glowIntensity);
                }
            }
        }

        poseStack.popPose();
    }

    private void renderQuadsDirect(PoseStack poseStack, MultiBufferSource bufferSource, BakedModel model,
                                    ShieldGeneratorBlockEntity blockEntity, int packedOverlay,
                                    int packedLight, float r, float g, float b, RenderType renderType) {
        var state = blockEntity.getBlockState();
        var random = RandomSource.create();
        var modelData = ModelData.EMPTY;
        var pose = poseStack.last();
        var vertConsumer = bufferSource.getBuffer(renderType);
        for (var direction : net.minecraft.core.Direction.values()) {
            for (var quad : model.getQuads(state, direction, random, modelData, RenderType.solid())) {
                vertConsumer.putBulkData(pose, quad, r, g, b, 1f, packedLight, packedOverlay);
            }
        }
        for (var quad : model.getQuads(state, null, random, modelData, RenderType.solid())) {
            vertConsumer.putBulkData(pose, quad, r, g, b, 1f, packedLight, packedOverlay);
        }
    }

    private void renderBeam(PoseStack poseStack, MultiBufferSource bufferSource, ShieldGeneratorBlockEntity be, float partialTick) {
        float mbSize = be.getMultiblockSize();
        float radius = (2f / 16f) * mbSize;
        float startY = mbSize > 1 ? 0f : 0.5f;
        float oy = (float) (be.getShieldCenterY() - be.getBlockPos().getY());
        float endY = oy + (float) be.getOffY() + (float) be.getEffectiveRadiusY();

        int color = be.getShieldColor();
        float cr = ((color >> 16) & 0xFF) / 255f;
        float cg = ((color >> 8) & 0xFF) / 255f;
        float cb = (color & 0xFF) / 255f;

        float alpha = be.getShieldAlpha() * 0.6f;
        alpha *= 1.0f;

        poseStack.pushPose();
        poseStack.translate(mbSize / 2f, 0f, mbSize / 2f);

        int segments = 8;
        var pose = poseStack.last();

        // Outer glow beam
        VertexConsumer buf = bufferSource.getBuffer(ThrustedRenderTypes.SHIELD_GLOW);
        float ba = Math.min(1f, alpha);
        for (int i = 0; i < segments; i++) {
            double a1 = i * 2.0 * Math.PI / segments;
            double a2 = (i + 1) * 2.0 * Math.PI / segments;

            float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
            float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);

            buf.addVertex(pose, c1 * radius, startY, s1 * radius).setColor(cr, cg, cb, ba);
            buf.addVertex(pose, c2 * radius, startY, s2 * radius).setColor(cr, cg, cb, ba);
            buf.addVertex(pose, c2 * radius, endY, s2 * radius).setColor(cr, cg, cb, ba);

            buf.addVertex(pose, c1 * radius, startY, s1 * radius).setColor(cr, cg, cb, ba);
            buf.addVertex(pose, c2 * radius, endY, s2 * radius).setColor(cr, cg, cb, ba);
            buf.addVertex(pose, c1 * radius, endY, s1 * radius).setColor(cr, cg, cb, ba);
        }

        // Bright white core
        float coreRadius = radius * 0.5f;
        buf = bufferSource.getBuffer(ThrustedRenderTypes.CELESTIAL_GLOW);
        float coreAlpha = Math.min(1f, alpha * 0.8f);
        for (int i = 0; i < segments; i++) {
            double a1 = i * 2.0 * Math.PI / segments;
            double a2 = (i + 1) * 2.0 * Math.PI / segments;

            float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
            float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);

            buf.addVertex(pose, c1 * coreRadius, startY, s1 * coreRadius).setColor(1f, 1f, 1f, coreAlpha);
            buf.addVertex(pose, c2 * coreRadius, startY, s2 * coreRadius).setColor(1f, 1f, 1f, coreAlpha);
            buf.addVertex(pose, c2 * coreRadius, endY, s2 * coreRadius).setColor(1f, 1f, 1f, coreAlpha);

            buf.addVertex(pose, c1 * coreRadius, startY, s1 * coreRadius).setColor(1f, 1f, 1f, coreAlpha);
            buf.addVertex(pose, c2 * coreRadius, endY, s2 * coreRadius).setColor(1f, 1f, 1f, coreAlpha);
            buf.addVertex(pose, c1 * coreRadius, endY, s1 * coreRadius).setColor(1f, 1f, 1f, coreAlpha);
        }

        poseStack.popPose();
    }

    private boolean beamShouldRender(ShieldGeneratorBlockEntity be) {
        if (!be.isOnShip()) return true;
        double mbSize = be.getMultiblockSize();
        double mcx = be.getBlockPos().getX() + mbSize / 2.0;
        double mcz = be.getBlockPos().getZ() + mbSize / 2.0;
        double scx = be.getShieldCenterX();
        double scz = be.getShieldCenterZ();
        double dist = Math.sqrt((scx - mcx) * (scx - mcx) + (scz - mcz) * (scz - mcz));
        return dist < mbSize * 0.5 + 2.0;
    }

    private void computeRipple(ThrustedIcosphere.MeshData mesh, int source, int duration) {
        rippleTimers = new int[mesh.faces.length];
        var queue = new ArrayDeque<int[]>();
        queue.add(new int[]{source, duration});
        rippleTimers[source] = duration;
        while (!queue.isEmpty()) {
            int[] entry = queue.poll();
            int fi = entry[0], t = entry[1];
            if (t <= 0) continue;
            int spread = t - RIPPLE_INTERVAL;
            if (spread <= 0) continue;
            for (int ni : mesh.faceNeighbors[fi]) {
                if (spread > rippleTimers[ni]) {
                    rippleTimers[ni] = spread;
                    queue.add(new int[]{ni, spread});
                }
            }
        }
    }

    private static float[] buildRotationMatrix(float rotX, float rotY, float rotZ) {
        float rx = (float) Math.toRadians(rotX);
        float ry = (float) Math.toRadians(rotY);
        float rz = (float) Math.toRadians(rotZ);
        float cx = (float) Math.cos(rx), sx = (float) Math.sin(rx);
        float cy = (float) Math.cos(ry), sy = (float) Math.sin(ry);
        float cz = (float) Math.cos(rz), sz = (float) Math.sin(rz);
        return new float[]{
                cy*cz,  sx*sy*cz - cx*sz,  cx*sy*cz + sx*sz,
                cy*sz,  sx*sy*sz + cx*cz,  cx*sy*sz - sx*cz,
                -sy,    sx*cy,              cx*cy
        };
    }
}
