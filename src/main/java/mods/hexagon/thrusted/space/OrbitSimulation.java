package mods.hexagon.thrusted.space;

import mods.hexagon.thrusted.space.body.CelestialBody;
import mods.hexagon.thrusted.space.body.Planet;

import java.util.HashMap;
import java.util.Map;

public class OrbitSimulation {
    private static final double G = 6.67430e-11;
    private static final double AU = 1.496e11;

    private final Map<String, OrbitalState> orbitalStates = new HashMap<>();

    public static class OrbitalState {
        public double meanAnomaly;
        public double trueAnomaly;
        public double radius;
        public final CelestialBody body;

        public OrbitalState(CelestialBody body) {
            this.body = body;
            this.meanAnomaly = Math.random() * 2.0 * Math.PI;
            this.trueAnomaly = 0;
            this.radius = body.getRadius();
        }
    }

    public void updateOrbit(Planet planet, float deltaTime) {
        OrbitalState state = orbitalStates.computeIfAbsent(planet.getName(),
                k -> new OrbitalState(planet));
        double n = 2.0 * Math.PI / (planet.getOrbitalPeriod() * 365.25 * 24 * 3600);
        state.meanAnomaly += n * deltaTime * 1000;
        state.trueAnomaly = solveKepler(state.meanAnomaly, planet.getEccentricity());
        state.radius = planet.getSemiMajorAxis() * AU *
                (1 - planet.getEccentricity() * planet.getEccentricity()) /
                (1 + planet.getEccentricity() * Math.cos(state.trueAnomaly));
    }

    public void updateMoonOrbit(Planet moon, Planet parent, float deltaTime) {
        OrbitalState state = orbitalStates.computeIfAbsent(moon.getName(),
                k -> new OrbitalState(moon));
        state.meanAnomaly += deltaTime * 0.5;
        state.trueAnomaly = solveKepler(state.meanAnomaly, moon.getEccentricity());
    }

    public OrbitalState getOrbitalState(String name) {
        return orbitalStates.get(name);
    }

    private double solveKepler(double meanAnomaly, double eccentricity) {
        double E = meanAnomaly;
        for (int i = 0; i < 10; i++) {
            double dE = (meanAnomaly - (E - eccentricity * Math.sin(E))) / (1 - eccentricity * Math.cos(E));
            E += dE;
            if (Math.abs(dE) < 1e-10) break;
        }
        return 2 * Math.atan2(
                Math.sqrt(1 + eccentricity) * Math.sin(E / 2),
                Math.sqrt(1 - eccentricity) * Math.cos(E / 2)
        );
    }
}
