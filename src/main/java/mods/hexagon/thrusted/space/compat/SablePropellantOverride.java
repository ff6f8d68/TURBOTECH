package mods.hexagon.thrusted.space.compat;

import mods.hexagon.thrusted.space.SpaceDimensions;
import net.minecraft.world.level.Level;

public class SablePropellantOverride {

    public static double getThrustMultiplier(Level level) {
        if (level == null) return 1.0;
        var dim = level.dimension();
        if (SpaceDimensions.isOrbitDimension(dim)) return 0.0;
        if (SpaceDimensions.isPlanetDimension(dim)) return 0.0;
        return 1.0;
    }

    public static boolean hasAtmosphere(Level level) {
        if (level == null) return true;
        var dim = level.dimension();
        if (SpaceDimensions.isOrbitDimension(dim)) return false;
        if (SpaceDimensions.isPlanetDimension(dim)) return false;
        return true;
    }
}
