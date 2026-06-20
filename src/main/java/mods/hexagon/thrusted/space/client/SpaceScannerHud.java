package mods.hexagon.thrusted.space.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import mods.hexagon.thrusted.space.SpaceEngine;
import mods.hexagon.thrusted.space.body.CelestialBody;
import mods.hexagon.thrusted.space.body.Planet;
import mods.hexagon.thrusted.space.body.Star;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

public class SpaceScannerHud {
    private static final int R = 55;
    private static final int M = 6;
    private static final double MIN_SZ = 1.5;
    private static final double MAX_SZ = 4.0;
    private static final double EARTH_R = 6371.0;

    public static void onRenderGui(GuiGraphics gui, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        String dim = mc.level.dimension().location().getPath();
        if (!dim.equals("space") && !dim.startsWith("planet_")) return;

        int cx = M + R;
        int cy = mc.getWindow().getGuiScaledHeight() - M - R;

        SpaceEngine e = SpaceEngine.getInstance();
        if (e == null || e.getSolarSystem() == null) return;

        // Semi-transparent background
        gui.fill(cx - R - 3, cy - R - 3, cx + R + 3, cy + R + 3, 0x66000000);

        // Draw 3D shaded sphere as minimap globe
        drawShadedSphere(gui, cx, cy, R);

        // Smooth circle outlines
        drawCircle(gui, cx, cy, R, 0xFF556677);
        drawCircle(gui, cx, cy, R / 2, 0x44334466);

        Vec3 sunP = e.getSolarSystem().getSun().getPosition();
        Vec3 plr = mc.player.position();
        double maxD = 1.0;
        for (CelestialBody b : e.getSolarSystem().getAllBodies()) {
            double d = b.getPosition().distanceTo(sunP);
            if (d > maxD) maxD = d;
        }
        double pd = plr.distanceTo(sunP);
        if (pd > maxD) maxD = pd;
        double zoom = R / (maxD * 1.2);

        // Player yaw for minimap rotation
        float yaw = mc.player.getYRot();
        double cosYaw = Math.cos(Math.toRadians(yaw));
        double sinYaw = Math.sin(Math.toRadians(yaw));

        // Draw orbits (rotated by player yaw)
        for (Planet p : e.getSolarSystem().getPlanets()) {
            if (p.isMoon()) continue;
            double a = p.getSemiMajorAxis() * Planet.AU_SCALE * zoom;
            int ia = (int) a;
            if (ia > R) ia = R;
            drawRotatedCircle(gui, cx, cy, ia, 0x445577AA, cosYaw, sinYaw);
            for (Planet m : p.getMoons()) {
                double ma = m.getSemiMajorAxis() * Planet.AU_SCALE * zoom;
                int ima = (int) ma;
                Vec3 pp = p.getPosition().subtract(sunP);
                double rpx = pp.x * cosYaw - pp.z * sinYaw;
                double rpz = pp.x * sinYaw + pp.z * cosYaw;
                int mcx = cx + (int) (rpx * zoom);
                int mcy = cy - (int) (rpz * zoom);
                if (ima > 0) drawCircle(gui, mcx, mcy, ima, 0x2255AA88);
            }
        }

        // Draw celestial bodies (rotated by player yaw)
        for (CelestialBody b : e.getSolarSystem().getAllBodies()) {
            if (b instanceof Star) {
                drawGlowDot(gui, cx, cy, 5, 0xFFFFC800);
                gui.drawString(Minecraft.getInstance().font, "Sun", cx + 7, cy - 3, 0xFFFFC800);
                continue;
            }
            Vec3 rv = b.getPosition().subtract(sunP);
            double rx = rv.x * cosYaw - rv.z * sinYaw;
            double rz = rv.x * sinYaw + rv.z * cosYaw;
            int bx = cx + (int) (rx * zoom);
            int by = cy - (int) (rz * zoom);
            double ratio = Math.min(1.0, b.getRadius() / EARTH_R);
            int sz = Math.max(1, (int) (MIN_SZ + (MAX_SZ - MIN_SZ) * ratio));
            drawDot(gui, bx, by, sz, b.getColor() | 0xFF000000);
            String lbl = b.getName().length() > 6 ? b.getName().substring(0, 6) : b.getName();
            gui.drawString(Minecraft.getInstance().font, lbl, bx + sz + 2, by - 3, b.getColor() | 0xFF000000);
        }

        // Player ship (dot relative to sun, rotated)
        Vec3 pRel = plr.subtract(sunP);
        double prx = pRel.x * cosYaw - pRel.z * sinYaw;
        double prz = pRel.x * sinYaw + pRel.z * cosYaw;
        int ppx = cx + (int) (prx * zoom);
        int ppy = cy - (int) (prz * zoom);
        if (ppx >= cx - R && ppx <= cx + R && ppy >= cy - R && ppy <= cy + R) {
            // Draw player-facing direction triangle
            double dirLen = 6;
            float pa = (float) Math.toRadians(yaw);
            float pdx = (float) (dirLen * Math.sin(pa));
            float pdy = (float) (dirLen * Math.cos(pa));
            drawTriangle(gui, ppx, ppy, pdx, -pdy, 0xFFAAAAAA);
            gui.drawString(Minecraft.getInstance().font, "You", ppx + 4, ppy - 3, 0xFFAAAAAA);
        }

        // Info labels
        int lx = M;
        int ly = cy + R + 2;
        gui.drawString(Minecraft.getInstance().font,
                String.format("X:%d Y:%d Z:%d", (int) plr.x, (int) plr.y, (int) plr.z), lx, ly, 0xFFFFFF);

        long pc = e.getSolarSystem().getPlanets().stream().filter(p -> !p.isMoon()).count();
        long mc_ = e.getSolarSystem().getPlanets().stream().mapToLong(p -> p.getMoons().size()).sum();
        gui.drawString(Minecraft.getInstance().font,
                String.format("Bodies: %d [%d m]", pc, mc_), lx, ly + 9, 0xFFFFFF);

        String scaleStr = String.format("Scale: %.0f m/px", (1.0 / zoom) * 1000);
        gui.drawString(Minecraft.getInstance().font, scaleStr, lx, ly + 18, 0xFFFFFF);
    }

    private static void drawShadedSphere(GuiGraphics gui, int cx, int cy, int r) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        double lx = -0.3, ly = -0.5, lz = 0.8;
        double llen = Math.sqrt(lx * lx + ly * ly + lz * lz);
        double amb = 0.35;

        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y > r * r) continue;

                double dist = Math.sqrt(x * x + y * y) / r;
                double z = Math.sqrt(1.0 - Math.min(1.0, dist * dist));

                double nx = x / (double) r;
                double ny = y / (double) r;
                double nz = z;

                double dot = (nx * lx + ny * ly + nz * lz) / llen;
                dot = Math.max(0, dot);

                double brightness = amb + (1.0 - amb) * dot;
                int ir = Math.min(255, (int) (0x33 * brightness));
                int ig = Math.min(255, (int) (0x44 * brightness));
                int ib = Math.min(255, (int) (0x55 * brightness));

                float x1 = cx + x, y1 = cy + y;
                float x2 = cx + x + 1, y2 = cy + y + 1;

                builder.addVertex(x1, y1, 0).setColor(ir, ig, ib, 200);
                builder.addVertex(x2, y1, 0).setColor(ir, ig, ib, 200);
                builder.addVertex(x2, y2, 0).setColor(ir, ig, ib, 200);
                builder.addVertex(x1, y2, 0).setColor(ir, ig, ib, 200);
            }
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void drawCircle(GuiGraphics gui, int cx, int cy, int r, int color) {
        if (r <= 0) return;
        int step = 1;
        int aR = (color >> 24) & 0xFF;
        int rR = (color >> 16) & 0xFF;
        int gR = (color >> 8) & 0xFF;
        int bR = color & 0xFF;
        int rgba = (aR << 24) | (rR << 16) | (gR << 8) | bR;
        for (int a = 0; a < 360; a += step) {
            double rad = Math.toRadians(a);
            int px = cx + (int) (r * Math.cos(rad));
            int py = cy + (int) (r * Math.sin(rad));
            gui.fill(px, py, px + 1, py + 1, rgba);
        }
    }

    private static void drawRotatedCircle(GuiGraphics gui, int cx, int cy, int r, int color, double cosYaw, double sinYaw) {
        if (r <= 0) return;
        int step = 2;
        int aR = (color >> 24) & 0xFF;
        int rR = (color >> 16) & 0xFF;
        int gR = (color >> 8) & 0xFF;
        int bR = color & 0xFF;
        int rgba = (aR << 24) | (rR << 16) | (gR << 8) | bR;
        for (int a = 0; a < 360; a += step) {
            double rad = Math.toRadians(a);
            double px = r * Math.cos(rad);
            double py = r * Math.sin(rad);
            double rx = px * cosYaw - py * sinYaw;
            double ry = px * sinYaw + py * cosYaw;
            gui.fill(cx + (int) rx, cy + (int) ry, cx + (int) rx + 1, cy + (int) ry + 1, rgba);
        }
    }

    private static void drawDot(GuiGraphics gui, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++)
            for (int x = -r; x <= r; x++)
                if (x * x + y * y <= r * r)
                    gui.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
    }

    private static void drawGlowDot(GuiGraphics gui, int cx, int cy, int r, int color) {
        for (int y = -r * 2; y <= r * 2; y++)
            for (int x = -r * 2; x <= r * 2; x++)
                if (x * x + y * y <= r * r * 4) {
                    double d = Math.sqrt(x * x + y * y) / (r * 2);
                    int baseColor = color | 0xFF000000;
                    int a = (int) ((1.0 - d) * 80);
                    if (x * x + y * y <= r * r) a = 255;
                    if (a <= 0) continue;
                    int fc = (Math.min(255, a) << 24) | ((baseColor >> 16) & 0xFF) << 16 | ((baseColor >> 8) & 0xFF) << 8 | (baseColor & 0xFF);
                    gui.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, fc);
                }
    }

    private static void drawTriangle(GuiGraphics gui, int cx, int cy, float dx, float dy, int color) {
        float nx = -dy;
        float ny = dx;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.1f) return;
        float mx = dx / len;
        float my = dy / len;
        float px = nx / len * 3;
        float py = ny / len * 3;
        int x1 = cx + (int) (mx * 7);
        int y1 = cy + (int) (my * 7);
        int x2 = cx + (int) (-mx * 4 + px);
        int y2 = cy + (int) (-my * 4 + py);
        int x3 = cx + (int) (-mx * 4 - px);
        int y3 = cy + (int) (-my * 4 - py);
        fillTriangle(gui, x1, y1, x2, y2, x3, y3, color);
    }

    private static void fillTriangle(GuiGraphics gui, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int w0 = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);
                int w1 = (x3 - x2) * (y - y2) - (y3 - y2) * (x - x2);
                int w2 = (x1 - x3) * (y - y3) - (y1 - y3) * (x - x3);
                boolean inside = (w0 >= 0 && w1 >= 0 && w2 >= 0) || (w0 <= 0 && w1 <= 0 && w2 <= 0);
                if (inside) gui.fill(x, y, x + 1, y + 1, color);
            }
        }
    }
}
