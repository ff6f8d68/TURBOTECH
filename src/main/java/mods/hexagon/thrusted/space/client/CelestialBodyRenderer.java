package mods.hexagon.thrusted.space.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mods.hexagon.thrusted.ThrustedRenderTypes;
import mods.hexagon.thrusted.space.SpaceEngine;
import mods.hexagon.thrusted.space.body.CelestialBody;
import mods.hexagon.thrusted.space.body.Planet;
import mods.hexagon.thrusted.space.body.Star;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class CelestialBodyRenderer {

    /** Skybox distance at which celestial bodies are projected */
    private static final double SKYBOX_DIST = 100.0;

    public static void renderCelestialBodies(PoseStack stack, MultiBufferSource buffer, Camera camera, float partialTick) {
        SpaceEngine engine = SpaceEngine.getInstance();
        if (engine == null) return;
        if (engine.getSolarSystem() == null) return;

        float pitchRad = (float) Math.toRadians(camera.getXRot());

        for (var body : engine.getSolarSystem().getAllBodies()) {
            renderCelestialBody(stack, buffer, camera, body, pitchRad);
        }
    }

    private static void renderCelestialBody(PoseStack stack, MultiBufferSource buffer, Camera camera, CelestialBody body, float pitchRad) {
        if (body instanceof Star star) {
            renderStar(stack, buffer, camera, star, pitchRad);
        } else if (body instanceof Planet planet) {
            renderPlanet(stack, buffer, camera, planet, pitchRad);
            for (Planet moon : planet.getMoons()) {
                renderMoon(stack, buffer, camera, moon, pitchRad);
            }
        }
    }

    /**
     * Positions the poseStack at a skybox-anchored point for the given body.
     * AFTER_SKY shader auto-applies yaw rotation, so we only compensate pitch.
     */
    private static void applySkyboxTransform(PoseStack stack, Camera camera, Vec3 bodyPos, float pitchRad) {
        Vec3 camPos = camera.getPosition();
        Vec3 dir = bodyPos.subtract(camPos);
        double len = dir.length();
        if (len < 0.001) return;
        dir = dir.normalize();

        stack.translate(dir.x * SKYBOX_DIST, dir.y * SKYBOX_DIST, dir.z * SKYBOX_DIST);
        // Inverse pitch compensation — AFTER_SKY shader handles yaw automatically
        stack.mulPose(new Quaternionf().rotationX(-pitchRad));
    }

    private static void renderStar(PoseStack stack, MultiBufferSource buffer, Camera camera, Star star, float pitchRad) {
        Vec3 pos = star.getPosition();
        Vec3 camPos = camera.getPosition();
        double dist = pos.distanceTo(camPos);

        double scale = Math.max(star.getRadius() * 5e-5, dist * 0.005);

        stack.pushPose();
        applySkyboxTransform(stack, camera, pos, pitchRad);
        stack.scale((float) scale, (float) scale, (float) scale);

        int color = star.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        VertexConsumer consumer = buffer.getBuffer(ThrustedRenderTypes.CELESTIAL_SOLID);
        renderSphere(stack, consumer, 1f, 48, 32, r, g, b, 1f);

        VertexConsumer glow = buffer.getBuffer(ThrustedRenderTypes.CELESTIAL_GLOW);
        float glowSize = 2.5f;
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

    private static void renderPlanet(PoseStack stack, MultiBufferSource buffer, Camera camera, Planet planet, float pitchRad) {
        Vec3 pos = planet.getPosition();
        Vec3 camPos = camera.getPosition();
        Vec3 toSun = SpaceEngine.getInstance().getSolarSystem().getSun().getPosition().subtract(pos).normalize();

        stack.pushPose();
        applySkyboxTransform(stack, camera, pos, pitchRad);

        double scale = planet.getRadius() * 0.0015;
        stack.scale((float) scale, (float) scale, (float) scale);

        // Apply axial tilt (pitch) then rotation (yaw)
        stack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(planet.getAxialTilt())));
        stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) planet.getRotationAngle()));

        int color = planet.getColor();
        float rBase = ((color >> 16) & 0xFF) / 255f;
        float gBase = ((color >> 8) & 0xFF) / 255f;
        float bBase = (color & 0xFF) / 255f;

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

                float nx1 = x1, ny1 = y1, nz1 = z1;
                float nx2 = x2, ny2 = y2, nz2 = z2;
                float nx3 = x3, ny3 = y3, nz3 = z3;
                float nx4 = x4, ny4 = y4, nz4 = z4;

                float diff1 = Math.max(0, nx1 * (float)toSun.x + ny1 * (float)toSun.y + nz1 * (float)toSun.z);
                float diff2 = Math.max(0, nx2 * (float)toSun.x + ny2 * (float)toSun.y + nz2 * (float)toSun.z);
                float diff3 = Math.max(0, nx3 * (float)toSun.x + ny3 * (float)toSun.y + nz3 * (float)toSun.z);
                float diff4 = Math.max(0, nx4 * (float)toSun.x + ny4 * (float)toSun.y + nz4 * (float)toSun.z);

                float amb = 0.3f;
                float light1 = amb + (1 - amb) * diff1;
                float light2 = amb + (1 - amb) * diff2;
                float light3 = amb + (1 - amb) * diff3;
                float light4 = amb + (1 - amb) * diff4;

                float rimPower = 3f;
                float rim1 = (float) Math.pow(1 - Math.abs(dotToCam(nx1, ny1, nz1, camPos, pos)), rimPower) * 0.3f;
                float rim2 = (float) Math.pow(1 - Math.abs(dotToCam(nx2, ny2, nz2, camPos, pos)), rimPower) * 0.3f;
                float rim3 = (float) Math.pow(1 - Math.abs(dotToCam(nx3, ny3, nz3, camPos, pos)), rimPower) * 0.3f;
                float rim4 = (float) Math.pow(1 - Math.abs(dotToCam(nx4, ny4, nz4, camPos, pos)), rimPower) * 0.3f;

                float r1 = clampRgb(rBase * (light1 + rim1));
                float g1 = clampRgb(gBase * (light1 + rim1));
                float b1 = clampRgb(bBase * (light1 + rim1));
                float r2 = clampRgb(rBase * (light2 + rim2));
                float g2 = clampRgb(gBase * (light2 + rim2));
                float b2 = clampRgb(bBase * (light2 + rim2));
                float r3 = clampRgb(rBase * (light3 + rim3));
                float g3 = clampRgb(gBase * (light3 + rim3));
                float b3 = clampRgb(bBase * (light3 + rim3));
                float r4 = clampRgb(rBase * (light4 + rim4));
                float g4 = clampRgb(gBase * (light4 + rim4));
                float b4 = clampRgb(bBase * (light4 + rim4));

                consumer.addVertex(stack.last().pose(), x1, y1, z1).setColor(r1, g1, b1, 1f);
                consumer.addVertex(stack.last().pose(), x2, y2, z2).setColor(r2, g2, b2, 1f);
                consumer.addVertex(stack.last().pose(), x3, y3, z3).setColor(r3, g3, b3, 1f);
                consumer.addVertex(stack.last().pose(), x4, y4, z4).setColor(r4, g4, b4, 1f);
            }
        }

        stack.popPose();
    }

    private static void renderMoon(PoseStack stack, MultiBufferSource buffer, Camera camera, Planet moon, float pitchRad) {
        Vec3 pos = moon.getPosition();
        Vec3 camPos = camera.getPosition();
        Vec3 toSun = SpaceEngine.getInstance().getSolarSystem().getSun().getPosition().subtract(pos).normalize();

        stack.pushPose();
        applySkyboxTransform(stack, camera, pos, pitchRad);

        double scale = moon.getRadius() * 0.003;
        stack.scale((float) scale, (float) scale, (float) scale);

        int color = moon.getColor();
        float rBase = ((color >> 16) & 0xFF) / 255f;
        float gBase = ((color >> 8) & 0xFF) / 255f;
        float bBase = (color & 0xFF) / 255f;

        VertexConsumer consumer = buffer.getBuffer(ThrustedRenderTypes.CELESTIAL_SOLID);
        float size = 1f;
        int segs = 24;
        int subs = 16;

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

                float nx1 = x1, ny1 = y1, nz1 = z1;
                float nx2 = x2, ny2 = y2, nz2 = z2;
                float nx3 = x3, ny3 = y3, nz3 = z3;
                float nx4 = x4, ny4 = y4, nz4 = z4;

                float diff1 = Math.max(0, nx1 * (float)toSun.x + ny1 * (float)toSun.y + nz1 * (float)toSun.z);
                float diff2 = Math.max(0, nx2 * (float)toSun.x + ny2 * (float)toSun.y + nz2 * (float)toSun.z);
                float diff3 = Math.max(0, nx3 * (float)toSun.x + ny3 * (float)toSun.y + nz3 * (float)toSun.z);
                float diff4 = Math.max(0, nx4 * (float)toSun.x + ny4 * (float)toSun.y + nz4 * (float)toSun.z);

                float amb = 0.25f;
                float light1 = amb + (1 - amb) * diff1;
                float light2 = amb + (1 - amb) * diff2;
                float light3 = amb + (1 - amb) * diff3;
                float light4 = amb + (1 - amb) * diff4;

                consumer.addVertex(stack.last().pose(), x1, y1, z1).setColor(clampRgb(rBase * light1), clampRgb(gBase * light1), clampRgb(bBase * light1), 1f);
                consumer.addVertex(stack.last().pose(), x2, y2, z2).setColor(clampRgb(rBase * light2), clampRgb(gBase * light2), clampRgb(bBase * light2), 1f);
                consumer.addVertex(stack.last().pose(), x3, y3, z3).setColor(clampRgb(rBase * light3), clampRgb(gBase * light3), clampRgb(bBase * light3), 1f);
                consumer.addVertex(stack.last().pose(), x4, y4, z4).setColor(clampRgb(rBase * light4), clampRgb(gBase * light4), clampRgb(bBase * light4), 1f);
            }
        }

        stack.popPose();
    }

    /** Render a solid sphere with flat color */
    private static void renderSphere(PoseStack stack, VertexConsumer consumer, float size, int segs, int subs, float r, float g, float b, float a) {
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

                consumer.addVertex(stack.last().pose(), x1, y1, z1).setColor(r, g, b, a);
                consumer.addVertex(stack.last().pose(), x2, y2, z2).setColor(r, g, b, a);
                consumer.addVertex(stack.last().pose(), x3, y3, z3).setColor(r, g, b, a);
                consumer.addVertex(stack.last().pose(), x4, y4, z4).setColor(r, g, b, a);
            }
        }
    }

    private static float dotToCam(float nx, float ny, float nz, Vec3 camPos, Vec3 bodyPos) {
        double dx = camPos.x - bodyPos.x;
        double dy = camPos.y - bodyPos.y;
        double dz = camPos.z - bodyPos.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return (float) ((dx / len) * nx + (dy / len) * ny + (dz / len) * nz);
    }

    private static float clampRgb(float v) {
        return Math.max(0, Math.min(1, v));
    }
}
