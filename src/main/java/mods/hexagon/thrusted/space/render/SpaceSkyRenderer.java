package mods.hexagon.thrusted.space.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.client.render.ThrustedRenderTypes;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.OrbitManager;
import mods.hexagon.thrusted.space.api.CelestialBodyType;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class SpaceSkyRenderer extends DimensionSpecialEffects {
    public static final ResourceLocation SPACE_EFFECTS = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "space");

    private static final int STAR_COUNT = 8000;
    private static final int NEBULA_PARTICLE_COUNT = 500;
    private static final int MILKY_WAY_PARTICLES = 3000;

    public SpaceSkyRenderer() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix,
                              Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        setupFog.run();

        Vec3 camPos = camera.getPosition();
        Vector3d viewerPos = new Vector3d(camPos.x, camPos.y, camPos.z);

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        renderDeepSpaceBackground(modelViewMatrix);
        renderMilkyWay(modelViewMatrix, level, partialTick);
        renderNebulae(modelViewMatrix, level, partialTick);
        renderStars(modelViewMatrix, level, partialTick);
        renderCelestialBodies(modelViewMatrix, viewerPos, partialTick, level);

        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        return true;
    }

    private void renderDeepSpaceBackground(Matrix4f modelViewMatrix) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float dist = 900;
        int r = 2, g = 2, b = 8, a = 255;

        buffer.addVertex(modelViewMatrix, -dist, -dist, -dist).setColor(r, g, b, a);
        buffer.addVertex(modelViewMatrix, dist, -dist, -dist).setColor(r, g, b, a);
        buffer.addVertex(modelViewMatrix, dist, dist, -dist).setColor(r + 1, g + 1, b + 3, a);
        buffer.addVertex(modelViewMatrix, -dist, dist, -dist).setColor(r + 1, g + 1, b + 3, a);

        buffer.addVertex(modelViewMatrix, -dist, -dist, dist).setColor(r, g, b, a);
        buffer.addVertex(modelViewMatrix, dist, -dist, dist).setColor(r, g, b, a);
        buffer.addVertex(modelViewMatrix, dist, dist, dist).setColor(r + 1, g + 1, b + 3, a);
        buffer.addVertex(modelViewMatrix, -dist, dist, dist).setColor(r + 1, g + 1, b + 3, a);

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
    }

    private void renderMilkyWay(Matrix4f modelViewMatrix, ClientLevel level, float partialTick) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        SeededRandom rng = new SeededRandom(777L);

        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        for (int i = 0; i < MILKY_WAY_PARTICLES; i++) {
            double bandAngle = rng.nextDouble() * 2.0 * Math.PI;
            double bandWidth = (rng.nextGaussian() * 0.15);
            double dist = 700.0 + rng.nextDouble() * 200.0;

            float x = (float) (dist * Math.cos(bandAngle));
            float y = (float) (dist * bandWidth);
            float z = (float) (dist * Math.sin(bandAngle));

            float brightness = 0.1f + rng.nextFloat() * 0.15f;
            int r = (int) (brightness * 220);
            int g = (int) (brightness * 200);
            int b = (int) (brightness * 180);
            int alpha = (int) (brightness * 80);

            float s = 1.5f + rng.nextFloat() * 2.5f;
            buffer.addVertex(modelViewMatrix, x - s, y - s, z).setColor(r, g, b, alpha);
            buffer.addVertex(modelViewMatrix, x + s, y - s, z).setColor(r, g, b, alpha);
            buffer.addVertex(modelViewMatrix, x + s, y + s, z).setColor(r, g, b, alpha);
            buffer.addVertex(modelViewMatrix, x - s, y + s, z).setColor(r, g, b, alpha);
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
        RenderSystem.defaultBlendFunc();
    }

    private void renderNebulae(Matrix4f modelViewMatrix, ClientLevel level, float partialTick) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        SeededRandom rng = new SeededRandom(1337L);

        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        int[][] nebulaColors = {
                {80, 20, 120},
                {20, 60, 140},
                {140, 40, 60},
                {30, 100, 80},
                {100, 80, 20}
        };

        for (int n = 0; n < 5; n++) {
            double centerTheta = rng.nextDouble() * 2.0 * Math.PI;
            double centerPhi = Math.acos(2.0 * rng.nextDouble() - 1.0);
            double centerDist = 600.0;
            int[] col = nebulaColors[n];

            for (int i = 0; i < NEBULA_PARTICLE_COUNT / 5; i++) {
                double offsetTheta = centerTheta + rng.nextGaussian() * 0.3;
                double offsetPhi = centerPhi + rng.nextGaussian() * 0.2;
                double dist = centerDist + rng.nextGaussian() * 50;

                float x = (float) (dist * Math.sin(offsetPhi) * Math.cos(offsetTheta));
                float y = (float) (dist * Math.cos(offsetPhi));
                float z = (float) (dist * Math.sin(offsetPhi) * Math.sin(offsetTheta));

                float pulse = 0.8f + 0.2f * (float) Math.sin(level.getGameTime() * 0.0005 + n);
                int alpha = (int) (pulse * (15 + rng.nextFloat() * 20));

                float s = 4.0f + rng.nextFloat() * 8.0f;
                buffer.addVertex(modelViewMatrix, x - s, y - s, z).setColor(col[0], col[1], col[2], alpha);
                buffer.addVertex(modelViewMatrix, x + s, y - s, z).setColor(col[0], col[1], col[2], alpha);
                buffer.addVertex(modelViewMatrix, x + s, y + s, z).setColor(col[0], col[1], col[2], alpha);
                buffer.addVertex(modelViewMatrix, x - s, y + s, z).setColor(col[0], col[1], col[2], alpha);
            }
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
        RenderSystem.defaultBlendFunc();
    }

    private void renderStars(Matrix4f modelViewMatrix, ClientLevel level, float partialTick) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        long seed = 42L;
        SeededRandom rng = new SeededRandom(seed);
        float twinkleBase = 0.7f + 0.3f * (float) Math.sin(level.getGameTime() * 0.002);

        int[][] starColors = {
                {255, 220, 180},
                {180, 200, 255},
                {255, 200, 150},
                {255, 180, 180},
                {200, 220, 255},
                {255, 255, 200},
                {255, 150, 100},
                {150, 180, 255}
        };

        for (int i = 0; i < STAR_COUNT; i++) {
            double theta = rng.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * rng.nextDouble() - 1.0);
            double dist = 500.0 + rng.nextDouble() * 400.0;

            float x = (float) (dist * Math.sin(phi) * Math.cos(theta));
            float y = (float) (dist * Math.cos(phi));
            float z = (float) (dist * Math.sin(phi) * Math.sin(theta));

            int colorIdx = (int) (rng.nextDouble() * starColors.length);
            int[] col = starColors[colorIdx];

            float magnitude = rng.nextFloat();
            float twinkle = twinkleBase * (0.5f + rng.nextFloat() * 0.5f);
            float twinkleOffset = (float) Math.sin(level.getGameTime() * 0.01 * (1 + i % 7) + i);
            float finalBright = twinkle * (0.6f + 0.4f * twinkleOffset);
            finalBright = Math.max(0.1f, Math.min(1.0f, finalBright));

            int r = (int) (col[0] * finalBright);
            int g = (int) (col[1] * finalBright);
            int b = (int) (col[2] * finalBright);
            int alpha = (int) (finalBright * 255);

            float s = 0.3f + magnitude * 0.8f;
            buffer.addVertex(modelViewMatrix, x - s, y - s, z).setColor(r, g, b, alpha);
            buffer.addVertex(modelViewMatrix, x + s, y - s, z).setColor(r, g, b, alpha);
            buffer.addVertex(modelViewMatrix, x + s, y + s, z).setColor(r, g, b, alpha);
            buffer.addVertex(modelViewMatrix, x - s, y + s, z).setColor(r, g, b, alpha);
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
    }

    private void renderCelestialBodies(Matrix4f modelViewMatrix, Vector3d viewerPos, float partialTick, ClientLevel level) {
        for (CelestialBody body : CelestialBodyRegistry.getAll()) {
            if ("Sun".equals(body.getName())) {
                renderSun(modelViewMatrix, body, viewerPos, partialTick, level);
            } else {
                CelestialBodyMesh.renderBody(modelViewMatrix, body, viewerPos, partialTick,
                        ThrustedRenderTypes.CELESTIAL_SOLID);
                CelestialBodyMesh.renderBody(modelViewMatrix, body, viewerPos, partialTick,
                        ThrustedRenderTypes.CELESTIAL_GLOW);

                if (body.hasRings()) {
                    renderRings(modelViewMatrix, body, viewerPos);
                }

                if (body.getAtmosphere().hasAtmosphere() && body.getType() != CelestialBodyType.GAS_GIANT) {
                    renderAtmosphericGlow(modelViewMatrix, body, viewerPos);
                }
            }
        }
    }

    private void renderSun(Matrix4f modelViewMatrix, CelestialBody sun, Vector3d viewerPos,
                           float partialTick, ClientLevel level) {
        CelestialBodyMesh.renderBody(modelViewMatrix, sun, viewerPos, partialTick,
                ThrustedRenderTypes.CELESTIAL_SOLID);

        Vector3d sunPos = OrbitManager.getPosition(sun.getName());
        double dx = sunPos.x - viewerPos.x;
        double dy = sunPos.y - viewerPos.y;
        double dz = sunPos.z - viewerPos.z;

        Matrix4f mat = new Matrix4f(modelViewMatrix).translate((float) dx, (float) dy, (float) dz);
        double radius = sun.getOrbitData().radius();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        float pulse = 0.9f + 0.1f * (float) Math.sin(level.getGameTime() * 0.05);

        for (int layer = 0; layer < 4; layer++) {
            float glowScale = (float) (radius * (2.0 + layer * 1.5)) * pulse;
            Matrix4f layerMat = new Matrix4f(mat).scale(glowScale, glowScale, glowScale);
            float alpha = 0.3f / (1 + layer);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            float r = 1.0f;
            float g = 0.85f - layer * 0.1f;
            float b = 0.2f - layer * 0.05f;

            buffer.addVertex(layerMat, -1, -1, 0).setColor(r, g, b, alpha);
            buffer.addVertex(layerMat, -1, 1, 0).setColor(r, g, b, alpha);
            buffer.addVertex(layerMat, 1, 1, 0).setColor(r, g, b, alpha);
            buffer.addVertex(layerMat, 1, -1, 0).setColor(r, g, b, alpha);

            var rendered = buffer.build();
            if (rendered != null) {
                BufferUploader.draw(rendered);
            }
        }

        renderSolarCorona(mat, radius, level);

        RenderSystem.defaultBlendFunc();
    }

    private void renderSolarCorona(Matrix4f baseMat, double radius, ClientLevel level) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        SeededRandom rng = new SeededRandom(99L);
        int streamCount = 24;
        float time = level.getGameTime() * 0.01f;

        for (int i = 0; i < streamCount; i++) {
            float angle = (float) (2.0 * Math.PI * i / streamCount) + time * 0.1f;
            float length = (float) (radius * (3.0 + rng.nextFloat() * 4.0));
            float width = (float) (radius * 0.15f);

            float wobble = (float) Math.sin(time + i * 0.5) * 0.3f;
            float adjustedAngle = angle + wobble;

            float x1 = (float) (radius * 0.8 * Math.cos(adjustedAngle));
            float y1 = (float) (radius * 0.8 * Math.sin(adjustedAngle));
            float x2 = (float) (length * Math.cos(adjustedAngle));
            float y2 = (float) (length * Math.sin(adjustedAngle));

            float perpX = (float) (-Math.sin(adjustedAngle) * width);
            float perpY = (float) (Math.cos(adjustedAngle) * width);

            buffer.addVertex(baseMat, x1 + perpX, y1 + perpY, 0).setColor(255, 200, 50, 40);
            buffer.addVertex(baseMat, x1 - perpX, y1 - perpY, 0).setColor(255, 200, 50, 40);
            buffer.addVertex(baseMat, x2, y2, 0).setColor(255, 150, 30, 0);
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
    }

    private void renderRings(Matrix4f modelViewMatrix, CelestialBody body, Vector3d viewerPos) {
        Vector3d bodyPos = OrbitManager.getPosition(body.getName());
        double dx = bodyPos.x - viewerPos.x;
        double dy = bodyPos.y - viewerPos.y;
        double dz = bodyPos.z - viewerPos.z;

        Matrix4f mat = new Matrix4f(modelViewMatrix).translate((float) dx, (float) dy, (float) dz);

        double innerR = body.getRingInnerRadius() * 0.5;
        double outerR = body.getRingOuterRadius() * 0.5;
        int ringColor = body.getRingColor();
        int rr = (ringColor >> 16) & 0xFF;
        int rg = (ringColor >> 8) & 0xFF;
        int rb = ringColor & 0xFF;
        int ra = (ringColor >> 24) & 0xFF;

        RenderSystem.enableBlend();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            buffer.addVertex(mat, (float)(innerR * cos), 0, (float)(innerR * sin))
                    .setColor(rr, rg, rb, ra / 2);
            buffer.addVertex(mat, (float)(outerR * cos), 0, (float)(outerR * sin))
                    .setColor(rr, rg, rb, ra);
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
    }

    private void renderAtmosphericGlow(Matrix4f modelViewMatrix, CelestialBody body, Vector3d viewerPos) {
        Vector3d bodyPos = OrbitManager.getPosition(body.getName());
        double dx = bodyPos.x - viewerPos.x;
        double dy = bodyPos.y - viewerPos.y;
        double dz = bodyPos.z - viewerPos.z;

        double radius = body.getOrbitData().radius();
        float glowRadius = (float) (radius * 0.6);

        Matrix4f mat = new Matrix4f(modelViewMatrix).translate((float) dx, (float) dy, (float) dz);
        mat.scale(glowRadius, glowRadius, glowRadius);

        int color = body.getColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float alpha = (float) Math.min(0.3, body.getAtmosphere().pressureAtm() * 0.1);
        buffer.addVertex(mat, -1.5f, -1.5f, 0).setColor(r, g, b, (int)(alpha * 255));
        buffer.addVertex(mat, -1.5f, 1.5f, 0).setColor(r, g, b, (int)(alpha * 255));
        buffer.addVertex(mat, 1.5f, 1.5f, 0).setColor(r, g, b, (int)(alpha * 255));
        buffer.addVertex(mat, 1.5f, -1.5f, 0).setColor(r, g, b, (int)(alpha * 255));

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0, 0, 0);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    private static class SeededRandom {
        private long seed;
        SeededRandom(long seed) { this.seed = seed; }
        double nextDouble() {
            seed = seed * 25214903917L + 11L;
            return (seed & 0x1FFFFFFFFFFFFFL) / (double) (1L << 53);
        }
        float nextFloat() { return (float) nextDouble(); }
        double nextGaussian() {
            double u1 = nextDouble();
            double u2 = nextDouble();
            return Math.sqrt(-2 * Math.log(Math.max(u1, 1e-10))) * Math.cos(2 * Math.PI * u2);
        }
    }
}
