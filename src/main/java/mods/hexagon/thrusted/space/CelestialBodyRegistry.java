package mods.hexagon.thrusted.space;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.api.CelestialBodyType;
import mods.hexagon.thrusted.space.api.OrbitData;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class CelestialBodyRegistry {
    private static final Map<String, CelestialBody> BODIES = new LinkedHashMap<>();
    private static CelestialBody SUN;

    public static final String SUN_NAME = "Sun";
    public static final String MERCURY = "Mercury";
    public static final String VENUS = "Venus";
    public static final String EARTH = "Earth";
    public static final String MARS = "Mars";
    public static final String JUPITER = "Jupiter";
    public static final String SATURN = "Saturn";
    public static final String URANUS = "Uranus";
    public static final String NEPTUNE = "Neptune";
    public static final String MOON = "Moon";
    public static final String PHOBOS = "Phobos";
    public static final String DEIMOS = "Deimos";
    public static final String EUROPA = "Europa";
    public static final String TITAN = "Titan";

    public static void init() {
        long day = 24000L;
        long year = day * 365;

        SUN = register(new CelestialBody(SUN_NAME, CelestialBodyType.SUN, null,
                OrbitData.builder()
                        .semiMajorAxis(0)
                        .radius(30)
                        .rotationPeriodTicks(day * 25)
                        .build(),
                tex("sun"), 0xFFF5D742));

        register(new CelestialBody(MERCURY, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(80).radius(2.5).inclinationDeg(7)
                        .orbitalPeriodTicks(day * 88).eccentricity(0.205)
                        .build(),
                tex("mercury"), 0xFFB0B0B0));

        register(new CelestialBody(VENUS, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(120).radius(4.5).inclinationDeg(3.4)
                        .orbitalPeriodTicks(day * 225).eccentricity(0.007)
                        .build(),
                tex("venus"), 0xFFE8C87D));

        CelestialBody earth = new CelestialBody(EARTH, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(170).radius(5).inclinationDeg(0)
                        .orbitalPeriodTicks(year).eccentricity(0.017)
                        .build(),
                tex("earth"), 0xFF4A8FE4);
        register(earth);

        register(new CelestialBody(MOON, CelestialBodyType.MOON, earth,
                OrbitData.builder()
                        .semiMajorAxis(15).radius(1.5).inclinationDeg(5.1)
                        .orbitalPeriodTicks(day * 27.3)
                        .build(),
                tex("moon"), 0xFFCCCCCC));

        CelestialBody mars = new CelestialBody(MARS, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(230).radius(3.5).inclinationDeg(1.85)
                        .orbitalPeriodTicks(day * 687).eccentricity(0.093)
                        .build(),
                tex("mars"), 0xFFD4734A);
        register(mars);

        register(new CelestialBody(PHOBOS, CelestialBodyType.MOON, mars,
                OrbitData.builder()
                        .semiMajorAxis(8).radius(0.4).inclinationDeg(1.1)
                        .orbitalPeriodTicks(day * 0.32)
                        .build(),
                tex("phobos"), 0xFF8A8A8A));

        register(new CelestialBody(DEIMOS, CelestialBodyType.MOON, mars,
                OrbitData.builder()
                        .semiMajorAxis(12).radius(0.3).inclinationDeg(0.9)
                        .orbitalPeriodTicks(day * 1.26)
                        .build(),
                tex("deimos"), 0xFF9A9A9A));

        register(new CelestialBody(JUPITER, CelestialBodyType.GAS_GIANT, SUN,
                OrbitData.builder()
                        .semiMajorAxis(400).radius(18).inclinationDeg(1.3)
                        .orbitalPeriodTicks(year * 12).eccentricity(0.049)
                        .rotationPeriodTicks(day * 0.41)
                        .build(),
                tex("jupiter"), 0xFFC88B5A));

        register(new CelestialBody(SATURN, CelestialBodyType.GAS_GIANT, SUN,
                OrbitData.builder()
                        .semiMajorAxis(550).radius(15).inclinationDeg(2.49)
                        .orbitalPeriodTicks(year * 29).eccentricity(0.057)
                        .rotationPeriodTicks(day * 0.45)
                        .build(),
                tex("saturn"), 0xFFE0C08A));

        register(new CelestialBody(URANUS, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(700).radius(8).inclinationDeg(0.77)
                        .orbitalPeriodTicks(year * 84).eccentricity(0.046)
                        .axialTiltDeg(97.8)
                        .build(),
                tex("uranus"), 0xFF7EC8E3));

        register(new CelestialBody(NEPTUNE, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(850).radius(7.5).inclinationDeg(1.77)
                        .orbitalPeriodTicks(year * 165).eccentricity(0.010)
                        .build(),
                tex("neptune"), 0xFF3F5EFF));

        BODIES.values().forEach(b -> Thrusted.LOGGER.info("Registered celestial body: {}", b.getName()));
    }

    private static CelestialBody register(CelestialBody body) {
        BODIES.put(body.getName(), body);
        return body;
    }

    public static CelestialBody get(String name) {
        return BODIES.get(name);
    }

    public static Collection<CelestialBody> getAll() {
        return BODIES.values();
    }

    public static CelestialBody getSun() {
        return SUN;
    }

    public static List<CelestialBody> getByType(CelestialBodyType type) {
        return BODIES.values().stream().filter(b -> b.getType() == type).toList();
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "textures/space/" + name + ".png");
    }

    private CelestialBodyRegistry() {}
}
