package mods.hexagon.thrusted.space.nav;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.SpaceDimensions;
import mods.hexagon.thrusted.space.environment.GravityManager;
import mods.hexagon.thrusted.space.environment.LifeSupportManager;
import mods.hexagon.thrusted.space.environment.ReentryManager;
import mods.hexagon.thrusted.space.environment.SpaceWeatherManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class NavHudOverlay implements LayeredDraw.Layer {
    public static final NavHudOverlay INSTANCE = new NavHudOverlay();

    @Override
    public void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ResourceKey<Level> dim = mc.level.dimension();
        String dimName = dim.location().getNamespace().equals(Thrusted.MODID)
                ? dim.location().getPath() : null;
        if (dimName == null) return;

        if (!dimName.startsWith("planet_") && !dimName.startsWith("orbit_")) return;

        int y = 10;
        int x = 10;

        gui.drawString(mc.font, formatDimensionName(dimName), x, y, 0xFFFFFF);
        y += 12;

        gui.drawString(mc.font, String.format("XYZ: %.0f / %.0f / %.0f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ()), x, y, 0xAAAAAA);
        y += 12;

        String bodyName = dimName.replace("planet_", "").replace("orbit_", "");
        CelestialBody body = CelestialBodyRegistry.get(capitalize(bodyName));
        if (body != null) {
            gui.drawString(mc.font, "Near: " + body.getName(), x, y, body.getColor());
            y += 10;
            gui.drawString(mc.font, String.format("G: %.3f | T: %.0fK",
                    body.getGravity().surfaceGravityG(),
                    body.getAtmosphere().surfaceTemperatureK()), x, y, 0x888888);
            y += 12;
        }

        double speed = mc.player.getDeltaMovement().length() * 20;
        gui.drawString(mc.font, String.format("Speed: %.1f m/s", speed), x, y, 0x88CCFF);
        y += 14;

        var lifeSupport = LifeSupportManager.getPlayerData(mc.player.getUUID());
        renderLifeSupportBar(gui, x, y, "O2", lifeSupport.oxygenLevel, 1200.0, 0x55FF55, 0xFF3333);
        y += 10;
        renderLifeSupportBar(gui, x, y, "RAD", lifeSupport.radiationLevel, 100.0, 0xFFFF55, 0xFF3333);
        y += 14;

        if (SpaceWeatherManager.isEventActive()) {
            var event = SpaceWeatherManager.getCurrentEvent();
            gui.drawString(mc.font, "\u26A0 " + event.displayName, x, y, event.color);
            y += 10;
        }

        if (ReentryManager.isReentering(mc.player.getUUID())) {
            var state = ReentryManager.getState(mc.player.getUUID());
            int heatColor = state.heatLevel > 60 ? 0xFF3333 : state.heatLevel > 30 ? 0xFFAA00 : 0xFFDD00;
            gui.drawString(mc.font, String.format("\uD83D\uDD25 HEAT: %.0f%%", state.heatLevel), x, y, heatColor);
        }
    }

    private void renderLifeSupportBar(GuiGraphics gui, int x, int y, String label,
                                       double current, double max, int goodColor, int badColor) {
        Minecraft mc = Minecraft.getInstance();
        double ratio = current / max;
        int color = ratio > 0.5 ? goodColor : (ratio > 0.2 ? 0xFFFF55 : badColor);

        gui.drawString(mc.font, String.format("%s: %.0f%%", label, ratio * 100), x, y, color);

        int barX = x + 55;
        int barW = 50;
        int barH = 6;
        gui.fill(barX, y + 1, barX + barW, y + 1 + barH, 0xFF222222);
        int fillW = (int)(barW * ratio);
        if (fillW > 0) {
            gui.fill(barX, y + 1, barX + fillW, y + 1 + barH, 0xFF000000 | color);
        }
    }

    private static String formatDimensionName(String name) {
        if (name.startsWith("planet_")) return "\uD83C\uDF0D " + capitalize(name.substring(7));
        if (name.startsWith("orbit_")) return "\uD83D\uDD30 " + capitalize(name.substring(6)) + " Orbit";
        return name;
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
