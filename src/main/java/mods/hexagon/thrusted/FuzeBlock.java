package mods.hexagon.thrusted;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FuzeBlock extends Block {
    public FuzeBlock(Properties properties) {
        super(properties);
    }

    protected void findAndTriggerCore(Level level, BlockPos pos) {
        // Search adjacent blocks for a Missile Core
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof MissileCoreBlockEntity core) {
                core.triggerDetonation();
                return;
            }
        }
    }
}
