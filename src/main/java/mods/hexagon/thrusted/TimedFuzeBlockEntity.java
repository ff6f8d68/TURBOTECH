package mods.hexagon.thrusted;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TimedFuzeBlockEntity extends BlockEntity {
    private int countdownTicks;
    private boolean activated;

    public TimedFuzeBlockEntity(BlockPos pos, BlockState state) {
        super(Thrusted.TIMED_FUZE_BLOCK_ENTITY.get(), pos, state);
        this.countdownTicks = 20 * 5; // Default 5 seconds
        this.activated = false;
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (activated && countdownTicks > 0) {
            countdownTicks--;
            if (countdownTicks == 0) {
                findAndTriggerCore(level, pos);
            }
        }
    }

    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                // Adjust countdown (example: +1 second)
                countdownTicks += 20;
                player.sendSystemMessage(Component.literal("Timed Fuze: " + (countdownTicks / 20) + "s"));
            } else {
                // Activate/Deactivate
                activated = !activated;
                player.sendSystemMessage(Component.literal("Timed Fuze " + (activated ? "Activated" : "Deactivated")));
            }
            setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    private void findAndTriggerCore(Level level, BlockPos pos) {
        // Logic to find MissileCore and trigger detonation
        // Similar to FuzeBlock's logic
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof MissileCoreBlockEntity core) {
                core.triggerDetonation();
                return;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CountdownTicks", countdownTicks);
        tag.putBoolean("Activated", activated);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.countdownTicks = tag.getInt("CountdownTicks");
        this.activated = tag.getBoolean("Activated");
    }
}
