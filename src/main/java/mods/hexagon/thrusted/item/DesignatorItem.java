package mods.hexagon.thrusted.item;
import mods.hexagon.thrusted.Thrusted;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

public class DesignatorItem extends Item {
    public DesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        
        if (!context.getLevel().isClientSide) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putInt("TargetX", pos.getX());
                tag.putInt("TargetY", pos.getY());
                tag.putInt("TargetZ", pos.getZ());
            });
            
            context.getPlayer().sendSystemMessage(Component.literal("Target Designated at: " + pos.toShortString()).withStyle(ChatFormatting.RED));
        }
        
        return InteractionResult.SUCCESS;
    }
}
