package mods.hexagon.thrusted.space.dimension;

import com.mojang.logging.LogUtils;
import mods.hexagon.thrusted.Thrusted;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import org.slf4j.Logger;

import java.util.*;

public class PlanetDimensionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ResourceKey<Level>> planetDimensions = new HashMap<>();
    private static final Map<String, ServerLevel> loadedDimensions = new HashMap<>();
    private static MinecraftServer server;

    public static void init(MinecraftServer srv) {
        server = srv;
        registerSpaceDimension();
        registerPlanetDimensions();
        preloadDimensions();
        LOGGER.info("Planet Dimension Manager initialized with {} dimensions", loadedDimensions.size());
    }

    private static void registerSpaceDimension() {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "space"));
        planetDimensions.put("space", key);
    }

    private static void registerPlanetDimensions() {
        String[] planets = {"mercury", "venus", "earth", "mars", "jupiter", "saturn", "uranus", "neptune"};
        for (String planet : planets) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "planet_" + planet));
            planetDimensions.put(planet, key);
        }
    }

    private static void preloadDimensions() {
        for (var entry : planetDimensions.entrySet()) {
            ServerLevel level = server.getLevel(entry.getValue());
            if (level != null) {
                loadedDimensions.put(entry.getKey(), level);
                LOGGER.debug("Pre-loaded dimension: {}", entry.getKey());
            } else {
                LOGGER.warn("Dimension {} not yet loaded, will try on demand", entry.getKey());
            }
        }
    }

    public static void transferToSpace(ServerPlayer player) {
        ResourceKey<Level> spaceKey = planetDimensions.get("space");
        if (spaceKey == null) {
            LOGGER.error("Space dimension not registered!");
            return;
        }
        ServerLevel spaceLevel = getOrCreateDimension(spaceKey, "space");
        if (spaceLevel != null) {
            BlockPos spawn = new BlockPos(0, 200, 0);
            player.teleportTo(spaceLevel, spawn.getX(), spawn.getY(), spawn.getZ(), player.getYRot(), player.getXRot());
            LOGGER.info("{} transferred to space", player.getName().getString());
        } else {
            LOGGER.error("Failed to load space dimension!");
        }
    }

    public static void transferToPlanet(ServerPlayer player, String planetName) {
        ResourceKey<Level> planetKey = planetDimensions.get(planetName);
        if (planetKey == null) {
            LOGGER.error("Planet dimension {} not registered!", planetName);
            return;
        }
        ServerLevel planetLevel = getOrCreateDimension(planetKey, planetName);
        if (planetLevel != null) {
            BlockPos spawn = new BlockPos(0, 100, 0);
            player.teleportTo(planetLevel, spawn.getX(), spawn.getY(), spawn.getZ(), player.getYRot(), player.getXRot());
            LOGGER.info("{} transferred to {}", player.getName().getString(), planetName);
        } else {
            LOGGER.error("Failed to load planet dimension {}!", planetName);
        }
    }

    private static ServerLevel getOrCreateDimension(ResourceKey<Level> key, String name) {
        if (loadedDimensions.containsKey(name)) {
            return loadedDimensions.get(name);
        }

        // Try to get existing level (datapack dimensions are auto-loaded)
        ServerLevel existing = server.getLevel(key);
        if (existing != null) {
            loadedDimensions.put(name, existing);
            return existing;
        }

        // Force load from LevelStem registry
        try {
            Registry<LevelStem> stemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
            ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, key.location());
            if (stemRegistry.containsKey(stemKey)) {
                // The dimension exists in data - the level might need to be forced
                for (ServerLevel loaded : server.getAllLevels()) {
                    if (loaded.dimension().location().equals(key.location())) {
                        loadedDimensions.put(name, loaded);
                        return loaded;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking registry for dimension {}", name, e);
        }

        LOGGER.error("Could not load dimension {}. Make sure {} is defined in datapack.",
                name, key.location());
        return null;
    }

    public static boolean isSpaceDimension(Level level) {
        String path = level.dimension().location().getPath();
        return path.equals("space") || path.startsWith("planet_");
    }

    public static void saveAssignments(MinecraftServer server) {
        LOGGER.info("Saving planet dimension assignments");
    }

    public static void cleanup() {
        loadedDimensions.clear();
        server = null;
    }

    public static void onChunkLoad(net.minecraft.world.level.chunk.LevelChunk chunk, Level level) {
    }

    public static ResourceKey<Level> getPlanetKey(String name) {
        return planetDimensions.get(name);
    }

    public static Map<String, ResourceKey<Level>> getPlanetDimensions() {
        return planetDimensions;
    }
}
