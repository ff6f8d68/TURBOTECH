package mods.hexagon.thrusted.space.compat;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionProperties;
import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.SpaceDimensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Thrusted.MODID)
public class AeronauticsGravity {

    private static final Vector3f ZERO_GRAVITY = new Vector3f(0, 0, 0);

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        for (String planetName : SpaceDimensions.getPlanetNames()) {
            var orbitKey = SpaceDimensions.orbitKey(planetName);
            ServerLevel orbitLevel = server.getLevel(orbitKey);
            if (orbitLevel == null) continue;

            DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(server);
            DynamicDimensionProperties props = registry.getDimensionProperties(orbitKey);

            if (props == null || !isZeroGravity(props)) {
                registry.setDimensionProperties(orbitKey, new ZeroGravityProperties());
            }
        }
    }

    private static boolean isZeroGravity(DynamicDimensionProperties props) {
        Vector3f grav = props.baseGravity();
        return grav.x == 0 && grav.y == 0 && grav.z == 0;
    }

    private static class ZeroGravityProperties extends DynamicDimensionProperties {
        @Override
        public int priority() { return 100; }

        @Override
        public Vector3f baseGravity() { return ZERO_GRAVITY; }

        @Override
        public double basePressure() { return 0.0; }

        @Override
        public float universalDrag() { return 0.001f; }

        @Override
        public Vector3f magneticNorth() { return new Vector3f(0, 1, 0); }
    }
}
