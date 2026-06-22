package mods.hexagon.thrusted.block;
import mods.hexagon.thrusted.blockentity.TurbofanBlockEntity;
import mods.hexagon.thrusted.Thrusted;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class TurbofanBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public static final MapCodec<TurbofanBlock> CODEC = simpleCodec(TurbofanBlock::new);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    
    public TurbofanBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbofanBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != Thrusted.TURBOFAN_BLOCK_ENTITY.get()) {
            return null;
        }
        
        if (level.isClientSide) {
            return (lvl, pos, stt, be) -> {
                if (be instanceof TurbofanBlockEntity turbofan) {
                    turbofan.clientTick(lvl, pos, stt);
                }
            };
        } else {
            return (lvl, pos, stt, be) -> {
                if (be instanceof TurbofanBlockEntity turbofan) {
                    TurbofanBlockEntity.serverTick(lvl, pos, stt, turbofan);
                }
            };
        }
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            level.updateNeighbourForOutputSignal(pos, this);
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TurbofanBlockEntity turbofan) {
            return turbofan.isActive() ? 15 : 0;
        }
        return 0;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Return a block-shaped collision box
        return Shapes.block();
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Return a block-shaped collision box for entity collision
        return Shapes.block();
    }
    
    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        // Return shape for supporting other blocks
        return Shapes.block();
    }
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        // Returning an empty shape tells the rendering engine: "This block does not hide ANY neighboring faces!"
        return Shapes.empty();
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(POWERED, false);
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Check for redstone power
        if (level instanceof Level realLevel) {
            boolean powered = realLevel.hasNeighborSignal(pos);
            BlockEntity blockEntity = realLevel.getBlockEntity(pos);
            if (blockEntity instanceof TurbofanBlockEntity turbofan) {
                if (powered != turbofan.isActive()) {
                    turbofan.setChanged();
                }
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
