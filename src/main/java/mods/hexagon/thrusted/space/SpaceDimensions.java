package mods.hexagon.thrusted.space;

import dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry;
import mods.hexagon.thrusted.Thrusted;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.*;

public class SpaceDimensions {
    private static final List<String> PLANET_NAMES = List.of(
            "mercury", "venus", "earth", "mars", "jupiter",
            "saturn", "uranus", "neptune", "moon"
    );
    public static final int PLANET_HEIGHT = 380;
    public static final int ORBIT_RETURN_HEIGHT = -60;

    public static final ResourceLocation ORBIT_DIM_TYPE = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "orbit_type");
    public static final ResourceLocation PLANET_DIM_TYPE = ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "planet_type");

    private static final Map<ResourceKey<Level>, ResourceKey<Level>> PLANET_TO_ORBIT = new HashMap<>();
    private static final Map<ResourceKey<Level>, ResourceKey<Level>> ORBIT_TO_PLANET = new HashMap<>();
    private static final Map<String, ResourceKey<Level>> PLANET_KEYS = new HashMap<>();
    private static final Set<ResourceKey<Level>> ORBIT_DIMENSIONS = new HashSet<>();
    private static boolean initialized = false;

    public static ResourceKey<Level> planetKey(String planetName) {
        String key = planetName.toLowerCase(Locale.ROOT);
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "planet_" + key));
    }

    public static ResourceKey<Level> orbitKey(String planetName) {
        String key = planetName.toLowerCase(Locale.ROOT);
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "orbit_" + key));
    }

    public static ResourceKey<Level> getOrbit(ResourceKey<Level> planetDim) {
        return PLANET_TO_ORBIT.get(planetDim);
    }

    public static ResourceKey<Level> getPlanet(ResourceKey<Level> orbitDim) {
        return ORBIT_TO_PLANET.get(orbitDim);
    }

    public static boolean isOrbitDimension(ResourceKey<Level> dim) {
        return ORBIT_DIMENSIONS.contains(dim);
    }

    public static boolean isPlanetDimension(ResourceKey<Level> dim) {
        return PLANET_TO_ORBIT.containsKey(dim);
    }

    public static ResourceKey<Level> getPlanetKey(String name) {
        return PLANET_KEYS.get(name);
    }

    public static void registerDimensions(MinecraftServer server) {
        if (initialized) return;

        DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(server);
        HolderGetter<DimensionType> dimTypes = server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);

        Holder<DimensionType> spaceDimType = dimTypes.get(ResourceKey.create(Registries.DIMENSION_TYPE, ORBIT_DIM_TYPE))
                .orElseThrow(() -> new IllegalStateException("Missing space dimension type: " + ORBIT_DIM_TYPE));

        ChunkGenerator voidGen = createVoidGenerator(server);

        ResourceKey<Level> overworldKey = Level.OVERWORLD;
        ResourceKey<Level> earthOrbitKey = orbitKey("earth");
        PLANET_TO_ORBIT.put(overworldKey, earthOrbitKey);
        ORBIT_TO_PLANET.put(earthOrbitKey, overworldKey);
        PLANET_KEYS.put("overworld", overworldKey);

        for (String name : PLANET_NAMES) {
            ResourceKey<Level> planetKey = planetKey(name);
            ResourceKey<Level> orbitKey = orbitKey(name);

            PLANET_KEYS.put(name, planetKey);
            PLANET_TO_ORBIT.put(planetKey, orbitKey);
            ORBIT_TO_PLANET.put(orbitKey, planetKey);
            ORBIT_DIMENSIONS.add(orbitKey);

            ServerLevel level = registry.createDynamicDimension(
                    orbitKey.location(),
                    voidGen,
                    spaceDimType.value()
            );
            Thrusted.LOGGER.info("Registered orbit dimension {}: level={}", orbitKey.location(), level);
        }

        initialized = true;
    }

    private static ChunkGenerator createVoidGenerator(MinecraftServer server) {
        var registryAccess = server.registryAccess();
        Holder.Reference<Biome> plains = registryAccess.lookupOrThrow(Registries.BIOME)
                .getOrThrow(Biomes.PLAINS);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
                Optional.empty(),
                plains,
                List.of()
        );
        settings = settings.withBiomeAndLayers(
                List.of(new FlatLayerInfo(1, net.minecraft.world.level.block.Blocks.AIR)),
                Optional.empty(),
                plains
        );
        return new FlatLevelSource(settings);
    }

    public static ServerLevel getOrCreateOrbit(ServerLevel planetLevel, String planetName) {
        ResourceKey<Level> orbitKey = orbitKey(planetName);
        ServerLevel orbitLevel = planetLevel.getServer().getLevel(orbitKey);
        if (orbitLevel == null) {
            var dimTypeLookup = planetLevel.getServer().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
            var spaceHolder = dimTypeLookup.get(ResourceKey.create(Registries.DIMENSION_TYPE, ORBIT_DIM_TYPE))
                    .orElseThrow(() -> new IllegalStateException("Missing space type"));
            var registry = DynamicDimensionRegistry.from(planetLevel.getServer());
            orbitLevel = registry.createDynamicDimension(
                    orbitKey.location(),
                    createVoidGenerator(planetLevel.getServer()),
                    spaceHolder.value()
            );
        }
        return orbitLevel;
    }

    public static List<String> getPlanetNames() {
        return PLANET_NAMES;
    }

    private SpaceDimensions() {}
}
