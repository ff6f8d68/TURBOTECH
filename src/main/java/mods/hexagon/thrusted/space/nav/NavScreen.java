package mods.hexagon.thrusted.space.nav;

import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.OrbitManager;
import mods.hexagon.thrusted.space.environment.SpaceWeatherManager;
import mods.hexagon.thrusted.space.resource.SpaceResource;
import mods.hexagon.thrusted.space.resource.SpaceResourceRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Vector3d;

import java.util.List;

public class NavScreen extends Screen {
    private final SolarSystemRenderer solarSystemRenderer = new SolarSystemRenderer();

    private float zoom = 1.0f;
    private float rotX = 30.0f;
    private float rotY = 0.0f;
    private float lastMouseX, lastMouseY;
    private boolean dragging = false;

    private CelestialBody selectedBody = null;
    private CelestialBody hoveredBody = null;
    private Tab currentTab = Tab.MAP;

    private enum Tab {
        MAP, INFO, TRANSFER, RESOURCES, WEATHER
    }

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

        renderTabs(gui, mouseX, mouseY);

        switch (currentTab) {
            case MAP -> renderMapTab(gui, mouseX, mouseY, partialTick);
            case INFO -> renderInfoTab(gui);
            case TRANSFER -> renderTransferTab(gui);
            case RESOURCES -> renderResourcesTab(gui);
            case WEATHER -> renderWeatherTab(gui);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderTabs(GuiGraphics gui, int mouseX, int mouseY) {
        int tabY = 5;
        int tabWidth = 70;
        int tabX = 10;

        for (Tab tab : Tab.values()) {
            boolean active = tab == currentTab;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= tabY && mouseY <= tabY + 16;
            int bgColor = active ? 0xCC3355AA : (hovered ? 0xCC224488 : 0xCC111133);
            gui.fill(tabX, tabY, tabX + tabWidth, tabY + 16, bgColor);
            gui.drawString(font, tab.name(), tabX + 4, tabY + 4,
                    active ? 0xFFFFFF : 0xAAAAAA, false);
            tabX += tabWidth + 4;
        }
    }

    private void renderMapTab(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2;
        int navSize = Math.min(width, height) - 60;
        int navX = cx - navSize / 2;
        int navY = 30;

        gui.fill(navX, navY, navX + navSize, navY + navSize, 0xCC0A0A1A);
        gui.enableScissor(navX, navY, navX + navSize, navY + navSize);

        solarSystemRenderer.render(gui, navSize, navSize,
                mouseX - navX, mouseY - navY, zoom, rotX, rotY);

        gui.disableScissor();

        renderMapOverlay(gui, navX, navY, navSize);
        renderBodyTooltip(gui, mouseX, mouseY);
    }

    private void renderMapOverlay(GuiGraphics gui, int navX, int navY, int navSize) {
        gui.drawString(font, Component.literal(String.format("Zoom: %.1fx", zoom)),
                navX + 5, navY + 5, 0xAAAAAA, false);
        gui.drawString(font, Component.literal("LMB: Rotate | Scroll: Zoom | RMB: Select"),
                navX + 5, navY + navSize - 12, 0x666666, false);

        if (selectedBody != null) {
            int infoX = navX + navSize + 5;
            if (infoX + 150 > width) infoX = navX - 155;
            renderBodyInfo(gui, selectedBody, infoX, navY);
        }
    }

    private void renderBodyInfo(GuiGraphics gui, CelestialBody body, int x, int y) {
        int w = 150;
        gui.fill(x, y, x + w, y + 160, 0xDD0A0A2A);

        int lineY = y + 5;
        gui.drawString(font, body.getName(), x + 5, lineY, body.getColor(), false);
        lineY += 12;
        gui.drawString(font, body.getType().name(), x + 5, lineY, 0x888888, false);
        lineY += 14;

        gui.drawString(font, "Gravity: " + String.format("%.3fg", body.getGravity().surfaceGravityG()),
                x + 5, lineY, 0xAAFFAA, false);
        lineY += 10;
        gui.drawString(font, "Temp: " + String.format("%.0fK", body.getAtmosphere().surfaceTemperatureK()),
                x + 5, lineY, 0xFFAAAA, false);
        lineY += 10;
        gui.drawString(font, "Pressure: " + String.format("%.3f atm", body.getAtmosphere().pressureAtm()),
                x + 5, lineY, 0xAAAAFF, false);
        lineY += 10;
        gui.drawString(font, "Radiation: " + String.format("%.1f", body.getAtmosphere().radiationLevel()),
                x + 5, lineY, 0xFFFF88, false);
        lineY += 14;

        if (body.getAtmosphere().breathable()) {
            gui.drawString(font, "\u2713 Breathable", x + 5, lineY, 0x55FF55, false);
        } else {
            gui.drawString(font, "\u2717 Not Breathable", x + 5, lineY, 0xFF5555, false);
        }
        lineY += 10;

        if (body.isLandable()) {
            gui.drawString(font, "\u2713 Landable", x + 5, lineY, 0x55FF55, false);
        } else {
            gui.drawString(font, "\u2717 Cannot Land", x + 5, lineY, 0xFF5555, false);
        }
        lineY += 14;

        String desc = body.getDescription();
        if (desc.length() > 25) desc = desc.substring(0, 25) + "...";
        gui.drawString(font, desc, x + 5, lineY, 0x888888, false);
    }

    private void renderBodyTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        if (hoveredBody != null && hoveredBody != selectedBody) {
            gui.drawString(font, hoveredBody.getName(), mouseX + 8, mouseY - 10, hoveredBody.getColor(), false);
        }
    }

    private void renderInfoTab(GuiGraphics gui) {
        int y = 30;
        gui.drawString(font, "Solar System Encyclopedia", 10, y, 0xFFFFFF, false);
        y += 16;

        for (CelestialBody body : CelestialBodyRegistry.getAll()) {
            if (y > height - 20) break;
            int color = body == selectedBody ? 0xFFFFFF : body.getColor();
            String entry = body.getName() + " [" + body.getType().name() + "]";
            gui.drawString(font, entry, 15, y, color, false);
            y += 11;
        }
    }

    private void renderTransferTab(GuiGraphics gui) {
        int y = 30;
        gui.drawString(font, "Transfer Orbit Calculator", 10, y, 0xFFFFFF, false);
        y += 16;

        if (selectedBody == null) {
            gui.drawString(font, "Select a body on the MAP tab first", 15, y, 0xAAAAAA, false);
            return;
        }

        gui.drawString(font, "From: Earth", 15, y, 0x4A8FE4, false);
        y += 12;
        gui.drawString(font, "To: " + selectedBody.getName(), 15, y, selectedBody.getColor(), false);
        y += 16;

        var transfer = TransferOrbitCalculator.calculateTransfer("Earth", selectedBody.getName());
        if (transfer != null) {
            gui.drawString(font, String.format("Delta-V: %.3f (%.3f + %.3f)",
                    transfer.totalDeltaV(), transfer.deltaV1(), transfer.deltaV2()),
                    15, y, 0xAAFFAA, false);
            y += 12;
            gui.drawString(font, String.format("Transfer Time: %d ticks (%.1f days)",
                    transfer.transferTimeTicks(), transfer.transferTimeTicks() / 24000.0),
                    15, y, 0xAAAAFF, false);
            y += 12;
            gui.drawString(font, transfer.description(), 15, y, 0x888888, false);
        } else {
            gui.drawString(font, "Cannot calculate transfer", 15, y, 0xFF5555, false);
        }
    }

    private void renderResourcesTab(GuiGraphics gui) {
        int y = 30;
        gui.drawString(font, "Planetary Resources", 10, y, 0xFFFFFF, false);
        y += 16;

        if (selectedBody == null) {
            gui.drawString(font, "Select a body on the MAP tab first", 15, y, 0xAAAAAA, false);
            return;
        }

        gui.drawString(font, selectedBody.getName() + " Resources:", 15, y, selectedBody.getColor(), false);
        y += 14;

        List<SpaceResource> resources = SpaceResourceRegistry.getResourcesFor(selectedBody.getName());
        if (resources.isEmpty()) {
            gui.drawString(font, "No known resources", 20, y, 0x888888, false);
        } else {
            for (SpaceResource res : resources) {
                if (y > height - 20) break;
                String rarity = res.baseRarity() > 0.5 ? "Common" : res.baseRarity() > 0.1 ? "Uncommon" : "Rare";
                gui.drawString(font, "\u2022 " + res.displayName() + " [" + rarity + "]",
                        20, y, res.color(), false);
                y += 11;
            }
        }
    }

    private void renderWeatherTab(GuiGraphics gui) {
        int y = 30;
        gui.drawString(font, "Space Weather Monitor", 10, y, 0xFFFFFF, false);
        y += 16;

        var event = SpaceWeatherManager.getCurrentEvent();
        if (SpaceWeatherManager.isEventActive()) {
            gui.drawString(font, "ACTIVE EVENT:", 15, y, 0xFF3333, false);
            y += 12;
            gui.drawString(font, event.displayName, 20, y, event.color, false);
            y += 12;
            int remaining = SpaceWeatherManager.getEventTicksRemaining();
            gui.drawString(font, String.format("Time remaining: %d:%02d",
                    remaining / 1200, (remaining % 1200) / 20),
                    20, y, 0xAAAAAA, false);
            y += 12;
            gui.drawString(font, String.format("Radiation: x%.1f", event.radiationMultiplier),
                    20, y, 0xFFFF88, false);
            y += 12;
            gui.drawString(font, String.format("Debris: %.0f%%", event.debrisIntensity * 100),
                    20, y, 0xAADDFF, false);
        } else {
            gui.drawString(font, "Status: CLEAR", 15, y, 0x55FF55, false);
            y += 12;
            gui.drawString(font, "No active space weather events", 20, y, 0x888888, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tabY = 5;
        int tabWidth = 70;
        int tabX = 10;

        if (mouseY >= tabY && mouseY <= tabY + 16) {
            for (Tab tab : Tab.values()) {
                if (mouseX >= tabX && mouseX <= tabX + tabWidth) {
                    currentTab = tab;
                    return true;
                }
                tabX += tabWidth + 4;
            }
        }

        if (button == 1 && currentTab == Tab.MAP) {
            selectBodyAtMouse(mouseX, mouseY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectBodyAtMouse(double mouseX, double mouseY) {
        float effectiveScale = 0.15f * zoom;
        int cx = width / 2;
        int cy = height / 2;

        CelestialBody closest = null;
        double closestDist = 20;

        for (CelestialBody body : CelestialBodyRegistry.getAll()) {
            Vector3d pos = OrbitManager.getPosition(body.getName());
            float bx = cx + (float)(pos.x * effectiveScale);
            float by = cy + (float)(pos.z * effectiveScale);

            double dist = Math.sqrt((mouseX - bx) * (mouseX - bx) + (mouseY - by) * (mouseY - by));
            if (dist < closestDist) {
                closestDist = dist;
                closest = body;
            }
        }
        selectedBody = closest;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && currentTab == Tab.MAP) {
            rotY += dragX * 0.5f;
            rotX += dragY * 0.5f;
            rotX = Math.max(0, Math.min(90, rotX));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentTab == Tab.MAP) {
            zoom *= (float) (1.0 - scrollY * 0.1);
            zoom = Math.max(0.05f, Math.min(20.0f, zoom));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
