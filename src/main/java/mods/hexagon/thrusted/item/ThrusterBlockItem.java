package mods.hexagon.thrusted.item;
import mods.hexagon.thrusted.Thrusted;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ThrusterBlockItem extends BlockItem {
    private final double minThrust;
    private final double maxThrust;
    private final String size;
    private final String startup;
    private final String shutdown;
    private final String color;
    private final String trail;
    private final String length;
    private final boolean ionized;

    public ThrusterBlockItem(Block block, Properties properties, double min, double max, String size, String startup, String shutdown, String color, String trail, String length, boolean ionized) {
        super(block, properties);
        this.minThrust = min;
        this.maxThrust = max;
        this.size = size;
        this.startup = startup;
        this.shutdown = shutdown;
        this.color = color;
        this.trail = trail;
        this.length = length;
        this.ionized = ionized;
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean hasShiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (FMLLoader.getDist().isClient() && hasShiftDown()) {
            tooltip.add(Component.literal("Min Thrust: ").withStyle(ChatFormatting.GRAY).append(Component.literal(minThrust + " N").withStyle(ChatFormatting.YELLOW)));
            tooltip.add(Component.literal("Max Thrust: ").withStyle(ChatFormatting.GRAY).append(Component.literal(maxThrust + " N").withStyle(ChatFormatting.GOLD)));
            tooltip.add(Component.literal("Size: ").withStyle(ChatFormatting.GRAY).append(Component.literal(size).withStyle(ChatFormatting.AQUA)));
            tooltip.add(Component.literal("Startup Time: ").withStyle(ChatFormatting.GRAY).append(Component.literal(startup).withStyle(ChatFormatting.WHITE)));
            tooltip.add(Component.literal("Shutdown Time: ").withStyle(ChatFormatting.GRAY).append(Component.literal(shutdown).withStyle(ChatFormatting.WHITE)));
            tooltip.add(Component.literal("Thrust Color: ").withStyle(ChatFormatting.GRAY).append(Component.literal(color).withStyle(ChatFormatting.DARK_PURPLE)));
            tooltip.add(Component.literal("Trail: ").withStyle(ChatFormatting.GRAY).append(Component.literal(trail).withStyle(ChatFormatting.DARK_GRAY)));
            tooltip.add(Component.literal("Thrust Length: ").withStyle(ChatFormatting.GRAY).append(Component.literal(length).withStyle(ChatFormatting.BLUE)));
            tooltip.add(Component.literal("Ionized Thrust: ").withStyle(ChatFormatting.GRAY).append(Component.literal(ionized ? "Yes" : "No").withStyle(ionized ? ChatFormatting.GREEN : ChatFormatting.RED)));
        } else {
            tooltip.add(Component.literal("Press ").withStyle(ChatFormatting.GRAY).append(Component.literal("SHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)).append(Component.literal(" for more info").withStyle(ChatFormatting.GRAY)));
        }
    }
}
