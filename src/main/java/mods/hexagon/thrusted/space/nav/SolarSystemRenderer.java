package mods.hexagon.thrusted.space.nav;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.OrbitManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import java.util.Collection;

public class SolarSystemRenderer {
    private static final float GRID_SIZE = 100.0f;
    private static final int GRID_LINES = 20;
    private static final float SCALE = 0.15f;

    public void render(GuiGraphics gui, int screenW, int screenH, float mouseX, float mouseY,
                        float zoom, float rotX, float rotY) {
        int cx = screenW / 2;
        int cy = screenH / 2;
        float effectiveScale = SCALE * zoom;

        PoseStack poseStack = gui.pose();
        poseStack.pushPose();
        poseStack.translate(cx, cy, 0);
        poseStack.scale(1, -1, 1);

        Matrix4f view = new Matrix4f();
        view.rotationX(rotX * (float) Math.PI / 180f);
        view.rotationY(rotY * (float) Math.PI / 180f);

        poseStack.mulPose(view);

        Matrix4f mat = poseStack.last().pose();

        renderCoordinateGrid(gui, effectiveScale);

        renderOrbits(gui, effectiveScale);

        Collection<CelestialBody> bodies = CelestialBodyRegistry.getAll();
        for (CelestialBody body : bodies) {
            Vector3d pos = OrbitManager.getPosition(body.getName());
            float x = (float) (pos.x * effectiveScale);
            float y = (float) (pos.z * effectiveScale);

            renderBodyIcon(gui, x, y, body.getColor(),
                    (float) (body.getOrbitData().radius() * effectiveScale));
        }

        renderPlayerIcons(gui, effectiveScale);

        poseStack.popPose();
    }

    private void renderCoordinateGrid(GuiGraphics gui, float scale) {
        PoseStack poseStack = gui.pose();
        Matrix4f mat = poseStack.last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = -GRID_LINES; i <= GRID_LINES; i++) {
            float pos = i * GRID_SIZE * scale;
            float limit = GRID_LINES * GRID_SIZE * scale;
            int alpha = (i == 0) ? 80 : 30;

            buffer.addVertex(mat, pos, 0, -limit).setColor(80, 180, 255, alpha);
            buffer.addVertex(mat, pos, 0, limit).setColor(80, 180, 255, alpha);
            buffer.addVertex(mat, -limit, 0, pos).setColor(80, 180, 255, alpha);
            buffer.addVertex(mat, limit, 0, pos).setColor(80, 180, 255, alpha);
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
    }

    private void renderOrbits(GuiGraphics gui, float scale) {
        PoseStack poseStack = gui.pose();
        Matrix4f mat = poseStack.last().pose();

        for (CelestialBody body : CelestialBodyRegistry.getAll()) {
            if (body.getParent() == null) continue;
            double a = body.getOrbitData().semiMajorAxis() * scale;
            int segments = 64;

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i <= segments; i++) {
                double angle = 2.0 * Math.PI * i / segments;
                float x = (float) (a * Math.cos(angle));
                float z = (float) (a * Math.sin(angle));
                buffer.addVertex(mat, x, 0, z).setColor(80, 180, 255, 40);
            }

            var rendered = buffer.build();
            if (rendered != null) {
                BufferUploader.draw(rendered);
            }
        }
    }

    private void renderBodyIcon(GuiGraphics gui, float x, float y, int color, float radius) {
        PoseStack poseStack = gui.pose();
        Matrix4f mat = poseStack.last().pose();

        float r = Math.max(radius, 2.0f);
        int cr = (color >> 16) & 0xFF;
        int cg = (color >> 8) & 0xFF;
        int cb = color & 0xFF;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        buffer.addVertex(mat, x, y, 0).setColor(cr, cg, cb, 255);
        int segments = 16;
        for (int i = 0; i <= segments; i++) {
            float a = (float) (2.0 * Math.PI * i / segments);
            buffer.addVertex(mat, x + r * (float) Math.cos(a), y + r * (float) Math.sin(a), 0)
                    .setColor(cr, cg, cb, 200);
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
    }

    private void renderPlayerIcons(GuiGraphics gui, float scale) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = gui.pose();
        Matrix4f mat = poseStack.last().pose();

        for (var player : mc.level.players()) {
            float px = (float) (player.getX() * scale);
            float pz = (float) (player.getZ() * scale);
            boolean isSelf = player == mc.player;

            RenderSystem.enableBlend();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

            float size = isSelf ? 4.0f : 3.0f;
            int r = isSelf ? 180 : 255;
            int g = isSelf ? 180 : 50;
            int b = isSelf ? 180 : 50;
            int a = 220;

            renderShipTriangle(buffer, mat, px, pz, size, r, g, b, a);

            var rendered = buffer.build();
            if (rendered != null) {
                BufferUploader.draw(rendered);
            }
        }
    }

    private void renderShipTriangle(BufferBuilder buffer, Matrix4f mat, float x, float z, float size,
                                     int r, int g, int b, int a) {
        float h = size * 0.6f;
        float w = size * 0.4f;
        buffer.addVertex(mat, x, z - h, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x - w, z + h, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, x + w, z + h, 0).setColor(r, g, b, a);
    }
}
