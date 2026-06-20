package mods.hexagon.thrusted;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

public class MissileCoreBlockEntity extends BlockEntity {
    private final List<BlockPos> payloadSequence = new ArrayList<>();
    private boolean isDetonating = false;
    private boolean isLaunched = false;
    private int detonationIndex = 0;

    public MissileCoreBlockEntity(BlockPos pos, BlockState state) {
        super(Thrusted.MISSILE_CORE_BLOCK_ENTITY.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (!isLaunched) {
            if (level.hasNeighborSignal(pos)) {
                isLaunched = true;
                setChanged();
            }
        }

        if (isDetonating) {
            processDetonation(level);
        } else {
            if (level.getGameTime() % 20 == 0) {
                scanPayloads(level, pos);
            }
        }
    }

    private void scanPayloads(Level level, BlockPos pos) {
        payloadSequence.clear();
        // Scan in all directions for payloads
        for (Direction dir : Direction.values()) {
            for (int i = 1; i < 16; i++) {
                BlockPos checkPos = pos.relative(dir, i);
                BlockState checkState = level.getBlockState(checkPos);
                if (isPayload(checkState)) {
                    if (!payloadSequence.contains(checkPos)) payloadSequence.add(checkPos);
                } else break;
            }
        }
    }

    private boolean isPayload(BlockState state) {
        return state.is(Thrusted.EXPLOSIVE_PAYLOAD.get()) || 
               state.is(Thrusted.FIRE_CHARGE_PAYLOAD.get()) || 
               state.is(Thrusted.REPULSION_PAYLOAD.get());
    }

    public void triggerDetonation() {
        if (!isDetonating) {
            isDetonating = true;
            detonationIndex = 0;
            setChanged();
        }
    }

    public boolean isLaunched() {
        return isLaunched;
    }

    private void processDetonation(Level level) {
        if (detonationIndex < payloadSequence.size()) {
            BlockPos payloadPos = payloadSequence.get(detonationIndex);
            BlockState payloadState = level.getBlockState(payloadPos);
            
            triggerPayloadEffect(level, payloadPos, payloadState);
            level.setBlock(payloadPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            
            detonationIndex++;
        } else {
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 3.0f, Level.ExplosionInteraction.BLOCK);
            level.setBlock(worldPosition, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void triggerPayloadEffect(Level level, BlockPos pos, BlockState state) {
        if (state.is(Thrusted.EXPLOSIVE_PAYLOAD.get())) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0f, Level.ExplosionInteraction.TNT);
        } else if (state.is(Thrusted.FIRE_CHARGE_PAYLOAD.get())) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0f, true, Level.ExplosionInteraction.NONE);
        } else if (state.is(Thrusted.REPULSION_PAYLOAD.get())) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0f, Level.ExplosionInteraction.NONE);
            // Kinetic impulse is handled by the explode call's knockback in standard MC
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsLaunched", isLaunched);
        tag.putBoolean("IsDetonating", isDetonating);
        tag.putInt("DetonationIndex", detonationIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isLaunched = tag.getBoolean("IsLaunched");
        this.isDetonating = tag.getBoolean("IsDetonating");
        this.detonationIndex = tag.getInt("DetonationIndex");
    }
}
