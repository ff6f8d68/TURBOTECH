package mods.hexagon.thrusted.space.nav;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public class NavKeybind {
    public static final KeyMapping NAV_KEY = new KeyMapping(
            "key.turbotech.nav_map",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_N, -1),
            "key.categories.turbotech"
    );

    private static boolean wasPressed = false;

    public static void handleTick() {
        boolean pressed = NAV_KEY.isDown();
        if (pressed && !wasPressed) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof NavScreen) {
                mc.setScreen(null);
            } else {
                mc.setScreen(new NavScreen());
            }
        }
        wasPressed = pressed;
    }
}
