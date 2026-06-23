package mods.hexagon.thrusted.space.suit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class SpaceSuitItem extends Item {

    private final SuitTier tier;
    private final EquipmentSlot slot;

    public enum SuitTier {
        BASIC("Basic", 0.3, 0.2, 0.3, 0.2, 0.0, 600),
        ADVANCED("Advanced", 0.7, 0.6, 0.7, 0.6, 0.3, 1200),
        ELITE("Elite", 0.95, 0.9, 0.95, 0.9, 0.7, 2400),
        QUANTUM("Quantum", 1.0, 1.0, 1.0, 1.0, 1.0, 6000);

        public final String displayName;
        public final double oxygenSupplyRate;
        public final double radiationShielding;
        public final double thermalProtection;
        public final double pressureProtection;
        public final double heatShielding;
        public final int maxDurability;

        SuitTier(String name, double oxygen, double radiation, double thermal,
                 double pressure, double heat, int durability) {
            this.displayName = name;
            this.oxygenSupplyRate = oxygen;
            this.radiationShielding = radiation;
            this.thermalProtection = thermal;
            this.pressureProtection = pressure;
            this.heatShielding = heat;
            this.maxDurability = durability;
        }
    }

    public SpaceSuitItem(EquipmentSlot slot, SuitTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
        this.slot = slot;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        initializeSuitData(stack);
        return stack;
    }

    public void initializeSuitData(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag suitTag = new CompoundTag();
            suitTag.putString("Tier", tier.name());
            suitTag.putDouble("OxygenSupplyRate", tier.oxygenSupplyRate);
            suitTag.putDouble("RadiationShielding", tier.radiationShielding / 4.0);
            suitTag.putDouble("ThermalProtection", tier.thermalProtection / 4.0);
            suitTag.putDouble("PressureProtection", tier.pressureProtection / 4.0);
            suitTag.putDouble("HeatShielding", tier.heatShielding / 4.0);
            tag.put("SpaceSuit", suitTag);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal(tier.displayName + " Space Suit").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Slot: " + slot.getName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("O\u2082 Supply: " + formatPercent(tier.oxygenSupplyRate)).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Radiation Shield: " + formatPercent(tier.radiationShielding)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Thermal Protection: " + formatPercent(tier.thermalProtection)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Pressure Seal: " + formatPercent(tier.pressureProtection)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("Heat Shield: " + formatPercent(tier.heatShielding)).withStyle(ChatFormatting.GOLD));
    }

    private String formatPercent(double value) {
        return String.format("%.0f%%", value * 100);
    }

    public SuitTier getTier() { return tier; }
    public EquipmentSlot getSlot() { return slot; }
}
