package mods.hexagon.thrusted.space.nav;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NavScreen extends Screen {
    private final SolarSystemRenderer solarSystemRenderer = new SolarSystemRenderer();

    private float zoom = 1.0f;
    private float rotX = 30.0f;
    private float rotY = 0.0f;
    private float lastMouseX, lastMouseY;
    private boolean dragging = false;

    public NavScreen() {
        super(Component.translatable("nav.turbotech.solar_system"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int navSize = Math.min(width, height) - 40;
        int navX = cx - navSize / 2;
        int navY = 30;

        gui.fill(navX, navY, navX + navSize, navY + navSize, 0xCC0A0A1A);
        gui.enableScissor(navX, navY, navX + navSize, navY + navSize);

        solarSystemRenderer.render(gui, navX + navSize / 2, navY + navSize / 2,
                mouseX, mouseY, zoom, rotX, rotY);

        gui.disableScissor();

        renderOverlay(gui);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderOverlay(GuiGraphics gui) {
        gui.drawString(font, Component.translatable("nav.turbotech.zoom", String.format("%.1fx", zoom)),
                10, 10, 0xAAAAAA);
        gui.drawString(font, Component.translatable("nav.turbotech.controls"),
                10, height - 30, 0x666666);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            rotY += dragX * 0.5f;
            rotX += dragY * 0.5f;
            rotX = Math.max(0, Math.min(90, rotX));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        zoom *= (float) (1.0 - scrollY * 0.1);
        zoom = Math.max(0.1f, Math.min(10.0f, zoom));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
