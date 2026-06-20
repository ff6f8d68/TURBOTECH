package mods.hexagon.thrusted.space;

import com.mojang.logging.LogUtils;
import mods.hexagon.thrusted.space.dimension.PlanetDimensionManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

public class AeronauticsIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean aeronauticsLoaded = false;
    private static boolean checked = false;

    public static boolean isAeronauticsLoaded() {
        if (!checked) {
            aeronauticsLoaded = ModList.get() != null && ModList.get().isLoaded("aeronautics");
            checked = true;
            if (aeronauticsLoaded) {
                LOGGER.info("Create Aeronautics detected - enabling space integration");
            }
        }
        return aeronauticsLoaded;
    }

    public static boolean isAirshipEntity(Entity entity) {
        if (!isAeronauticsLoaded()) return false;
        String className = entity.getClass().getName();
        return className.contains("aeronautics") || className.contains("Airship") || className.contains("Balloon");
    }

    public static void applySpacePhysics(Entity entity) {
        if (!PlanetDimensionManager.isSpaceDimension(entity.level())) return;
        entity.setNoGravity(true);
        entity.fallDistance = 0;

        Vec3 vel = entity.getDeltaMovement();
        double speed = vel.length();
        if (speed > 0) {
            double damp = 0.95;
            vel = vel.scale(damp);
            if (speed > 0.5) {
                vel = vel.scale(0.5 / speed);
            }
            entity.setDeltaMovement(vel);
        }
    }
}
