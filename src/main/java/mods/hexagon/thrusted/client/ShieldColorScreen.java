package mods.hexagon.thrusted.client;
import mods.hexagon.thrusted.Thrusted;

import mods.hexagon.thrusted.blockentity.ShieldGeneratorBlockEntity;
import mods.hexagon.thrusted.menu.ShieldColorMenu;
import mods.hexagon.thrusted.network.FriendlyNamePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.stream.Collectors;

public class ShieldColorScreen extends Screen implements MenuAccess<ShieldColorMenu> {

    private static final int SLIDER_W = 200;
    private static final int SLIDER_H = 20;
    private static final int GAP = 22;

    private final ShieldColorMenu menu;
    private int r, g, b, alpha = 255;
    private boolean glowing = true, whole = true, onShip = false, beam = true;
    private int arcDegrees = 180;
    private int rotX, rotY, rotZ;
    private int offX, offY, offZ;
    private int sizeX, sizeY, sizeZ;

    private int page = 0;
    private int scrollY = 0;
    private int contentHeightP1;
    private int contentHeightP2;
    private int multiblockRadius = 20;

    private ColorSlider sliderR, sliderG, sliderB, sliderA;
    private Checkbox glowingCheck, wholeCheck;
    private int previewX, previewY, previewSize = 40;
    private boolean synced = false;
    private boolean reloading = false;

    // Page 2 fields
    private int entityFilterFlags;
    private EditBox searchBox;
    private String searchText = "";
    private int entityScroll = 0;
    private List<EntityTypeEntry> allEntityTypes;
    private boolean entityTypesLoaded = false;
    private Set<String> customEntityTypes = new HashSet<>();

    // Page 3 fields
    private EditBox friendlyNameBox;
    private Set<String> friendlyPlayerNames = new HashSet<>();
    private int friendlyPlayerScroll = 0;
    private int contentHeightP3;

    private record EntityTypeEntry(String id, int registryId) {}

    public ShieldColorScreen(ShieldColorMenu menu, Inventory inv, Component title) {
        super(title);
        this.menu = menu;
        loadInitial();
    }

    private void loadInitial() {
        int color = menu.getColor();
        if (color == 0 && Minecraft.getInstance().level != null) {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
            if (be instanceof ShieldGeneratorBlockEntity sbe) {
                color = sbe.getShieldColor();
            }
        }
        if (color == 0) color = 0x3366D9;
        this.r = (color >> 16) & 0xFF;
        this.g = (color >> 8) & 0xFF;
        this.b = color & 0xFF;
        this.alpha = clamp(menu.getAlpha(), 255);
        int flags = menu.getFlags();
        this.glowing = (flags & 1) != 0;
        this.whole = (flags & 2) != 0;
        this.onShip = (flags & 4) != 0;
        this.beam = (flags & 8) != 0;
        this.arcDegrees = menu.getArcDegrees();
        this.rotX = menu.getRotX();
        this.rotY = menu.getRotY();
        this.rotZ = menu.getRotZ();
        this.offX = menu.getOffX();
        this.offY = menu.getOffY();
        this.offZ = menu.getOffZ();
        this.sizeX = menu.getSizeX();
        this.sizeY = menu.getSizeY();
        this.sizeZ = menu.getSizeZ();
        int mr = menu.getMultiblockRadius();
        this.multiblockRadius = mr > 0 ? mr : 20;
        this.entityFilterFlags = menu.getEntityFilterFlags();
        loadCustomEntityTypes();
        loadFriendlyPlayerNames();
    }

    private void loadCustomEntityTypes() {
        customEntityTypes.clear();
        if (Minecraft.getInstance().level == null) return;
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
        if (be instanceof ShieldGeneratorBlockEntity sbe) {
            customEntityTypes.addAll(sbe.getCustomEntityTypes());
        }
    }

    private void loadFriendlyPlayerNames() {
        friendlyPlayerNames.clear();
        if (Minecraft.getInstance().level == null) return;
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(menu.getPos());
        if (be instanceof ShieldGeneratorBlockEntity sbe) {
            friendlyPlayerNames.addAll(sbe.getFriendlyPlayerNames());
        }
    }

    private void ensureEntityTypesLoaded() {
        if (entityTypesLoaded) return;
        allEntityTypes = new ArrayList<>();
        var registry = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
        for (var entry : registry.entrySet()) {
            var key = registry.getKey(entry.getValue());
            int id = registry.getId(entry.getValue());
            allEntityTypes.add(new EntityTypeEntry(key.toString(), id));
        }
        allEntityTypes.sort(Comparator.comparing(a -> a.id));
        entityTypesLoaded = true;
    }

    private List<EntityTypeEntry> getFilteredEntities() {
        if (allEntityTypes == null) return List.of();
        if (searchText.isEmpty()) return allEntityTypes;
        String lower = searchText.toLowerCase(Locale.ROOT);
        return allEntityTypes.stream()
                .filter(e -> e.id.toLowerCase(Locale.ROOT).contains(lower))
                .collect(Collectors.toList());
    }

    private static int clamp(int v, int def) {
        return v == 0 ? def : v;
    }

    @Override
    protected void init() {
        rebuildPage();
    }

    protected void rebuildPage() {
        if (reloading) return;
        reloading = true;
        clearWidgets();

        int cx = width / 2;
        int left = cx - SLIDER_W / 2;
        int startY = 40;

        if (page == 0) {
            buildPage0(cx, left, startY);
        } else if (page == 1) {
            buildPage1(cx, left, startY);
        } else if (page == 2) {
            buildPage2(cx, left, startY);
        } else if (page == 3) {
            buildPage3(cx, left, startY);
        }

        reloading = false;
    }

    private void buildPage0(int cx, int left, int startY) {
        sliderR = new ColorSlider(left, startY, SLIDER_W, SLIDER_H, r / 255.0, 0, "R");
        sliderG = new ColorSlider(left, startY + GAP, SLIDER_W, SLIDER_H, g / 255.0, 1, "G");
        sliderB = new ColorSlider(left, startY + GAP * 2, SLIDER_W, SLIDER_H, b / 255.0, 2, "B");
        sliderA = new ColorSlider(left, startY + GAP * 3, SLIDER_W, SLIDER_H, alpha / 255.0, 3, "A");
        addRenderableWidget(sliderR);
        addRenderableWidget(sliderG);
        addRenderableWidget(sliderB);
        addRenderableWidget(sliderA);

        int cbY = startY + GAP * 4 + 4;
        glowingCheck = addRenderableWidget(Checkbox.builder(Component.literal("Glowing"), font)
                .pos(left, cbY)
                .selected(glowing)
                .onValueChange((cb, val) -> {
                    glowing = val;
                    sendFlags();
                })
                .build());

        wholeCheck = addRenderableWidget(Checkbox.builder(Component.literal("Whole"), font)
                .pos(left + 110, cbY)
                .selected(whole)
                .onValueChange((cb, val) -> {
                    whole = val;
                    sendFlags();
                    rebuildPage();
                })
                .build());

        if (!whole) {
            addRenderableWidget(Button.builder(Component.literal("Custom \u00BB"), btn -> {
                page = 1;
                scrollY = 0;
                rebuildPage();
            }).bounds(left + 220, cbY, 60, 20).build());
        }

        addRenderableWidget(Checkbox.builder(Component.literal("Beam"), font)
                .pos(left, cbY + GAP)
                .selected(beam)
                .onValueChange((cb, val) -> {
                    beam = val;
                    sendFlags();
                })
                .build());

        addRenderableWidget(Button.builder(Component.literal("Entities \u00BB"), btn -> {
            page = 2;
            scrollY = 0;
            entityScroll = 0;
            rebuildPage();
        }).bounds(left + 220, cbY + GAP, 60, 20).build());

        previewX = cx - previewSize / 2;
        previewY = startY + GAP * 4 + 50;
    }

    private void buildPage1(int cx, int left, int startY) {
        int backEnd = startY + 20;
        addRenderableWidget(Button.builder(Component.literal("\u00AB Back"), btn -> {
            page = 0;
            rebuildPage();
        }).bounds(left, startY, 60, 20).build());

        int scrollTop = startY + 30;
        int scrollBot = height - previewSize - 20;
        int visibleH = scrollBot - scrollTop;

        int cy = startY + 30 - scrollY;

        addRenderableWidget(new ColorSlider(left, cy, SLIDER_W, SLIDER_H, arcDegrees / 360.0, 4, "Arc"));
        cy += GAP;
        addRenderableWidget(new ColorSlider(left, cy, SLIDER_W, SLIDER_H, rotX / 3600.0, 5, "RotX"));
        addRenderableWidget(new ColorSlider(left, cy + GAP, SLIDER_W, SLIDER_H, rotY / 3600.0, 6, "RotY"));
        addRenderableWidget(new ColorSlider(left, cy + GAP * 2, SLIDER_W, SLIDER_H, rotZ / 3600.0, 7, "RotZ"));
        cy += GAP * 3 + 2;
        addRenderableWidget(new ColorSlider(left, cy, SLIDER_W, SLIDER_H, (offX + 10000) / 20000.0, 8, "OffX"));
        addRenderableWidget(new ColorSlider(left, cy + GAP, SLIDER_W, SLIDER_H, (offY + 10000) / 20000.0, 9, "OffY"));
        addRenderableWidget(new ColorSlider(left, cy + GAP * 2, SLIDER_W, SLIDER_H, (offZ + 10000) / 20000.0, 10, "OffZ"));
        cy += GAP * 3 + 2;
        double maxSz = multiblockRadius * 100.0;
        addRenderableWidget(new ColorSlider(left, cy, SLIDER_W, SLIDER_H, Math.min(sizeX, (int)maxSz) / maxSz, 11, "SizeX"));
        addRenderableWidget(new ColorSlider(left, cy + GAP, SLIDER_W, SLIDER_H, Math.min(sizeY, (int)maxSz) / maxSz, 12, "SizeY"));
        addRenderableWidget(new ColorSlider(left, cy + GAP * 2, SLIDER_W, SLIDER_H, Math.min(sizeZ, (int)maxSz) / maxSz, 13, "SizeZ"));
        cy += GAP * 3 + 10;

        contentHeightP1 = cy - (startY + 30 - scrollY) + 30;

        previewX = cx - previewSize / 2;
        previewY = height - previewSize - 10;
    }

    private void buildPage2(int cx, int left, int startY) {
        int backEnd = startY + 20;
        addRenderableWidget(Button.builder(Component.literal("\u00AB Back"), btn -> {
            page = 0;
            rebuildPage();
        }).bounds(left, startY, 60, 20).build());

        entityFilterFlags = menu.getEntityFilterFlags();

        int y = startY + 30 - scrollY;

        // Group checkboxes
        y = addGroupCheckbox(left, y, "Projectiles", ShieldGeneratorBlockEntity.FILTER_PROJECTILES);
        y = addGroupCheckbox(left, y, "Mobs", ShieldGeneratorBlockEntity.FILTER_MOBS);
        y = addGroupCheckbox(left, y, "Players", ShieldGeneratorBlockEntity.FILTER_PLAYERS);
        y = addGroupCheckbox(left, y, "Vehicles", ShieldGeneratorBlockEntity.FILTER_VEHICLES);
        y = addGroupCheckbox(left, y, "Misc", ShieldGeneratorBlockEntity.FILTER_MISC);
        y += 6;

        // Friendly player names sub-page button
        addRenderableWidget(Button.builder(Component.literal("Friendly Players \u00BB"), btn -> {
            page = 3;
            scrollY = 0;
            friendlyPlayerScroll = 0;
            rebuildPage();
        }).bounds(left, y, 130, 18).build());
        y += 22;

        int checkboxesEnd = y;

        // Search box
        searchBox = addRenderableWidget(new EditBox(font, left, y, SLIDER_W, 18, Component.literal("Search")));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchText);
        searchBox.setResponder(s -> { searchText = s; entityScroll = 0; rebuildPage(); });
        y += 22;

        ensureEntityTypesLoaded();
        List<EntityTypeEntry> filtered = getFilteredEntities();

        // Entity list
        int itemH = 13;
        int listTop = y;
        int listBot = height - 12;
        int visible = (listBot - listTop) / itemH;

        int maxScroll = Math.max(0, filtered.size() - visible);
        entityScroll = Math.min(entityScroll, maxScroll);

        int btnW = SLIDER_W;
        for (int i = 0; i < visible && (i + entityScroll) < filtered.size(); i++) {
            EntityTypeEntry entry = filtered.get(i + entityScroll);
            int ey = listTop + i * itemH;
            boolean added = customEntityTypes.contains(entry.id);
            int bg = added ? 0xFF1A4A1A : 0xFF333333;
            int fg = added ? 0xFF55FF55 : 0xFFAAAAAA;
            int border = added ? 0xFF55FF55 : 0xFF0066FF;
            addRenderableWidget(new EntityButton(left + 4, ey, btnW, 11, entry.id, bg, fg, border, b -> {
                if (added) sendRemoveEntityType(entry.registryId);
                else sendAddEntityType(entry.registryId);
            }));
        }

        contentHeightP2 = filtered.size() * itemH + (checkboxesEnd - (startY + 30 - scrollY)) + 60;
    }

    private void buildPage3(int cx, int left, int startY) {
        addRenderableWidget(Button.builder(Component.literal("\u00AB Back"), btn -> {
            page = 2;
            rebuildPage();
        }).bounds(left, startY, 60, 20).build());

        // Edit box + Add button (fixed, above scissor)
        friendlyNameBox = addRenderableWidget(new EditBox(font, left, startY + 30, SLIDER_W - 56, 18, Component.literal("Player name")));
        friendlyNameBox.setMaxLength(16);
        friendlyNameBox.setValue(friendlyNameBox.getValue());
        addRenderableWidget(Button.builder(Component.literal("Add"), btn -> {
            String name = friendlyNameBox.getValue().trim();
            if (!name.isEmpty() && !friendlyPlayerNames.contains(name)) {
                friendlyPlayerNames.add(name);
                sendFriendlyPlayerName(name, true);
                friendlyNameBox.setValue("");
                rebuildPage();
            }
        }).bounds(left + SLIDER_W - 52, startY + 30, 52, 18).build());

        // Scrollable list of names with remove buttons
        int listTop = startY + 56 - friendlyPlayerScroll;
        int itemH = 13;

        List<String> sortedNames = new ArrayList<>(friendlyPlayerNames);
        java.util.Collections.sort(sortedNames);

        int listBot = height - 12;
        int visible = (listBot - (startY + 56)) / itemH;
        int maxScroll = Math.max(0, sortedNames.size() - visible);
        friendlyPlayerScroll = Math.min(friendlyPlayerScroll, maxScroll);

        int btnW = SLIDER_W;
        for (int i = 0; i < visible && (i + friendlyPlayerScroll) < sortedNames.size(); i++) {
            String name = sortedNames.get(i + friendlyPlayerScroll);
            int ey = listTop + i * itemH;
            addRenderableWidget(new EntityButton(left + 4, ey, btnW - 30, 11, name,
                    0xFF333333, 0xFFAAAAAA, 0xFF555555, btn2 -> {}));
            addRenderableWidget(Button.builder(Component.literal("X"), btn -> {
                friendlyPlayerNames.remove(name);
                sendFriendlyPlayerName(name, false);
                rebuildPage();
            }).bounds(left + btnW - 22, ey - 1, 20, 13).build());
        }

        contentHeightP3 = sortedNames.size() * itemH + 60;
    }

    private int addGroupCheckbox(int x, int y, String label, int flag) {
        addRenderableWidget(Checkbox.builder(Component.literal(label), font)
                .pos(x, y)
                .selected((entityFilterFlags & flag) != 0)
                .onValueChange((cb, val) -> {
                    if (val) entityFilterFlags |= flag;
                    else entityFilterFlags &= ~flag;
                    sendEntityFilterFlags();
                }).build());
        return y + 18;
    }

    private class EntityButton extends Button {
        private final int bgColor, fgColor, borderColor;
        EntityButton(int x, int y, int w, int h, String label, int bgColor, int fgColor, int borderColor, OnPress onPress) {
            super(x, y, w, h, Component.literal(label), onPress, DEFAULT_NARRATION);
            this.bgColor = bgColor;
            this.fgColor = fgColor;
            this.borderColor = borderColor;
        }

        @Override
        public void renderWidget(GuiGraphics gui, int mx, int my, float partial) {
            int c = isHoveredOrFocused() ? borderColor : bgColor;
            gui.fill(getX(), getY(), getX() + width, getY() + height, c);
            if (isHoveredOrFocused()) {
                gui.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, bgColor);
            }
            gui.drawString(font, getMessage(), getX() + 3, getY() + 1, fgColor, false);
        }
    }

    private void sendEntityFilterFlags() {
        sendValue(20, entityFilterFlags);
    }

    private void sendFriendlyPlayerName(String name, boolean add) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new FriendlyNamePayload(menu.getPos(), name, add));
    }

    private void sendAddEntityType(int registryId) {
        sendValue(21, registryId);
        var opt = allEntityTypes.stream().filter(e -> e.registryId == registryId).findFirst();
        opt.ifPresent(e -> customEntityTypes.add(e.id));
        rebuildPage();
    }

    private void sendRemoveEntityType(int registryId) {
        sendValue(22, registryId);
        var opt = allEntityTypes.stream().filter(e -> e.registryId == registryId).findFirst();
        opt.ifPresent(e -> customEntityTypes.remove(e.id));
        rebuildPage();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == 1) {
            int scrollTop = 70;
            int scrollBot = height - previewSize - 20;
            int visibleH = scrollBot - scrollTop;
            int maxScroll = Math.max(0, contentHeightP1 - visibleH);
            this.scrollY = (int) Mth.clamp(this.scrollY - scrollY * 20, 0, maxScroll);
            rebuildPage();
            return true;
        } else if (page == 2) {
            int scrollTop = 70;
            int scrollBot = height - 10;
            int visibleH = scrollBot - scrollTop;
            int maxScroll = Math.max(0, contentHeightP2 - visibleH);
            this.scrollY = (int) Mth.clamp(this.scrollY - scrollY * 20, 0, maxScroll);
            rebuildPage();
            return true;
        } else if (page == 3) {
            int listTop = 96;
            int listBot = height - 12;
            int visibleH = listBot - listTop;
            int maxScroll = Math.max(0, contentHeightP3 - visibleH);
            friendlyPlayerScroll = (int) Mth.clamp(friendlyPlayerScroll - (int) scrollY * 20, 0, maxScroll);
            rebuildPage();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        super.tick();
        if (!synced) {
            int color = menu.getColor();
            if (color != 0 || Minecraft.getInstance().level == null) {
                syncFromMenu();
            }
        }
    }

    private void syncFromMenu() {
        int color = menu.getColor();
        if (color == 0 && Minecraft.getInstance().level != null) return;
        if (color == 0) color = 0x3366D9;

        r = (color >> 16) & 0xFF;
        g = (color >> 8) & 0xFF;
        b = color & 0xFF;
        alpha = clamp(menu.getAlpha(), 255);
        int flags = menu.getFlags();
        glowing = (flags & 1) != 0;
        whole = (flags & 2) != 0;
        onShip = (flags & 4) != 0;
        beam = (flags & 8) != 0;
        arcDegrees = menu.getArcDegrees();
        rotX = menu.getRotX();
        rotY = menu.getRotY();
        rotZ = menu.getRotZ();
        offX = menu.getOffX();
        offY = menu.getOffY();
        offZ = menu.getOffZ();
        sizeX = menu.getSizeX();
        sizeY = menu.getSizeY();
        sizeZ = menu.getSizeZ();
        int mr = menu.getMultiblockRadius();
        multiblockRadius = mr > 0 ? mr : 20;
        entityFilterFlags = menu.getEntityFilterFlags();
        loadCustomEntityTypes();
        loadFriendlyPlayerNames();

        rebuildPage();

        if (page == 0) {
            sliderR.setValue(r / 255.0);
            sliderG.setValue(g / 255.0);
            sliderB.setValue(b / 255.0);
            sliderA.setValue(alpha / 255.0);
        }

        synced = true;
    }

    private void sendColor() {
        int packed = (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
        sendValue(0, packed);
    }

    private void sendFlags() {
        int flags = (glowing ? 1 : 0) | (whole ? 2 : 0) | (onShip ? 4 : 0) | (beam ? 8 : 0);
        sendValue(2, flags);
    }

    private void sendValue(int channel, int value) {
        int id = (channel << 24) | (value & 0xFFFFFF);
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        renderBackground(gui, mouseX, mouseY, partial);

        if (page == 1) {
            int scrollBot = height - previewSize - 20;
            gui.enableScissor(0, 70, width, scrollBot);
        } else if (page == 2) {
            gui.enableScissor(0, 70, width, height - 10);
        } else if (page == 3) {
            gui.enableScissor(0, 96, width, height - 10);
        }

        super.render(gui, mouseX, mouseY, partial);

        if (page == 1 || page == 2 || page == 3) {
            gui.disableScissor();
        }

        // Color preview box (pages 0,1)
        if (page == 0 || page == 1) {
            int packed = (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
            int argb = (Mth.clamp(alpha, 0, 255) << 24) | packed;
            gui.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, argb);
            gui.drawCenteredString(font, Component.literal(String.format("#%06X  A:%d", packed, alpha)), width / 2, previewY + previewSize + 4, 0xAAAAAA);
        }

        if (page == 1) {
            gui.drawCenteredString(font, Component.literal("Custom Shield Settings"), width / 2, 20, 0xFFFFFF);
        } else if (page == 2) {
            gui.drawCenteredString(font, Component.literal("Entity Filtering"), width / 2, 20, 0xFFFFFF);
        } else if (page == 3) {
            gui.drawCenteredString(font, Component.literal("Friendly Players"), width / 2, 20, 0xFFFFFF);
            if (friendlyPlayerNames.isEmpty()) {
                gui.drawCenteredString(font, Component.literal("No friendly players added yet"), width / 2, 120, 0x666666);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public ShieldColorMenu getMenu() { return menu; }

    private class ColorSlider extends AbstractSliderButton {
        private final int channel;

        public ColorSlider(int x, int y, int w, int h, double value, int channel, String label) {
            super(x, y, w, h, Component.literal(label + ": " + formatVal(channel, value, ShieldColorScreen.this.multiblockRadius)), value);
            this.channel = channel;
        }

        void setValue(double v) { this.value = v; updateMessage(); }

        private static String formatVal(int ch, double v, int maxBlocks) {
            return switch (ch) {
                case 0, 1, 2 -> String.valueOf((int) (v * 255));
                case 3 -> String.valueOf((int) (v * 255));
                case 4 -> (int) (v * 360) + "\u00B0";
                case 5, 6, 7 -> (int) (v * 360) + "\u00B0";
                case 8, 9, 10 -> String.format("%.1f", v * 200 - 100);
                case 11, 12, 13 -> v < 0.005 ? "Auto" : String.format("%.0f blocks", v * maxBlocks);
                default -> "";
            };
        }

        @Override
        protected void updateMessage() {
            String label = switch (channel) {
                case 0 -> "R"; case 1 -> "G"; case 2 -> "B"; case 3 -> "A";
                case 4 -> "Arc"; case 5 -> "RotX"; case 6 -> "RotY"; case 7 -> "RotZ";
                case 8 -> "OffX"; case 9 -> "OffY"; case 10 -> "OffZ";
                case 11 -> "SizeX (blocks)"; case 12 -> "SizeY (blocks)"; case 13 -> "SizeZ (blocks)";
                default -> "?";
            };
            setMessage(Component.literal(label + ": " + formatVal(channel, value, multiblockRadius)));
        }

        @Override
        protected void applyValue() {
            switch (channel) {
                case 0 -> { r = (int) (value * 255); sendColor(); }
                case 1 -> { g = (int) (value * 255); sendColor(); }
                case 2 -> { b = (int) (value * 255); sendColor(); }
                case 3 -> { int a = (int) (value * 255); alpha = a; sendValue(1, a); }
                case 4 -> { arcDegrees = (int) (value * 360); sendValue(3, arcDegrees); }
                case 5 -> { rotX = (int) (value * 3600); sendValue(4, rotX); }
                case 6 -> { rotY = (int) (value * 3600); sendValue(5, rotY); }
                case 7 -> { rotZ = (int) (value * 3600); sendValue(6, rotZ); }
                case 8 -> { offX = (int) (value * 20000 - 10000); sendValue(7, offX); }
                case 9 -> { offY = (int) (value * 20000 - 10000); sendValue(8, offY); }
                case 10 -> { offZ = (int) (value * 20000 - 10000); sendValue(9, offZ); }
                case 11 -> { sizeX = (int) (value * multiblockRadius * 100); sendValue(10, sizeX); }
                case 12 -> { sizeY = (int) (value * multiblockRadius * 100); sendValue(11, sizeY); }
                case 13 -> { sizeZ = (int) (value * multiblockRadius * 100); sendValue(12, sizeZ); }
            }
        }

        @Override
        public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partial) {
            super.renderWidget(gui, mouseX, mouseY, partial);
            int barColor = switch (channel) {
                case 0 -> 0x80FF0000;
                case 1 -> 0x8000FF00;
                case 2 -> 0x800000FF;
                case 3 -> 0x80FFFFFF;
                case 4 -> 0x8000FFFF;
                case 5, 6, 7 -> 0x80FFAA00;
                case 8, 9, 10 -> 0x80AA00FF;
                case 11, 12, 13 -> 0x8000FFAA;
                default -> 0x80FFFFFF;
            };
            int fillEnd = getX() + (int) (value * (double) width);
            gui.fill(getX() + 1, getY() + 1, fillEnd, getY() + height - 1, barColor);
        }
    }
}
