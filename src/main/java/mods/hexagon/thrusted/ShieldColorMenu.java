package mods.hexagon.thrusted;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class ShieldColorMenu extends AbstractContainerMenu {

    private static final int DATA_SIZE = 19;

    private final ContainerData data;
    private BlockPos pos = BlockPos.ZERO;

    public ShieldColorMenu(int containerId, Inventory inv) {
        super(Thrusted.SHIELD_COLOR_MENU.get(), containerId);
        this.data = new SimpleContainerData(DATA_SIZE);
        addDataSlots(this.data);
    }

    public void init(BlockPos pos, int color, int alpha, boolean glowing, boolean whole, int arcDegrees,
                     int rotX, int rotY, int rotZ,
                     int offX, int offY, int offZ,
                     int sizeX, int sizeY, int sizeZ, boolean onShip, boolean beamEnabled, int multiblockRadius,
                     int entityFilterFlags, boolean showFriendlyNames) {
        this.pos = pos;
        this.data.set(0, pos.getX());
        this.data.set(1, pos.getY());
        this.data.set(2, pos.getZ());
        this.data.set(3, color);
        this.data.set(4, alpha);
        int flags = (glowing ? 1 : 0) | (whole ? 2 : 0) | (onShip ? 4 : 0) | (beamEnabled ? 8 : 0) | (showFriendlyNames ? 16 : 0);
        this.data.set(5, flags);
        this.data.set(6, arcDegrees);
        this.data.set(7, rotX);
        this.data.set(8, rotY);
        this.data.set(9, rotZ);
        this.data.set(10, offX);
        this.data.set(11, offY);
        this.data.set(12, offZ);
        this.data.set(13, sizeX);
        this.data.set(14, sizeY);
        this.data.set(15, sizeZ);
        this.data.set(16, multiblockRadius);
        this.data.set(17, entityFilterFlags);
        this.data.set(18, showFriendlyNames ? 1 : 0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int channel = (id >> 24) & 0xFF;
        int value = id & 0xFFFFFF;
        if ((value & 0x800000) != 0) value |= 0xFF000000;

        if (!(player.level().getBlockEntity(this.pos) instanceof ShieldGeneratorBlockEntity be))
            return true;

        switch (channel) {
            case 0 -> {
                this.data.set(3, value);
                be.setShieldColor(value);
            }
            case 1 -> {
                this.data.set(4, value & 0xFF);
                be.setBaseAlpha(value & 0xFF);
            }
            case 2 -> {
                this.data.set(5, value);
                be.setGlowing((value & 1) != 0);
                be.setWhole((value & 2) != 0);
                be.setBeamEnabled((value & 8) != 0);
            }
            case 3 -> {
                int deg = Math.max(0, Math.min(360, value));
                this.data.set(6, deg);
                be.setArcDegrees(deg);
            }
            case 4 -> {
                int v = Math.max(0, Math.min(3600, value));
                this.data.set(7, v);
                be.setRotX(v / 10f);
            }
            case 5 -> {
                int v = Math.max(0, Math.min(3600, value));
                this.data.set(8, v);
                be.setRotY(v / 10f);
            }
            case 6 -> {
                int v = Math.max(0, Math.min(3600, value));
                this.data.set(9, v);
                be.setRotZ(v / 10f);
            }
            case 7 -> {
                this.data.set(10, value);
                be.setOffX(value / 100.0);
            }
            case 8 -> {
                this.data.set(11, value);
                be.setOffY(value / 100.0);
            }
            case 9 -> {
                this.data.set(12, value);
                be.setOffZ(value / 100.0);
            }
            case 10 -> {
                int v = Math.max(0, value);
                this.data.set(13, v);
                be.setCustomRx(v / 100.0);
            }
            case 11 -> {
                int v = Math.max(0, value);
                this.data.set(14, v);
                be.setCustomRy(v / 100.0);
            }
            case 12 -> {
                int v = Math.max(0, value);
                this.data.set(15, v);
                be.setCustomRz(v / 100.0);
            }
            case 20 -> {
                this.data.set(17, value);
                be.setEntityFilterFlags(value);
            }
            case 23 -> {
                this.data.set(18, value);
                be.setShowFriendlyNames(value != 0);
            }
            case 21 -> {
                var entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.byId(value);
                if (entityType != null) {
                    String key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
                    be.addCustomEntityType(key);
                }
            }
            case 22 -> {
                var entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.byId(value);
                if (entityType != null) {
                    String key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
                    be.removeCustomEntityType(key);
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public int getColor() { return data.get(3); }
    public int getAlpha() { return data.get(4); }
    public int getFlags() { return data.get(5); }
    public int getArcDegrees() { return data.get(6); }
    public int getRotX() { return data.get(7); }
    public int getRotY() { return data.get(8); }
    public int getRotZ() { return data.get(9); }
    public int getOffX() { return data.get(10); }
    public int getOffY() { return data.get(11); }
    public int getOffZ() { return data.get(12); }
    public int getSizeX() { return data.get(13); }
    public int getSizeY() { return data.get(14); }
    public int getSizeZ() { return data.get(15); }
    public int getMultiblockRadius() { return data.get(16); }
    public int getEntityFilterFlags() { return data.get(17); }
    public boolean getShowFriendlyNames() { return data.get(18) != 0; }
    public BlockPos getPos() { return new BlockPos(data.get(0), data.get(1), data.get(2)); }
}
