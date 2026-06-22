package mods.hexagon.thrusted.space.nav;

import mods.hexagon.thrusted.Thrusted;
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

        gui.drawString(mc.font, formatDimensionName(dimName), 10, 10, 0xFFFFFF);
        gui.drawString(mc.font, String.format("XYZ: %.0f / %.0f / %.0f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ()), 10, 22, 0xAAAAAA);

        String body = dimName.replace("planet_", "").replace("orbit_", "");
        gui.drawString(mc.font, "Near: " + capitalize(body), 10, 34, 0x888888);
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
