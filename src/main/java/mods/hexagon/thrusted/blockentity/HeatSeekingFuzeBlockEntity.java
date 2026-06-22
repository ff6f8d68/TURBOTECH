package mods.hexagon.thrusted.blockentity;
import mods.hexagon.thrusted.Thrusted;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import java.util.List;

public class HeatSeekingFuzeBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    public HeatSeekingFuzeBlockEntity(BlockPos pos, BlockState state) {
        super(Thrusted.HEAT_SEEKING_FUZE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        AABB searchArea = new AABB(worldPosition).inflate(50.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea);
        
        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(0); 
            
            Vec3 targetPos = target.position();
            Vec3 missilePos = Vec3.atCenterOf(worldPosition);
            Vec3 directionToTarget = targetPos.subtract(missilePos).normalize();
            
            // Steering force scaled by dt to convert to impulse
            Vector3d steeringImpulse = new Vector3d(directionToTarget.x, directionToTarget.y, directionToTarget.z).mul(0.1 * dt);
            body.applyLinearImpulse(steeringImpulse);
        }
    }
}
