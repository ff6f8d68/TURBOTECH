package mods.hexagon.thrusted.space;

import mods.hexagon.thrusted.Thrusted;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;

import java.util.*;

@EventBusSubscriber(modid = Thrusted.MODID)
public class OrbitManager {
    private static final Map<String, Vector3d> BODY_POSITIONS = new HashMap<>();
    private static final Map<String, Float> BODY_ROTATIONS = new HashMap<>();
    private static long lastUpdateTick = -1;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        Level overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        long gameTime = overworld.getGameTime();
        if (gameTime == lastUpdateTick) return;
        lastUpdateTick = gameTime;

        for (CelestialBody body : CelestialBodyRegistry.getAll()) {
            body.computePosition(gameTime);
            Vector3d pos = body.getCurrentPosition();
            BODY_POSITIONS.put(body.getName(), new Vector3d(pos));
            BODY_ROTATIONS.put(body.getName(), body.getCurrentRotation());
        }
    }

    public static Vector3d getPosition(String bodyName) {
        Vector3d pos = BODY_POSITIONS.get(bodyName);
        return pos != null ? new Vector3d(pos) : new Vector3d();
    }

    public static float getRotation(String bodyName) {
        return BODY_ROTATIONS.getOrDefault(bodyName, 0.0f);
    }

    public static Map<String, Vector3d> getAllPositions() {
        Map<String, Vector3d> copy = new HashMap<>();
        BODY_POSITIONS.forEach((k, v) -> copy.put(k, new Vector3d(v)));
        return copy;
    }
}
