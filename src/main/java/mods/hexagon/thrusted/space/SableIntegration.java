package mods.hexagon.thrusted.space;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.slf4j.Logger;

public class SableIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean isSpaceEnvironment(Entity entity) {
        return isSpaceEnvironment(entity.level());
    }

    public static void applySpacePhysics(Entity entity) {
        if (!isSpaceEnvironment(entity)) return;
        entity.setNoGravity(true);
    }

    public static boolean canAirPropulsionWork(Entity entity) {
        return !isSpaceEnvironment(entity);
    }

    public static boolean canRocketPropulsionWork(Entity entity) {
        return true;
    }

    public static boolean isAirBasedEngine(String engineType) {
        return engineType.equals("propeller") || engineType.equals("fan") ||
                engineType.equals("turbofan") || engineType.equals("rotor") ||
                engineType.equals("sail");
    }

    public static boolean isRocketEngine(String engineType) {
        return engineType.equals("rocket") || engineType.equals("ion") ||
                engineType.equals("raptor") || engineType.equals("thruster") ||
                engineType.equals("missile_thruster");
    }

    public static double getEngineEfficiency(String engineType, Entity entity) {
        if (isSpaceEnvironment(entity)) {
            if (isAirBasedEngine(engineType)) return 0.0;
            if (isRocketEngine(engineType)) return 1.0;
            return 0.5;
        }
        if (isAirBasedEngine(engineType)) return 1.0;
        if (isRocketEngine(engineType)) return 0.8;
        return 1.0;
    }

    public static void applyAntiGravity(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (subLevel == null) return;
        Level level = subLevel.getLevel();
        if (level == null) return;
        if (!isSpaceEnvironment(level)) return;
        double mass = subLevel.getMassTracker().getMass();
        if (mass <= 0) return;
        double impulse = mass * 0.049 * dt;
        body.applyLinearImpulse(new Vector3d(0, impulse, 0));
    }

    public static boolean isSpaceEnvironment(Level level) {
        var key = level.dimension();
        return SpaceDimensions.isOrbitDimension(key) || SpaceDimensions.isPlanetDimension(key);
    }

    public static void handleShipMovement(Entity entity, Vec3 thrust) {
        if (isSpaceEnvironment(entity)) {
            double damp = 0.98;
            Vec3 current = entity.getDeltaMovement();
            Vec3 newMove = current.add(thrust).scale(damp);
            double speed = newMove.length();
            if (speed > 0.5) {
                newMove = newMove.scale(0.5 / speed);
            }
            entity.setDeltaMovement(newMove);
        } else {
            entity.setDeltaMovement(entity.getDeltaMovement().add(thrust));
        }
    }
}
