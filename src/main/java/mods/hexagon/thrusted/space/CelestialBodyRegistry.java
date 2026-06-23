package mods.hexagon.thrusted.space;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.api.AtmosphereData;
import mods.hexagon.thrusted.space.api.CelestialBodyType;
import mods.hexagon.thrusted.space.api.GravityData;
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
    public static final String IO = "Io";
    public static final String EUROPA = "Europa";
    public static final String GANYMEDE = "Ganymede";
    public static final String CALLISTO = "Callisto";
    public static final String TITAN = "Titan";
    public static final String ENCELADUS = "Enceladus";
    public static final String MIMAS = "Mimas";
    public static final String RHEA = "Rhea";
    public static final String MIRANDA = "Miranda";
    public static final String ARIEL = "Ariel";
    public static final String TITANIA = "Titania";
    public static final String OBERON = "Oberon";
    public static final String TRITON = "Triton";
    public static final String PLUTO = "Pluto";
    public static final String CHARON = "Charon";
    public static final String CERES = "Ceres";
    public static final String ERIS = "Eris";
    public static final String HAUMEA = "Haumea";
    public static final String MAKEMAKE = "Makemake";
    public static final String SEDNA = "Sedna";
    public static final String VESTA = "Vesta";
    public static final String PALLAS = "Pallas";
    public static final String HALLEY = "Halley";
    public static final String HALE_BOPP = "Hale-Bopp";
    public static final String ASTEROID_BELT = "AsteroidBelt";

    public static void init() {
        long day = 24000L;
        long year = day * 365;

        // === THE SUN ===
        SUN = register(new CelestialBody(SUN_NAME, CelestialBodyType.SUN, null,
                OrbitData.builder()
                        .semiMajorAxis(0)
                        .radius(30)
                        .rotationPeriodTicks(day * 25)
                        .build(),
                tex("sun"), 0xFFF5D742,
                AtmosphereData.builder()
                        .pressure(0).temperature(5778).radiation(10.0)
                        .composition(Map.of("hydrogen", 0.735, "helium", 0.248)).build(),
                GravityData.builder().gravity(28.0).escapeVelocity(617.5).soi(Double.MAX_VALUE).build(),
                "The star at the center of our solar system"));

        // === MERCURY ===
        register(new CelestialBody(MERCURY, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(80).radius(2.5).inclinationDeg(7)
                        .orbitalPeriodTicks(day * 88).eccentricity(0.205)
                        .rotationPeriodTicks(day * 58.6)
                        .build(),
                tex("mercury"), 0xFFB0B0B0,
                AtmosphereData.builder()
                        .pressure(0.0000000001).temperature(440).radiation(3.5)
                        .composition(Map.of("sodium", 0.29, "oxygen", 0.42, "hydrogen", 0.22)).build(),
                GravityData.builder().gravity(0.378).escapeVelocity(4.25).soi(112000.0).build(),
                "Smallest planet, closest to the Sun. Extreme temperature swings."));

        // === VENUS ===
        register(new CelestialBody(VENUS, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(120).radius(4.5).inclinationDeg(3.4)
                        .orbitalPeriodTicks(day * 225).eccentricity(0.007)
                        .rotationPeriodTicks(day * 243)
                        .build(),
                tex("venus"), 0xFFE8C87D,
                AtmosphereData.builder()
                        .pressure(92.0).temperature(737).radiation(0.01).wind(100.0)
                        .toxic(true).corrosive(true)
                        .composition(Map.of("carbon_dioxide", 0.965, "nitrogen", 0.035)).build(),
                GravityData.builder().gravity(0.904).escapeVelocity(10.36).soi(616000.0).build(),
                "Scorching hellworld with crushing atmosphere and sulfuric acid clouds."));

        // === EARTH ===
        CelestialBody earth = new CelestialBody(EARTH, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(170).radius(5).inclinationDeg(0)
                        .orbitalPeriodTicks(year).eccentricity(0.017)
                        .rotationPeriodTicks(day)
                        .build(),
                tex("earth"), 0xFF4A8FE4,
                AtmosphereData.EARTH_LIKE,
                GravityData.EARTH,
                "Our home world. The cradle of humanity.");
        register(earth);

        // === MOON (Luna) ===
        register(new CelestialBody(MOON, CelestialBodyType.MOON, earth,
                OrbitData.builder()
                        .semiMajorAxis(15).radius(1.5).inclinationDeg(5.1)
                        .orbitalPeriodTicks(day * 27.3)
                        .rotationPeriodTicks(day * 27.3)
                        .build(),
                tex("moon"), 0xFFCCCCCC,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(250).radiation(1.0).build(),
                GravityData.builder().gravity(0.166).escapeVelocity(2.38).soi(66100.0).build(),
                "Earth's only natural satellite. Rich in Helium-3."));

        // === MARS ===
        CelestialBody mars = new CelestialBody(MARS, CelestialBodyType.PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(230).radius(3.5).inclinationDeg(1.85)
                        .orbitalPeriodTicks(day * 687).eccentricity(0.093)
                        .rotationPeriodTicks((long)(day * 1.03))
                        .build(),
                tex("mars"), 0xFFD4734A,
                AtmosphereData.builder()
                        .pressure(0.006).temperature(210).radiation(0.7).wind(30.0)
                        .composition(Map.of("carbon_dioxide", 0.953, "nitrogen", 0.027, "argon", 0.016)).build(),
                GravityData.builder().gravity(0.376).escapeVelocity(5.03).soi(577000.0).build(),
                "The Red Planet. Thin atmosphere, dust storms, potential for colonization.");
        register(mars);

        // === PHOBOS ===
        register(new CelestialBody(PHOBOS, CelestialBodyType.MOON, mars,
                OrbitData.builder()
                        .semiMajorAxis(8).radius(0.4).inclinationDeg(1.1)
                        .orbitalPeriodTicks(day * 0.32)
                        .build(),
                tex("phobos"), 0xFF8A8A8A,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.0006).escapeVelocity(0.0114).soi(50.0).build(),
                "Mars' larger moon. Irregular shape, Stickney crater."));

        // === DEIMOS ===
        register(new CelestialBody(DEIMOS, CelestialBodyType.MOON, mars,
                OrbitData.builder()
                        .semiMajorAxis(12).radius(0.3).inclinationDeg(0.9)
                        .orbitalPeriodTicks(day * 1.26)
                        .build(),
                tex("deimos"), 0xFF9A9A9A,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.0003).escapeVelocity(0.0056).soi(20.0).build(),
                "Mars' smaller moon. Smooth surface."));

        // === CERES (Asteroid Belt) ===
        register(new CelestialBody(CERES, CelestialBodyType.DWARF_PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(310).radius(1.2).inclinationDeg(10.6)
                        .orbitalPeriodTicks(day * 1682).eccentricity(0.076)
                        .rotationPeriodTicks((long)(day * 0.378))
                        .build(),
                tex("ceres"), 0xFF9E9E9E,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(168).radiation(0.8).build(),
                GravityData.builder().gravity(0.029).escapeVelocity(0.51).soi(7500.0).build(),
                "Largest object in the asteroid belt. Water ice deposits."));

        // === VESTA ===
        register(new CelestialBody(VESTA, CelestialBodyType.ASTEROID, SUN,
                OrbitData.builder()
                        .semiMajorAxis(290).radius(0.6).inclinationDeg(7.1)
                        .orbitalPeriodTicks(day * 1325).eccentricity(0.089)
                        .rotationPeriodTicks((long)(day * 0.222))
                        .build(),
                tex("vesta"), 0xFFAAAAAA,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.025).escapeVelocity(0.36).soi(3000.0).build(),
                "Second largest asteroid. Differentiated interior with iron core."));

        // === PALLAS ===
        register(new CelestialBody(PALLAS, CelestialBodyType.ASTEROID, SUN,
                OrbitData.builder()
                        .semiMajorAxis(320).radius(0.5).inclinationDeg(34.8)
                        .orbitalPeriodTicks(day * 1686).eccentricity(0.231)
                        .rotationPeriodTicks((long)(day * 0.326))
                        .build(),
                tex("pallas"), 0xFF888888,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.022).escapeVelocity(0.32).soi(2500.0).build(),
                "Highly inclined orbit. B-type asteroid rich in carbon."));

        // === JUPITER ===
        CelestialBody jupiter = new CelestialBody(JUPITER, CelestialBodyType.GAS_GIANT, SUN,
                OrbitData.builder()
                        .semiMajorAxis(400).radius(18).inclinationDeg(1.3)
                        .orbitalPeriodTicks(year * 12).eccentricity(0.049)
                        .rotationPeriodTicks((long)(day * 0.41))
                        .build(),
                tex("jupiter"), 0xFFC88B5A,
                AtmosphereData.builder()
                        .pressure(1000.0).temperature(165).radiation(5.0).wind(150.0)
                        .composition(Map.of("hydrogen", 0.898, "helium", 0.102)).build(),
                GravityData.builder().gravity(2.528).escapeVelocity(59.5).soi(48200000.0).build(),
                "King of planets. Massive radiation belts, Great Red Spot.");
        register(jupiter);

        // === IO ===
        register(new CelestialBody(IO, CelestialBodyType.MOON, jupiter,
                OrbitData.builder()
                        .semiMajorAxis(25).radius(1.6).inclinationDeg(0.04)
                        .orbitalPeriodTicks(day * 1.77)
                        .rotationPeriodTicks(day * 1.77)
                        .build(),
                tex("io"), 0xFFE8D44A,
                AtmosphereData.builder()
                        .pressure(0.00000003).temperature(130).radiation(8.0)
                        .toxic(true)
                        .composition(Map.of("sulfur_dioxide", 0.9, "sulfur", 0.04)).build(),
                GravityData.builder().gravity(0.183).escapeVelocity(2.56).soi(7500.0).build(),
                "Most volcanically active body in the solar system. Extreme radiation."));

        // === EUROPA ===
        register(new CelestialBody(EUROPA, CelestialBodyType.MOON, jupiter,
                OrbitData.builder()
                        .semiMajorAxis(35).radius(1.4).inclinationDeg(0.47)
                        .orbitalPeriodTicks(day * 3.55)
                        .rotationPeriodTicks(day * 3.55)
                        .build(),
                tex("europa"), 0xFFD4CCB8,
                AtmosphereData.builder()
                        .pressure(0.0000001).temperature(102).radiation(5.0)
                        .composition(Map.of("oxygen", 1.0)).build(),
                GravityData.builder().gravity(0.134).escapeVelocity(2.03).soi(9700.0).build(),
                "Subsurface ocean beneath ice shell. Potential for life."));

        // === GANYMEDE ===
        register(new CelestialBody(GANYMEDE, CelestialBodyType.MOON, jupiter,
                OrbitData.builder()
                        .semiMajorAxis(48).radius(2.0).inclinationDeg(0.2)
                        .orbitalPeriodTicks(day * 7.15)
                        .rotationPeriodTicks(day * 7.15)
                        .build(),
                tex("ganymede"), 0xFFA89880,
                AtmosphereData.builder()
                        .pressure(0.000001).temperature(110).radiation(2.0)
                        .composition(Map.of("oxygen", 1.0)).build(),
                GravityData.builder().gravity(0.146).escapeVelocity(2.74).soi(24000.0).build(),
                "Largest moon in the solar system. Has its own magnetosphere."));

        // === CALLISTO ===
        register(new CelestialBody(CALLISTO, CelestialBodyType.MOON, jupiter,
                OrbitData.builder()
                        .semiMajorAxis(65).radius(1.8).inclinationDeg(0.19)
                        .orbitalPeriodTicks(day * 16.7)
                        .rotationPeriodTicks(day * 16.7)
                        .build(),
                tex("callisto"), 0xFF6B5E50,
                AtmosphereData.builder()
                        .pressure(0.0000075).temperature(134).radiation(0.01)
                        .composition(Map.of("carbon_dioxide", 1.0)).build(),
                GravityData.builder().gravity(0.126).escapeVelocity(2.44).soi(15000.0).build(),
                "Heavily cratered. Low radiation, good for bases."));

        // === SATURN ===
        CelestialBody saturn = new CelestialBody(SATURN, CelestialBodyType.GAS_GIANT, SUN,
                OrbitData.builder()
                        .semiMajorAxis(550).radius(15).inclinationDeg(2.49)
                        .orbitalPeriodTicks(year * 29).eccentricity(0.057)
                        .rotationPeriodTicks((long)(day * 0.45))
                        .build(),
                tex("saturn"), 0xFFE0C08A,
                AtmosphereData.builder()
                        .pressure(1000.0).temperature(134).radiation(0.5).wind(500.0)
                        .composition(Map.of("hydrogen", 0.963, "helium", 0.0325)).build(),
                GravityData.builder().gravity(1.065).escapeVelocity(35.5).soi(54500000.0).build(),
                "Ringed giant. Spectacular ring system of ice and rock.",
                true, 18.0, 30.0, 0xAAD4B896);
        register(saturn);

        // === TITAN ===
        register(new CelestialBody(TITAN, CelestialBodyType.MOON, saturn,
                OrbitData.builder()
                        .semiMajorAxis(40).radius(2.2).inclinationDeg(0.33)
                        .orbitalPeriodTicks(day * 16)
                        .rotationPeriodTicks(day * 16)
                        .build(),
                tex("titan"), 0xFFD4A050,
                AtmosphereData.builder()
                        .pressure(1.45).temperature(94).radiation(0.01).wind(1.0)
                        .toxic(true)
                        .composition(Map.of("nitrogen", 0.948, "methane", 0.05, "hydrogen", 0.002)).build(),
                GravityData.builder().gravity(0.138).escapeVelocity(2.64).soi(44000.0).build(),
                "Dense atmosphere. Methane lakes and rain. Only moon with thick atmosphere."));

        // === ENCELADUS ===
        register(new CelestialBody(ENCELADUS, CelestialBodyType.MOON, saturn,
                OrbitData.builder()
                        .semiMajorAxis(18).radius(0.5).inclinationDeg(0.009)
                        .orbitalPeriodTicks(day * 1.37)
                        .rotationPeriodTicks(day * 1.37)
                        .build(),
                tex("enceladus"), 0xFFE8E8F0,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(75).radiation(0.05)
                        .composition(Map.of("water_vapor", 0.91, "nitrogen", 0.04)).build(),
                GravityData.builder().gravity(0.0113).escapeVelocity(0.239).soi(800.0).build(),
                "Geysers of water. Subsurface ocean. Potential for life."));

        // === MIMAS ===
        register(new CelestialBody(MIMAS, CelestialBodyType.MOON, saturn,
                OrbitData.builder()
                        .semiMajorAxis(12).radius(0.3).inclinationDeg(1.57)
                        .orbitalPeriodTicks(day * 0.94)
                        .rotationPeriodTicks(day * 0.94)
                        .build(),
                tex("mimas"), 0xFFB8B8B8,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.0064).escapeVelocity(0.159).soi(300.0).build(),
                "Herschel crater makes it resemble a Death Star."));

        // === RHEA ===
        register(new CelestialBody(RHEA, CelestialBodyType.MOON, saturn,
                OrbitData.builder()
                        .semiMajorAxis(30).radius(0.9).inclinationDeg(0.35)
                        .orbitalPeriodTicks(day * 4.52)
                        .rotationPeriodTicks(day * 4.52)
                        .build(),
                tex("rhea"), 0xFFC0C0C0,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(76).radiation(0.1).build(),
                GravityData.builder().gravity(0.026).escapeVelocity(0.635).soi(5000.0).build(),
                "Second-largest Saturnian moon. Possible tenuous ring."));

        // === URANUS ===
        CelestialBody uranus = new CelestialBody(URANUS, CelestialBodyType.GAS_GIANT, SUN,
                OrbitData.builder()
                        .semiMajorAxis(700).radius(8).inclinationDeg(0.77)
                        .orbitalPeriodTicks(year * 84).eccentricity(0.046)
                        .axialTiltDeg(97.8)
                        .rotationPeriodTicks((long)(day * 0.72))
                        .build(),
                tex("uranus"), 0xFF7EC8E3,
                AtmosphereData.builder()
                        .pressure(1000.0).temperature(76).radiation(0.2).wind(250.0)
                        .composition(Map.of("hydrogen", 0.83, "helium", 0.15, "methane", 0.023)).build(),
                GravityData.builder().gravity(0.886).escapeVelocity(21.3).soi(51800000.0).build(),
                "Ice giant tilted on its side. Extreme seasons.",
                true, 9.5, 12.0, 0x447EC8E3);
        register(uranus);

        // === MIRANDA ===
        register(new CelestialBody(MIRANDA, CelestialBodyType.MOON, uranus,
                OrbitData.builder()
                        .semiMajorAxis(8).radius(0.3).inclinationDeg(4.34)
                        .orbitalPeriodTicks(day * 1.41)
                        .rotationPeriodTicks(day * 1.41)
                        .build(),
                tex("miranda"), 0xFF9A9A9A,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.0079).escapeVelocity(0.193).soi(200.0).build(),
                "Extreme terrain. Verona Rupes - tallest cliff in the solar system."));

        // === ARIEL ===
        register(new CelestialBody(ARIEL, CelestialBodyType.MOON, uranus,
                OrbitData.builder()
                        .semiMajorAxis(12).radius(0.5).inclinationDeg(0.04)
                        .orbitalPeriodTicks(day * 2.52)
                        .rotationPeriodTicks(day * 2.52)
                        .build(),
                tex("ariel"), 0xFFB0B0B0,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.027).escapeVelocity(0.56).soi(2000.0).build(),
                "Brightest Uranian moon. Extensive canyon systems."));

        // === TITANIA ===
        register(new CelestialBody(TITANIA, CelestialBodyType.MOON, uranus,
                OrbitData.builder()
                        .semiMajorAxis(20).radius(0.7).inclinationDeg(0.08)
                        .orbitalPeriodTicks(day * 8.71)
                        .rotationPeriodTicks(day * 8.71)
                        .build(),
                tex("titania"), 0xFFA8A0A0,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.039).escapeVelocity(0.77).soi(5000.0).build(),
                "Largest Uranian moon. Possible subsurface ocean."));

        // === OBERON ===
        register(new CelestialBody(OBERON, CelestialBodyType.MOON, uranus,
                OrbitData.builder()
                        .semiMajorAxis(28).radius(0.65).inclinationDeg(0.07)
                        .orbitalPeriodTicks(day * 13.46)
                        .rotationPeriodTicks(day * 13.46)
                        .build(),
                tex("oberon"), 0xFF8A7E78,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.035).escapeVelocity(0.73).soi(4500.0).build(),
                "Outermost major Uranian moon. Heavily cratered."));

        // === NEPTUNE ===
        CelestialBody neptune = new CelestialBody(NEPTUNE, CelestialBodyType.GAS_GIANT, SUN,
                OrbitData.builder()
                        .semiMajorAxis(850).radius(7.5).inclinationDeg(1.77)
                        .orbitalPeriodTicks(year * 165).eccentricity(0.010)
                        .rotationPeriodTicks((long)(day * 0.67))
                        .build(),
                tex("neptune"), 0xFF3F5EFF,
                AtmosphereData.builder()
                        .pressure(1000.0).temperature(72).radiation(0.3).wind(600.0)
                        .composition(Map.of("hydrogen", 0.80, "helium", 0.19, "methane", 0.015)).build(),
                GravityData.builder().gravity(1.14).escapeVelocity(23.5).soi(86800000.0).build(),
                "Windiest planet. Great Dark Spot. Deep blue color from methane.",
                true, 8.5, 10.0, 0x223F5EFF);
        register(neptune);

        // === TRITON ===
        register(new CelestialBody(TRITON, CelestialBodyType.MOON, neptune,
                OrbitData.builder()
                        .semiMajorAxis(18).radius(1.2).inclinationDeg(156.9)
                        .orbitalPeriodTicks(day * 5.88)
                        .rotationPeriodTicks(day * 5.88)
                        .build(),
                tex("triton"), 0xFFE0D8CC,
                AtmosphereData.builder()
                        .pressure(0.000014).temperature(38).radiation(0.1)
                        .composition(Map.of("nitrogen", 0.99, "methane", 0.01)).build(),
                GravityData.builder().gravity(0.0794).escapeVelocity(1.455).soi(15000.0).build(),
                "Retrograde orbit - captured Kuiper Belt object. Nitrogen geysers."));

        // === DWARF PLANETS (KUIPER BELT) ===

        // === PLUTO ===
        CelestialBody pluto = new CelestialBody(PLUTO, CelestialBodyType.DWARF_PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(1000).radius(1.0).inclinationDeg(17.2)
                        .orbitalPeriodTicks(year * 248).eccentricity(0.248)
                        .rotationPeriodTicks(day * 6.39)
                        .build(),
                tex("pluto"), 0xFFCCB8A0,
                AtmosphereData.builder()
                        .pressure(0.00001).temperature(44).radiation(0.5)
                        .composition(Map.of("nitrogen", 0.97, "methane", 0.025, "carbon_monoxide", 0.005)).build(),
                GravityData.builder().gravity(0.063).escapeVelocity(1.21).soi(5900000.0).build(),
                "Heart-shaped nitrogen glacier. Complex geology despite tiny size.");
        register(pluto);

        // === CHARON ===
        register(new CelestialBody(CHARON, CelestialBodyType.MOON, pluto,
                OrbitData.builder()
                        .semiMajorAxis(5).radius(0.5).inclinationDeg(0.001)
                        .orbitalPeriodTicks(day * 6.39)
                        .rotationPeriodTicks(day * 6.39)
                        .build(),
                tex("charon"), 0xFF808080,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.029).escapeVelocity(0.58).soi(2000.0).build(),
                "Pluto's largest moon. Tidally locked binary system."));

        // === ERIS ===
        register(new CelestialBody(ERIS, CelestialBodyType.DWARF_PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(1400).radius(1.0).inclinationDeg(44.0)
                        .orbitalPeriodTicks(year * 559).eccentricity(0.436)
                        .rotationPeriodTicks(day * 1.08)
                        .build(),
                tex("eris"), 0xFFE0E0E0,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(30).radiation(0.6).build(),
                GravityData.builder().gravity(0.084).escapeVelocity(1.38).soi(8000000.0).build(),
                "Most massive known dwarf planet. Scattered disc."));

        // === HAUMEA ===
        register(new CelestialBody(HAUMEA, CelestialBodyType.DWARF_PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(1100).radius(0.7).inclinationDeg(28.2)
                        .orbitalPeriodTicks(year * 284).eccentricity(0.189)
                        .rotationPeriodTicks((long)(day * 0.163))
                        .build(),
                tex("haumea"), 0xFFD0D0D0,
                AtmosphereData.VACUUM,
                GravityData.builder().gravity(0.045).escapeVelocity(0.84).soi(4000000.0).build(),
                "Elongated shape from rapid rotation. Has a ring system.",
                true, 1.0, 1.5, 0x44FFFFFF));

        // === MAKEMAKE ===
        register(new CelestialBody(MAKEMAKE, CelestialBodyType.DWARF_PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(1200).radius(0.6).inclinationDeg(29.0)
                        .orbitalPeriodTicks(year * 306).eccentricity(0.161)
                        .rotationPeriodTicks((long)(day * 0.953))
                        .build(),
                tex("makemake"), 0xFFCC9966,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(32).radiation(0.6).build(),
                GravityData.builder().gravity(0.05).escapeVelocity(0.8).soi(3500000.0).build(),
                "Reddish-brown color. One of the brightest KBOs."));

        // === SEDNA ===
        register(new CelestialBody(SEDNA, CelestialBodyType.DWARF_PLANET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(2000).radius(0.5).inclinationDeg(11.9)
                        .orbitalPeriodTicks(year * 11400).eccentricity(0.842)
                        .rotationPeriodTicks((long)(day * 0.428))
                        .build(),
                tex("sedna"), 0xFFCC3333,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(12).radiation(0.8).build(),
                GravityData.builder().gravity(0.033).escapeVelocity(0.6).soi(10000000.0).build(),
                "Extremely distant. One of the reddest objects known. Inner Oort Cloud."));

        // === COMETS ===
        register(new CelestialBody(HALLEY, CelestialBodyType.COMET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(600).radius(0.15).inclinationDeg(162.3)
                        .orbitalPeriodTicks(year * 76).eccentricity(0.967)
                        .rotationPeriodTicks(day * 2.2)
                        .build(),
                tex("comet"), 0xFF88CCFF,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(50).radiation(0.3)
                        .composition(Map.of("water_vapor", 0.8, "carbon_dioxide", 0.1, "dust", 0.1)).build(),
                GravityData.builder().gravity(0.00001).escapeVelocity(0.001).soi(10.0).build(),
                "Most famous periodic comet. Visible every 76 years."));

        register(new CelestialBody(HALE_BOPP, CelestialBodyType.COMET, SUN,
                OrbitData.builder()
                        .semiMajorAxis(800).radius(0.3).inclinationDeg(89.4)
                        .orbitalPeriodTicks(year * 2533).eccentricity(0.995)
                        .rotationPeriodTicks(day * 0.49)
                        .build(),
                tex("comet"), 0xFF99DDFF,
                AtmosphereData.builder()
                        .pressure(0.0).temperature(45).radiation(0.2)
                        .composition(Map.of("water_vapor", 0.7, "carbon_monoxide", 0.2, "dust", 0.1)).build(),
                GravityData.builder().gravity(0.0001).escapeVelocity(0.01).soi(50.0).build(),
                "Great Comet of 1997. Unusually large nucleus."));

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

    public static List<CelestialBody> getLandable() {
        return BODIES.values().stream().filter(CelestialBody::isLandable).toList();
    }

    public static List<CelestialBody> getMoonsOf(String parentName) {
        return BODIES.values().stream()
                .filter(b -> b.getParent() != null && b.getParent().getName().equals(parentName))
                .toList();
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "textures/space/" + name + ".png");
    }

    private CelestialBodyRegistry() {}
}
