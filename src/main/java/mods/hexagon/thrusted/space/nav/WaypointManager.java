package mods.hexagon.thrusted.space.nav;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.*;

public class WaypointManager {
    private static final Map<UUID, List<Waypoint>> PLAYER_WAYPOINTS = new HashMap<>();
    private static final int MAX_WAYPOINTS_PER_PLAYER = 50;

    public record Waypoint(
            String name,
            String dimension,
            double x, double y, double z,
            int color,
            WaypointType type,
            long createdAt
    ) {
        public enum WaypointType {
            CUSTOM,
            PLANET_SURFACE,
            ORBIT_STATION,
            RESOURCE_DEPOSIT,
            ANOMALY,
            BASE
        }

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Name", name);
            tag.putString("Dimension", dimension);
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putInt("Color", color);
            tag.putString("Type", type.name());
            tag.putLong("Created", createdAt);
            return tag;
        }

        public static Waypoint fromNbt(CompoundTag tag) {
            return new Waypoint(
                    tag.getString("Name"),
                    tag.getString("Dimension"),
                    tag.getDouble("X"),
                    tag.getDouble("Y"),
                    tag.getDouble("Z"),
                    tag.getInt("Color"),
                    WaypointType.valueOf(tag.getString("Type")),
                    tag.getLong("Created")
            );
        }
    }

    public static void addWaypoint(UUID player, Waypoint waypoint) {
        List<Waypoint> waypoints = PLAYER_WAYPOINTS.computeIfAbsent(player, k -> new ArrayList<>());
        if (waypoints.size() >= MAX_WAYPOINTS_PER_PLAYER) {
            waypoints.remove(0);
        }
        waypoints.add(waypoint);
    }

    public static void removeWaypoint(UUID player, String name) {
        List<Waypoint> waypoints = PLAYER_WAYPOINTS.get(player);
        if (waypoints != null) {
            waypoints.removeIf(w -> w.name().equals(name));
        }
    }

    public static List<Waypoint> getWaypoints(UUID player) {
        return PLAYER_WAYPOINTS.getOrDefault(player, List.of());
    }

    public static List<Waypoint> getWaypointsInDimension(UUID player, String dimension) {
        return getWaypoints(player).stream()
                .filter(w -> w.dimension().equals(dimension))
                .toList();
    }

    public static Optional<Waypoint> getNearestWaypoint(UUID player, String dimension, Vector3d position) {
        return getWaypointsInDimension(player, dimension).stream()
                .min(Comparator.comparingDouble(w ->
                        position.distance(new Vector3d(w.x(), w.y(), w.z()))));
    }

    public static CompoundTag saveAll(UUID player) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Waypoint wp : getWaypoints(player)) {
            list.add(wp.toNbt());
        }
        tag.put("Waypoints", list);
        return tag;
    }

    public static void loadAll(UUID player, CompoundTag tag) {
        ListTag list = tag.getList("Waypoints", Tag.TAG_COMPOUND);
        List<Waypoint> waypoints = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            waypoints.add(Waypoint.fromNbt(list.getCompound(i)));
        }
        PLAYER_WAYPOINTS.put(player, waypoints);
    }
}
