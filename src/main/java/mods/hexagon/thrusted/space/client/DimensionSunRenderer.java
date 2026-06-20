package mods.hexagon.thrusted.space.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mods.hexagon.thrusted.space.SpaceEngine;
import mods.hexagon.thrusted.space.body.Star;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class DimensionSunRenderer {
    private static final ResourceLocation SUN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");

    public static class SunProperties {
        public float size = 30f;
        public int color = 0xFFFFFF;
        public float brightness = 1f;
        public double temperature = 5778.0;
    }

    public static void renderDimensionSun(Level level, PoseStack stack, MultiBufferSource buffer, Camera camera) {
        String dimPath = level.dimension().location().getPath();
        SunProperties props = getSunPropertiesForDimension(dimPath);
        renderCustomSun(stack, buffer, camera, props, level);
    }

    private static SunProperties getSunPropertiesForDimension(String dimPath) {
        SpaceEngine engine = SpaceEngine.getInstance();
        Star sun = engine.getSolarSystem().getSun();

        SunProperties props = new SunProperties();
        props.size = 30f;
        props.color = sun.getColor();
        props.brightness = 1f;
        props.temperature = sun.getSurfaceTemperature();

        switch (dimPath) {
            case "mercury" -> {
                props.size = 45f; props.brightness = 1.5f; props.temperature = 6000;
            }
            case "venus" -> {
                props.size = 38f; props.brightness = 1.3f; props.temperature = 5800;
            }
            case "earth" -> {
                props.size = 30f; props.brightness = 1f; props.temperature = 5778;
            }
            case "mars" -> {
                props.size = 20f; props.brightness = 0.7f; props.temperature = 5700;
            }
            case "jupiter" -> {
                props.size = 12f; props.brightness = 0.4f; props.temperature = 5600;
            }
            case "saturn" -> {
                props.size = 10f; props.brightness = 0.35f; props.temperature = 5550;
            }
            case "uranus" -> {
                props.size = 7f; props.brightness = 0.25f; props.temperature = 5500;
            }
            case "neptune" -> {
                props.size = 6f; props.brightness = 0.2f; props.temperature = 5450;
            }
            case "space" -> {
                props.size = 50f; props.brightness = 2f; props.temperature = 5778;
            }
        }

        return props;
    }

    private static void renderCustomSun(PoseStack stack, MultiBufferSource buffer, Camera camera, SunProperties props, Level level) {
        long dayTime = level.getDayTime() % 24000L;
        float angle = (dayTime / 24000f) * 360f;
        float dist = 1000f;

        float sunX = (float) Math.cos(Math.toRadians(angle)) * dist;
        float sunZ = (float) Math.sin(Math.toRadians(angle)) * dist;

        stack.pushPose();
        stack.translate(sunX, sunZ, -dist);
        stack.scale(props.size, props.size, props.size);

        int color = props.color;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (props.temperature > 6000) {
            b = Math.min(1f, b + 0.2f);
        } else if (props.temperature < 5500) {
            r = Math.min(1f, r + 0.2f);
            b = Math.max(0f, b - 0.1f);
        }

        VertexConsumer beam = buffer.getBuffer(RenderType.beaconBeam(
                ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png"), false));

        float s = 1f;
        beam.addVertex(stack.last().pose(), -s, -s, 0).setColor(r, g, b, props.brightness).setUv(0, 0).setOverlay(655360).setLight(15728880);
        beam.setNormal(stack.last(), 0, 0, -1);
        beam.addVertex(stack.last().pose(), s, -s, 0).setColor(r, g, b, props.brightness).setUv(1, 0).setOverlay(655360).setLight(15728880);
        beam.setNormal(stack.last(), 0, 0, -1);
        beam.addVertex(stack.last().pose(), s, s, 0).setColor(r, g, b, props.brightness).setUv(1, 1).setOverlay(655360).setLight(15728880);
        beam.setNormal(stack.last(), 0, 0, -1);
        beam.addVertex(stack.last().pose(), -s, s, 0).setColor(r, g, b, props.brightness).setUv(0, 1).setOverlay(655360).setLight(15728880);
        beam.setNormal(stack.last(), 0, 0, -1);

        VertexConsumer glow = buffer.getBuffer(RenderType.translucent());
        float gs = s * 1.5f;
        glow.addVertex(stack.last().pose(), -gs, -gs, 0).setColor(r, g, b, props.brightness * 0.5f).setUv(0, 0).setLight(15728880);
        glow.setNormal(stack.last(), 0, 0, -1);
        glow.addVertex(stack.last().pose(), gs, -gs, 0).setColor(r, g, b, props.brightness * 0.5f).setUv(1, 0).setLight(15728880);
        glow.setNormal(stack.last(), 0, 0, -1);
        glow.addVertex(stack.last().pose(), gs, gs, 0).setColor(r, g, b, props.brightness * 0.5f).setUv(1, 1).setLight(15728880);
        glow.setNormal(stack.last(), 0, 0, -1);
        glow.addVertex(stack.last().pose(), -gs, gs, 0).setColor(r, g, b, props.brightness * 0.5f).setUv(0, 1).setLight(15728880);
        glow.setNormal(stack.last(), 0, 0, -1);

        stack.popPose();
    }
}
