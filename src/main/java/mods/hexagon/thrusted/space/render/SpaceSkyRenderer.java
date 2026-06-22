package mods.hexagon.thrusted.space.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.client.render.ThrustedRenderTypes;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.OrbitManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class SpaceSkyRenderer extends DimensionSpecialEffects {
    public static final ResourceLocation SPACE_EFFECTS = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "space");

    private static final int STAR_COUNT = 2000;

    public SpaceSkyRenderer() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix,
                              Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        setupFog.run();

        Vec3 camPos = camera.getPosition();
        Vector3d viewerPos = new Vector3d(camPos.x, camPos.y, camPos.z);

        renderStars(modelViewMatrix, level, partialTick);
        renderCelestialBodies(modelViewMatrix, viewerPos, partialTick);

        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        return true;
    }

    private void renderStars(Matrix4f modelViewMatrix, ClientLevel level, float partialTick) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        long seed = 42L;
        SeededRandom rng = new SeededRandom(seed);
        float brightness = 0.6f + 0.4f * (float) Math.sin(level.getGameTime() * 0.001);

        for (int i = 0; i < STAR_COUNT; i++) {
            double theta = rng.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * rng.nextDouble() - 1.0);
            double dist = 500.0 + rng.nextDouble() * 500.0;

            float x = (float) (dist * Math.sin(phi) * Math.cos(theta));
            float y = (float) (dist * Math.cos(phi));
            float z = (float) (dist * Math.sin(phi) * Math.sin(theta));

            float starBright = brightness * (0.5f + rng.nextFloat() * 0.5f);
            int alpha = (int) (starBright * 255);
            float s = 0.5f;
            buffer.addVertex(modelViewMatrix, x - s, y - s, z).setColor(alpha, alpha, alpha, alpha);
            buffer.addVertex(modelViewMatrix, x + s, y - s, z).setColor(alpha, alpha, alpha, alpha);
            buffer.addVertex(modelViewMatrix, x + s, y + s, z).setColor(alpha, alpha, alpha, alpha);
            buffer.addVertex(modelViewMatrix, x - s, y + s, z).setColor(alpha, alpha, alpha, alpha);
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
    }

    private void renderCelestialBodies(Matrix4f modelViewMatrix, Vector3d viewerPos, float partialTick) {
        for (CelestialBody body : CelestialBodyRegistry.getAll()) {
            if ("Sun".equals(body.getName())) {
                renderSun(modelViewMatrix, body, viewerPos, partialTick);
            } else {
                CelestialBodyMesh.renderBody(modelViewMatrix, body, viewerPos, partialTick,
                        ThrustedRenderTypes.CELESTIAL_SOLID);
                CelestialBodyMesh.renderBody(modelViewMatrix, body, viewerPos, partialTick,
                        ThrustedRenderTypes.CELESTIAL_GLOW);
            }
        }
    }

    private void renderSun(Matrix4f modelViewMatrix, CelestialBody sun, Vector3d viewerPos, float partialTick) {
        CelestialBodyMesh.renderBody(modelViewMatrix, sun, viewerPos, partialTick,
                ThrustedRenderTypes.CELESTIAL_SOLID);

        Vector3d sunPos = OrbitManager.getPosition(sun.getName());
        double dx = sunPos.x - viewerPos.x;
        double dy = sunPos.y - viewerPos.y;
        double dz = sunPos.z - viewerPos.z;

        Matrix4f mat = new Matrix4f(modelViewMatrix).translate((float) dx, (float) dy, (float) dz);
        double radius = sun.getOrbitData().radius();
        float glowScale = (float) (radius * 4.0);
        mat.scale(glowScale, glowScale, glowScale);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float g = 0.8f;
        buffer.addVertex(mat, -1, -1, 0).setColor(g, g, 0.3f, 0.3f);
        buffer.addVertex(mat, -1, 1, 0).setColor(g, g, 0.3f, 0.3f);
        buffer.addVertex(mat, 1, 1, 0).setColor(g, g, 0.3f, 0.3f);
        buffer.addVertex(mat, 1, -1, 0).setColor(g, g, 0.3f, 0.3f);

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
    }
}
