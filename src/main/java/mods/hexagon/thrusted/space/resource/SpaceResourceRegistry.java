package mods.hexagon.thrusted.space.resource;

import java.util.*;

public class SpaceResourceRegistry {
    private static final Map<String, SpaceResource> RESOURCES = new LinkedHashMap<>();

    public static void init() {
        register(new SpaceResource("helium_3", "Helium-3", 0xFFAADDFF, 0.3,
                List.of("Moon", "Jupiter", "Saturn", "Uranus"),
                SpaceResource.ResourceCategory.FUEL));

        register(new SpaceResource("titanium_ore", "Titanium Ore", 0xFF8899AA, 0.5,
                List.of("Mercury", "Moon", "Vesta", "Mars"),
                SpaceResource.ResourceCategory.ORE));

        register(new SpaceResource("platinum_ore", "Platinum Ore", 0xFFE0E0D0, 0.15,
                List.of("Mercury", "Vesta", "Pallas", "Ceres"),
                SpaceResource.ResourceCategory.ORE));

        register(new SpaceResource("ice_water", "Water Ice", 0xFFCCEEFF, 0.7,
                List.of("Europa", "Enceladus", "Ceres", "Moon", "Mars", "Pluto", "Triton"),
                SpaceResource.ResourceCategory.ICE));

        register(new SpaceResource("methane_ice", "Methane Ice", 0xFF88AACC, 0.5,
                List.of("Titan", "Pluto", "Triton", "Neptune"),
                SpaceResource.ResourceCategory.ICE));

        register(new SpaceResource("ammonia_ice", "Ammonia Ice", 0xFF99BB88, 0.4,
                List.of("Jupiter", "Saturn", "Uranus", "Neptune"),
                SpaceResource.ResourceCategory.ICE));

        register(new SpaceResource("sulfur", "Volcanic Sulfur", 0xFFDDCC00, 0.8,
                List.of("Io", "Venus"),
                SpaceResource.ResourceCategory.ORE));

        register(new SpaceResource("iron_ore_space", "Space Iron", 0xFFAA6633, 0.6,
                List.of("Mercury", "Mars", "Vesta", "Moon", "Phobos", "Deimos"),
                SpaceResource.ResourceCategory.ORE));

        register(new SpaceResource("regolith", "Lunar Regolith", 0xFF999999, 0.9,
                List.of("Moon", "Phobos", "Deimos", "Callisto"),
                SpaceResource.ResourceCategory.ORE));

        register(new SpaceResource("deuterium", "Deuterium", 0xFF4488FF, 0.25,
                List.of("Jupiter", "Saturn", "Uranus", "Neptune"),
                SpaceResource.ResourceCategory.FUEL));

        register(new SpaceResource("antimatter_trace", "Antimatter Traces", 0xFFFF00FF, 0.01,
                List.of("Jupiter", "Saturn"),
                SpaceResource.ResourceCategory.EXOTIC));

        register(new SpaceResource("dark_matter_residue", "Dark Matter Residue", 0xFF220044, 0.005,
                List.of("Sedna", "Eris"),
                SpaceResource.ResourceCategory.EXOTIC));

        register(new SpaceResource("nitrogen_ice", "Nitrogen Ice", 0xFFAABBFF, 0.6,
                List.of("Pluto", "Triton"),
                SpaceResource.ResourceCategory.ICE));

        register(new SpaceResource("carbon_fiber_raw", "Carbon Compounds", 0xFF333333, 0.4,
                List.of("Titan", "Venus", "Pallas"),
                SpaceResource.ResourceCategory.ORGANIC));

        register(new SpaceResource("tholins", "Tholins", 0xFFCC6633, 0.35,
                List.of("Titan", "Pluto", "Sedna", "Makemake"),
                SpaceResource.ResourceCategory.ORGANIC));

        register(new SpaceResource("silicates", "Silicate Minerals", 0xFFBBAA88, 0.7,
                List.of("Mercury", "Venus", "Mars", "Moon", "Io"),
                SpaceResource.ResourceCategory.ORE));

        register(new SpaceResource("uranium_ore_space", "Uraninite", 0xFF44AA44, 0.1,
                List.of("Mercury", "Moon", "Ceres"),
                SpaceResource.ResourceCategory.ORE));

        register(new SpaceResource("iridium_ore", "Iridium Ore", 0xFFDDDDEE, 0.08,
                List.of("Vesta", "Pallas", "Mercury"),
                SpaceResource.ResourceCategory.ORE));

        register(new SpaceResource("exotic_matter", "Exotic Matter", 0xFF8800FF, 0.002,
                List.of("Sedna"),
                SpaceResource.ResourceCategory.EXOTIC));

        register(new SpaceResource("metallic_hydrogen", "Metallic Hydrogen", 0xFF6688FF, 0.02,
                List.of("Jupiter"),
                SpaceResource.ResourceCategory.EXOTIC));

        register(new SpaceResource("cryo_volatiles", "Cryogenic Volatiles", 0xFF88DDFF, 0.5,
                List.of("Enceladus", "Europa", "Triton", "Pluto"),
                SpaceResource.ResourceCategory.GAS));
    }

    private static void register(SpaceResource resource) {
        RESOURCES.put(resource.id(), resource);
    }

    public static SpaceResource get(String id) {
        return RESOURCES.get(id);
    }

    public static Collection<SpaceResource> getAll() {
        return RESOURCES.values();
    }

    public static List<SpaceResource> getResourcesFor(String bodyName) {
        return RESOURCES.values().stream()
                .filter(r -> r.foundOn().contains(bodyName))
                .toList();
    }

    public static List<SpaceResource> getByCategory(SpaceResource.ResourceCategory category) {
        return RESOURCES.values().stream()
                .filter(r -> r.category() == category)
                .toList();
    }
}
