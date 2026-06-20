package mods.hexagon.thrusted;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Raptor3Block extends Block implements EntityBlock {
    public static final MapCodec<Raptor3Block> CODEC = simpleCodec(Raptor3Block::new);
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public Raptor3Block(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockPos bodyPos = context.getClickedPos();
        BlockPos nozzlePos = bodyPos.relative(facing);
        Level level = context.getLevel();

        if (level.isInWorldBounds(nozzlePos) && level.getBlockState(nozzlePos).canBeReplaced(context)) {
             return this.defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(HALF, DoubleBlockHalf.UPPER)
                    .setValue(POWERED, false);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.relative(state.getValue(FACING)), state.setValue(HALF, DoubleBlockHalf.LOWER), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        Direction engineFacing = state.getValue(FACING);
        Direction otherHalfDir = (half == DoubleBlockHalf.LOWER ? engineFacing.getOpposite() : engineFacing);
        
        if (direction == otherHalfDir) {
            if (neighborState.is(this) && neighborState.getValue(HALF) != half && neighborState.getValue(FACING) == engineFacing) {
                return state;
            }
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, HALF);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        DoubleBlockHalf half = state.getValue(HALF);
        
        if (half == DoubleBlockHalf.LOWER) {
            return switch (facing) {
                case NORTH -> Block.box(0, 0, 0, 16, 16, 32);
                case SOUTH -> Block.box(0, 0, -16, 16, 16, 16);
                case WEST  -> Block.box(0, 0, 0, 32, 16, 16);
                case EAST  -> Block.box(-16, 0, 0, 16, 16, 16);
                case UP    -> Block.box(0, -16, 0, 16, 16, 16);
                case DOWN  -> Block.box(0, 0, 0, 16, 32, 16);
            };
        } else {
            return switch (facing) {
                case NORTH -> Block.box(0, 0, -16, 16, 16, 16);
                case SOUTH -> Block.box(0, 0, 0, 16, 16, 32);
                case WEST  -> Block.box(-16, 0, 0, 16, 16, 16);
                case EAST  -> Block.box(0, 0, 0, 32, 16, 16);
                case UP    -> Block.box(0, 0, 0, 16, 32, 16);
                case DOWN  -> Block.box(0, -16, 0, 16, 16, 16);
            };
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        return this.getShape(state, reader, pos, CollisionContext.empty());
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new Raptor3BlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != Thrusted.RAPTOR3_BLOCK_ENTITY.get() || state.getValue(HALF) != DoubleBlockHalf.LOWER) return null;
        if (level.isClientSide) {
            return (lvl, pos, stt, be) -> { if (be instanceof Raptor3BlockEntity raptor) raptor.clientTick(lvl, pos, stt); };
        } else {
            return (lvl, pos, stt, be) -> { if (be instanceof Raptor3BlockEntity raptor) Raptor3BlockEntity.serverTick(lvl, pos, stt, raptor); };
        }
    }
}
