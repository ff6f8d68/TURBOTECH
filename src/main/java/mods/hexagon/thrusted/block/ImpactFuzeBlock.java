package mods.hexagon.thrusted.block;
import mods.hexagon.thrusted.Thrusted;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ImpactFuzeBlock extends FuzeBlock {
    public ImpactFuzeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        // Trigger if moved (impact)
        if (movedByPiston) {
            findAndTriggerCore(level, pos);
        }
    }
}
