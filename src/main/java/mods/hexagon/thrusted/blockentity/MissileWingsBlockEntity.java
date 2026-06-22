package mods.hexagon.thrusted.blockentity;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.SableIntegration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public class MissileWingsBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    public MissileWingsBlockEntity(BlockPos pos, BlockState state) {
        super(Thrusted.MISSILE_WINGS_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        SableIntegration.applyAntiGravity(subLevel, body, dt);
        // Stabilization logic: Apply counter-torque to zero out angular velocity
        Vector3d angularVel = body.getAngularVelocity(new Vector3d());
        
        // Stabilization strength
        double damping = 0.5 * dt; // Scale by dt for impulse
        Vector3d stabilizationTorque = new Vector3d(angularVel).mul(-damping);
        
        // RigidBodyHandle uses impulses. 
        body.applyTorqueImpulse(stabilizationTorque);

        // Aerodynamic Lift placeholder
        Vector3d velocity = body.getLinearVelocity(new Vector3d());
        if (velocity.length() > 5.0) {
            Vector3d lift = new Vector3d(0, 0.02 * dt, 0); 
            body.applyLinearImpulse(lift);
        }
    }
}
