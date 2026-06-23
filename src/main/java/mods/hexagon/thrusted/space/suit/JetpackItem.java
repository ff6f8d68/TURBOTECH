package mods.hexagon.thrusted.space.suit;

import mods.hexagon.thrusted.space.SpaceDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class JetpackItem extends Item {

    private final JetpackTier tier;

    public enum JetpackTier {
        CHEMICAL("Chemical", 0.15, 500, 2.0, false),
        ION("Ion", 0.05, 5000, 0.5, true),
        PLASMA("Plasma", 0.25, 2000, 3.0, true),
        QUANTUM("Quantum", 0.4, 10000, 5.0, true);

        public final String displayName;
        public final double thrustForce;
        public final int maxFuel;
        public final double fuelConsumptionRate;
        public final boolean worksInSpace;

        JetpackTier(String name, double thrust, int fuel, double consumption, boolean space) {
            this.displayName = name;
            this.thrustForce = thrust;
            this.maxFuel = fuel;
            this.fuelConsumptionRate = consumption;
            this.worksInSpace = space;
        }
    }

    public JetpackItem(JetpackTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public boolean tryThrust(ServerPlayer player, ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        CompoundTag tag = customData.copyTag();
        double fuel = tag.getDouble("Fuel");
        if (fuel <= 0) return false;

        boolean inSpace = SpaceDimensions.isOrbitDimension(player.level().dimension());
        if (inSpace && !tier.worksInSpace) return false;

        double thrustMult = inSpace ? 1.5 : 1.0;
        Vec3 look = player.getLookAngle();
        Vec3 thrust = look.scale(tier.thrustForce * thrustMult);

        Vec3 currentMotion = player.getDeltaMovement();
        player.setDeltaMovement(currentMotion.add(thrust));
        player.fallDistance = 0;
        player.hurtMarked = true;

        fuel -= tier.fuelConsumptionRate;
        tag.putDouble("Fuel", Math.max(0, fuel));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
            t.putDouble("Fuel", tag.getDouble("Fuel"));
        });

        return true;
    }

    public void refuel(ItemStack stack, double amount) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        double fuel = 0;
        if (customData != null) {
            fuel = customData.copyTag().getDouble("Fuel");
        }
        double finalFuel = Math.min(tier.maxFuel, fuel + amount);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putDouble("Fuel", finalFuel));
    }

    public double getFuelLevel(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getDouble("Fuel");
    }

    public double getFuelPercent(ItemStack stack) {
        return getFuelLevel(stack) / tier.maxFuel;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putDouble("Fuel", tier.maxFuel);
            tag.putString("Tier", tier.name());
        });
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal(tier.displayName + " Jetpack").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(""));

        double fuel = getFuelLevel(stack);
        double percent = fuel / tier.maxFuel * 100;
        ChatFormatting fuelColor = percent > 50 ? ChatFormatting.GREEN : percent > 20 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        tooltip.add(Component.literal("Fuel: " + String.format("%.0f/%.0f (%.0f%%)", fuel, (double)tier.maxFuel, percent))
                .withStyle(fuelColor));
        tooltip.add(Component.literal("Thrust: " + String.format("%.2f", tier.thrustForce)).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("Space-rated: " + (tier.worksInSpace ? "Yes" : "No"))
                .withStyle(tier.worksInSpace ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int)(getFuelPercent(stack) * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double percent = getFuelPercent(stack);
        if (percent > 0.5) return 0x00FF00;
        if (percent > 0.2) return 0xFFFF00;
        return 0xFF0000;
    }

    public JetpackTier getTier() { return tier; }
}
