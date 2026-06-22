package mods.hexagon.thrusted.block;
import mods.hexagon.thrusted.blockentity.TimedFuzeBlockEntity;
import mods.hexagon.thrusted.Thrusted;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TimedFuzeBlock extends FuzeBlock implements EntityBlock {
    public TimedFuzeBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimedFuzeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, stt, be) -> {
            if (be instanceof TimedFuzeBlockEntity fuze) fuze.serverTick(lvl, pos, stt);
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TimedFuzeBlockEntity fuze) {
            return fuze.onUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
