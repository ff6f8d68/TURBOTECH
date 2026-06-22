package mods.hexagon.thrusted.block;
import mods.hexagon.thrusted.blockentity.HeatSeekingFuzeBlockEntity;
import mods.hexagon.thrusted.Thrusted;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HeatSeekingFuzeBlock extends Block implements EntityBlock {
    public HeatSeekingFuzeBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatSeekingFuzeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Core guidance logic is in the BlockEntity's sable$physicsTick, no ticker needed for guidance.
        return null;
    }
}
