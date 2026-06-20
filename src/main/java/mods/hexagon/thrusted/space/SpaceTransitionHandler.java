package mods.hexagon.thrusted.space;

import com.mojang.logging.LogUtils;
import mods.hexagon.thrusted.space.dimension.PlanetDimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class SpaceTransitionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double SPACE_ALTITUDE = 320.0;
    private static final double RETURN_ALTITUDE = -60.0;
    private static final Vec3 SPACE_SPAWN_OFFSET = new Vec3(0, 200, 0);

    public static void checkSpaceTransition(ServerPlayer player) {
        double y = player.getY();
        String dimPath = player.level().dimension().location().getPath();

        if (dimPath.equals("space")) {
            if (y < RETURN_ALTITUDE) {
                returnFromSpace(player);
            }
        } else {
            if (y > SPACE_ALTITUDE) {
                transferToSpace(player);
            }
        }
    }

    private static void transferToSpace(ServerPlayer player) {
        PlanetDimensionManager.transferToSpace(player);
    }

    private static void returnFromSpace(ServerPlayer player) {
        returnToNearestPlanet(player);
    }

    private static void returnToNearestPlanet(ServerPlayer player) {
        if (player.getServer() == null) return;

        SpaceEngine engine = SpaceEngine.getInstance();
        var planets = engine.getSolarSystem().getPlanets();
        Vec3 playerPos = player.position();

        String nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (var planet : planets) {
            if (planet.isMoon()) continue;
            double dist = playerPos.distanceTo(planet.getPosition());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = planet.getName();
            }
        }

        if (nearest != null) {
            PlanetDimensionManager.transferToPlanet(player, nearest);
        } else {
            ServerLevel overworld = player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (overworld != null) {
                player.teleportTo(overworld, 0, 320, 0, player.getYRot(), player.getXRot());
            }
        }
    }

    public static double getSpaceAltitude() { return SPACE_ALTITUDE; }
    public static double getReturnAltitude() { return RETURN_ALTITUDE; }
}
