package mods.hexagon.thrusted.blockentity;
import mods.hexagon.thrusted.blockentity.MissileCoreBlockEntity;

import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.SableIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class MissileThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller {
    public static final double THRUST_FORCE = 15000.0;
    
    public MissileThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(Thrusted.MISSILE_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        SableIntegration.applyAntiGravity(subLevel, body, dt);
        // Find core to see if we are launched
        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        BlockEntity coreBe = subLevel.getLevel().getBlockEntity(worldPosition.relative(facing.getOpposite()));
        
        if (coreBe instanceof MissileCoreBlockEntity core && core.isLaunched()) {
            Vec3 thrustDir = Vec3.atLowerCornerOf(facing.getNormal());
            double scale = 0.005; 
            Vec3 force = thrustDir.scale(THRUST_FORCE * scale);

            Vector3d pos = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
            Vector3d vec = new Vector3d(force.x, force.y, force.z);

            var key = ResourceLocation.fromNamespaceAndPath("sable", "propulsion");
            var group = dev.ryanhcode.sable.api.physics.force.ForceGroups.REGISTRY.get(key);
            if (group != null) {
                // Use applyAndRecordPointForce which is known to work in this project
                subLevel.getOrCreateQueuedForceGroup(group).applyAndRecordPointForce(pos, vec);
            }
        }
    }

    @Override public Direction getBlockDirection() { return getBlockState().getValue(BlockStateProperties.FACING); }
    @Override public double getAirflow() {
        if (level != null && SableIntegration.isSpaceEnvironment(level)) return 0.0;
        return 1000.0;
    }
    @Override public double getThrust() {
        if (level != null && SableIntegration.isSpaceEnvironment(level)) return 0.0;
        return THRUST_FORCE;
    }
    @Override public boolean isActive() { return true; }
    @Override public Level getLevel() { return level; }
    @Override public BlockPos getBlockPos() { return worldPosition; }
    @Override public BlockEntityPropeller getPropeller() { return this; }
}
