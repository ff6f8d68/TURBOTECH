package mods.hexagon.thrusted.space;

import com.mojang.logging.LogUtils;
import mods.hexagon.thrusted.space.body.CelestialBody;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpaceEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static SpaceEngine instance;

    // Physics constants
    public static final double SPACE_DAMPING = 0.95;
    public static final double MAX_SPEED = 0.5;
    public static final double THRUST_ACCEL = 0.005;
    public static final double SCANNER_RANGE = 500.0;

    private final Map<UUID, Vec3> playerVelocities = new HashMap<>();
    private final Map<UUID, Double> playerAltitudes = new HashMap<>();
    private final SolarSystem solarSystem;

    private SpaceEngine() {
        this.solarSystem = new SolarSystem();
        LOGGER.info("Space Engine initialized");
    }

    public static SpaceEngine getInstance() {
        if (instance == null) {
            instance = new SpaceEngine();
        }
        return instance;
    }

    public void update(ServerPlayer player, float deltaTime) {
        UUID uuid = player.getUUID();
        Vec3 velocity = player.getDeltaMovement();
        playerVelocities.put(uuid, velocity);
        playerAltitudes.put(uuid, player.getY());
        solarSystem.update(deltaTime);
    }

    public void applySpaceMovement(Player player) {
        Vec3 vel = player.getDeltaMovement();
        vel = vel.scale(SPACE_DAMPING);
        double speed = vel.length();
        if (speed > MAX_SPEED) {
            vel = vel.scale(MAX_SPEED / speed);
        }
        player.setDeltaMovement(vel);
    }

    public Vec3 getPlayerVelocity(UUID uuid) {
        return playerVelocities.getOrDefault(uuid, Vec3.ZERO);
    }

    public double getPlayerAltitude(UUID uuid) {
        return playerAltitudes.getOrDefault(uuid, 0.0);
    }

    public SolarSystem getSolarSystem() {
        return solarSystem;
    }

    public CelestialBody getBodyByName(String name) {
        return solarSystem.findBodyByName(name);
    }

    public static void reset() {
        instance = null;
    }
}
