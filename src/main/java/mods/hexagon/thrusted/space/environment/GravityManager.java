package mods.hexagon.thrusted.space.environment;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.SpaceDimensions;
import mods.hexagon.thrusted.space.api.GravityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Thrusted.MODID)
public class GravityManager {

    private static final double EARTH_GRAVITY = 0.08;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!(entity instanceof LivingEntity)) return;
        if (entity instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) return;

        Level level = entity.level();
        var dimKey = level.dimension();

        if (SpaceDimensions.isOrbitDimension(dimKey)) {
            applyZeroGravity(entity);
        } else if (SpaceDimensions.isPlanetDimension(dimKey)) {
            String planetName = dimKey.location().getPath().replace("planet_", "");
            applyPlanetGravity(entity, planetName);
        }
    }

    private static void applyZeroGravity(Entity entity) {
        entity.setNoGravity(true);
        Vec3 motion = entity.getDeltaMovement();
        double drag = 0.999;
        entity.setDeltaMovement(motion.x * drag, motion.y * drag, motion.z * drag);

        if (entity instanceof LivingEntity living) {
            living.fallDistance = 0;
        }
    }

    private static void applyPlanetGravity(Entity entity, String planetName) {
        CelestialBody body = CelestialBodyRegistry.get(capitalize(planetName));
        if (body == null) return;

        GravityData gravity = body.getGravity();
        double gravMultiplier = gravity.getMinecraftGravityMultiplier();

        if (Math.abs(gravMultiplier - 1.0) < 0.01) return;

        entity.setNoGravity(true);

        Vec3 motion = entity.getDeltaMovement();
        double customGravity = -EARTH_GRAVITY * gravMultiplier;

        if (!entity.onGround()) {
            entity.setDeltaMovement(motion.x, motion.y + customGravity, motion.z);
        }

        if (entity instanceof LivingEntity living) {
            if (gravMultiplier < 0.5) {
                living.fallDistance *= (float) gravMultiplier;
            }
        }
    }

    public static double getGravityMultiplier(Level level) {
        var dimKey = level.dimension();
        if (SpaceDimensions.isOrbitDimension(dimKey)) return 0.0;
        if (SpaceDimensions.isPlanetDimension(dimKey)) {
            String planetName = dimKey.location().getPath().replace("planet_", "");
            CelestialBody body = CelestialBodyRegistry.get(capitalize(planetName));
            if (body != null) return body.getGravity().getMinecraftGravityMultiplier();
        }
        return 1.0;
    }

    public static double getJumpMultiplier(Level level) {
        double grav = getGravityMultiplier(level);
        if (grav <= 0) return 3.0;
        return 1.0 / Math.sqrt(grav);
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
