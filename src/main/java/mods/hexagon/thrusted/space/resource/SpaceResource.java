package mods.hexagon.thrusted.space.resource;

import java.util.List;
import java.util.Map;

public record SpaceResource(
        String id,
        String displayName,
        int color,
        double baseRarity,
        List<String> foundOn,
        ResourceCategory category
) {
    public enum ResourceCategory {
        ORE,
        GAS,
        ICE,
        ORGANIC,
        EXOTIC,
        FUEL
    }
}
