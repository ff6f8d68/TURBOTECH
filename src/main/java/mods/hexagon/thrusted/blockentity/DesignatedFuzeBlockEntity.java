package mods.hexagon.thrusted.blockentity;
import mods.hexagon.thrusted.blockentity.MissileCoreBlockEntity;
import mods.hexagon.thrusted.Thrusted;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class DesignatedFuzeBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private BlockPos targetPos = null;

    public DesignatedFuzeBlockEntity(BlockPos pos, BlockState state) {
        super(Thrusted.DESIGNATED_FUZE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (targetPos != null) {
            Vec3 target = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
            Vec3 current = Vec3.atCenterOf(worldPosition);
            
            // Check for arrival (proximity detonation)
            if (current.distanceTo(target) < 3.0) {
                triggerCore();
                return;
            }

            // Guidance
            Vec3 dir = target.subtract(current).normalize();
            Vector3d impulse = new Vector3d(dir.x, dir.y, dir.z).mul(0.15 * dt);
            body.applyLinearImpulse(impulse);
        }
    }

    private void triggerCore() {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be instanceof MissileCoreBlockEntity core) {
                core.triggerDetonation();
                return;
            }
        }
    }

    public void setTarget(BlockPos pos) {
        this.targetPos = pos;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (targetPos != null) {
            tag.putInt("TX", targetPos.getX());
            tag.putInt("TY", targetPos.getY());
            tag.putInt("TZ", targetPos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("TX")) {
            targetPos = new BlockPos(tag.getInt("TX"), tag.getInt("TY"), tag.getInt("TZ"));
        }
    }
}
