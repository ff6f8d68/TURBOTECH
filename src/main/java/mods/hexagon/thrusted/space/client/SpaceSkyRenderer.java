package mods.hexagon.thrusted.space.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mods.hexagon.thrusted.ThrustedRenderTypes;
import mods.hexagon.thrusted.space.SpaceEngine;
import mods.hexagon.thrusted.space.body.Planet;
import mods.hexagon.thrusted.space.body.Star;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SpaceSkyRenderer {

    private static final double SKYBOX_DIST = 100.0;

    public static void renderSpaceSky(PoseStack stack, MultiBufferSource buffer, Camera camera, float partialTick) {
        float pitchRad = (float) Math.toRadians(camera.getXRot());

        stack.pushPose();
        renderSun(stack, buffer, camera, pitchRad);
        CelestialBodyRenderer.renderCelestialBodies(stack, buffer, camera, partialTick);
        renderOrbitalPaths(stack, buffer, camera, pitchRad);
        stack.popPose();
    }

    private static void renderSun(PoseStack stack, MultiBufferSource buffer, Camera camera, float pitchRad) {
        SpaceEngine engine = SpaceEngine.getInstance();
        Star sun = engine.getSolarSystem().getSun();
        Vec3 sunPos = sun.getPosition();
        Vec3 camPos = camera.getPosition();

        double dist = sunPos.distanceTo(camPos);
        double scale = Math.max(sun.getRadius() * 5e-5, dist * 0.005);

        stack.pushPose();

        // Skybox-anchored positioning with pitch compensation
        Vec3 dir = sunPos.subtract(camPos);
        double len = dir.length();
        if (len > 0.001) {
            dir = dir.normalize();
        }
        stack.translate(dir.x * SKYBOX_DIST, dir.y * SKYBOX_DIST, dir.z * SKYBOX_DIST);
        stack.mulPose(new Quaternionf().rotationX(-pitchRad));
        stack.scale((float) scale, (float) scale, (float) scale);

        int color = sun.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        VertexConsumer consumer = buffer.getBuffer(ThrustedRenderTypes.CELESTIAL_SOLID);
        float size = 1f;
        int segs = 48;
        int subs = 32;

        for (int seg = 0; seg < segs; seg++) {
            float theta1 = seg * (float) (2 * Math.PI) / segs;
            float theta2 = (seg + 1) * (float) (2 * Math.PI) / segs;
            for (int sub = 0; sub < subs; sub++) {
                float phi1 = sub * (float) Math.PI / subs;
                float phi2 = (sub + 1) * (float) Math.PI / subs;

                float x1 = size * (float) Math.sin(phi1) * (float) Math.cos(theta1);
                float y1 = size * (float) Math.cos(phi1);
                float z1 = size * (float) Math.sin(phi1) * (float) Math.sin(theta1);
                float x2 = size * (float) Math.sin(phi1) * (float) Math.cos(theta2);
                float y2 = size * (float) Math.cos(phi1);
                float z2 = size * (float) Math.sin(phi1) * (float) Math.sin(theta2);
                float x3 = size * (float) Math.sin(phi2) * (float) Math.cos(theta2);
                float y3 = size * (float) Math.cos(phi2);
                float z3 = size * (float) Math.sin(phi2) * (float) Math.sin(theta2);
                float x4 = size * (float) Math.sin(phi2) * (float) Math.cos(theta1);
                float y4 = size * (float) Math.cos(phi2);
                float z4 = size * (float) Math.sin(phi2) * (float) Math.sin(theta1);

                consumer.addVertex(stack.last().pose(), x1, y1, z1).setColor(r, g, b, 1f);
                consumer.addVertex(stack.last().pose(), x2, y2, z2).setColor(r, g, b, 1f);
                consumer.addVertex(stack.last().pose(), x3, y3, z3).setColor(r, g, b, 1f);
                consumer.addVertex(stack.last().pose(), x4, y4, z4).setColor(r, g, b, 1f);
            }
        }

        VertexConsumer glow = buffer.getBuffer(ThrustedRenderTypes.CELESTIAL_GLOW);
        float glowSize = size * 2.5f;
        int glowSegs = 32;
        for (int seg = 0; seg < glowSegs; seg++) {
            float theta1 = seg * (float) (2 * Math.PI) / glowSegs;
            float theta2 = (seg + 1) * (float) (2 * Math.PI) / glowSegs;
            glow.addVertex(stack.last().pose(), glowSize * (float) Math.cos(theta1), 0, glowSize * (float) Math.sin(theta1)).setColor(r, g, b, 0.3f);
            glow.addVertex(stack.last().pose(), glowSize * (float) Math.cos(theta2), 0, glowSize * (float) Math.sin(theta2)).setColor(r, g, b, 0.3f);
            glow.addVertex(stack.last().pose(), 0, 0, 0).setColor(r, g, b, 0.5f);
            glow.addVertex(stack.last().pose(), 0, 0, 0).setColor(r, g, b, 0.5f);
        }

        stack.popPose();
    }

    private static void renderOrbitalPaths(PoseStack stack, MultiBufferSource buffer, Camera camera, float pitchRad) {
        SpaceEngine engine = SpaceEngine.getInstance();
        VertexConsumer consumer = buffer.getBuffer(ThrustedRenderTypes.CELESTIAL_LINES);

        // Orbital paths are world-space, apply pitch compensation
        stack.pushPose();
        stack.mulPose(new Quaternionf().rotationX(-pitchRad));

        for (Planet planet : engine.getSolarSystem().getPlanets()) {
            if (planet.isMoon()) continue;

            double a = planet.getSemiMajorAxis() * Planet.AU_SCALE;
            double e = planet.getEccentricity();
            int segments = 48;

            // Scale orbits to fit within skybox view
            float orbitScale = (float) (SKYBOX_DIST / Math.max(a, 1.0));

            for (int i = 0; i < segments; i++) {
                float theta1 = i * (float) (2 * Math.PI) / segments;
                float theta2 = (i + 1) * (float) (2 * Math.PI) / segments;

                double r1 = a * (1 - e * e) / (1 + e * Math.cos(theta1));
                double r2 = a * (1 - e * e) / (1 + e * Math.cos(theta2));

                float x1 = (float) (r1 * Math.cos(theta1) * orbitScale);
                float z1 = (float) (r1 * Math.sin(theta1) * orbitScale);
                float x2 = (float) (r2 * Math.cos(theta2) * orbitScale);
                float z2 = (float) (r2 * Math.sin(theta2) * orbitScale);

                consumer.addVertex(stack.last().pose(), x1, 0, z1).setColor(0.3f, 0.3f, 0.5f, 0.4f);
                consumer.setNormal(stack.last(), 0, 1, 0);
                consumer.addVertex(stack.last().pose(), x2, 0, z2).setColor(0.3f, 0.3f, 0.5f, 0.4f);
                consumer.setNormal(stack.last(), 0, 1, 0);
            }

            // Moon orbits
            for (Planet moon : planet.getMoons()) {
                double mA = moon.getSemiMajorAxis() * Planet.AU_SCALE;
                double mE = moon.getEccentricity();
                int mSeg = 24;
                float moonOrbitScale = (float) (SKYBOX_DIST / Math.max(mA, 1.0)) * 0.1f;
                Vec3 pDir = planet.getPosition().normalize();
                float px = (float) (pDir.x * SKYBOX_DIST);
                float pz = (float) (pDir.z * SKYBOX_DIST);
                for (int i = 0; i < mSeg; i++) {
                    float theta1 = i * (float) (2 * Math.PI) / mSeg;
                    float theta2 = (i + 1) * (float) (2 * Math.PI) / mSeg;
                    double r1 = mA * (1 - mE * mE) / (1 + mE * Math.cos(theta1));
                    double r2 = mA * (1 - mE * mE) / (1 + mE * Math.cos(theta2));
                    float x1 = (float) (px + r1 * Math.cos(theta1) * moonOrbitScale);
                    float z1 = (float) (pz + r1 * Math.sin(theta1) * moonOrbitScale);
                    float x2 = (float) (px + r2 * Math.cos(theta2) * moonOrbitScale);
                    float z2 = (float) (pz + r2 * Math.sin(theta2) * moonOrbitScale);
                    consumer.addVertex(stack.last().pose(), x1, 0, z1).setColor(0.5f, 0.5f, 0.3f, 0.25f);
                    consumer.setNormal(stack.last(), 0, 1, 0);
                    consumer.addVertex(stack.last().pose(), x2, 0, z2).setColor(0.5f, 0.5f, 0.3f, 0.25f);
                    consumer.setNormal(stack.last(), 0, 1, 0);
                }
            }
        }

        stack.popPose();
    }
}
