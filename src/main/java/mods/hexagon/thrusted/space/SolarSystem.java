package mods.hexagon.thrusted.space;

import mods.hexagon.thrusted.space.body.Planet;
import mods.hexagon.thrusted.space.body.Star;

import java.util.ArrayList;
import java.util.List;

public class SolarSystem {
    private final Star sun;
    private final List<Planet> planets;
    private final List<mods.hexagon.thrusted.space.body.CelestialBody> allBodies;
    private float simulationTime;

    public SolarSystem() {
        this.sun = createSun();
        this.planets = createPlanets();
        this.allBodies = new ArrayList<>();
        allBodies.add(sun);
        allBodies.addAll(planets);
        this.simulationTime = 0;
    }

    private Star createSun() {
        return new Star("sun", 0, 0, 0, 696340.0, 1.989e30, 5778.0, 0xFFFFC800);
    }

    private List<Planet> createPlanets() {
        List<Planet> list = new ArrayList<>();

        Planet mercury = new Planet("mercury", 5.79e7, 2439.7, 3.301e23, 0.24, 0.387, 0.2056, 0x8C8C8C, 0);
        list.add(mercury);

        Planet venus = new Planet("venus", 1.082e8, 6051.8, 4.867e24, 0.615, 0.723, 0.0067, 0xE6CBA8, 177.3);
        list.add(venus);

        Planet earth = new Planet("earth", 1.496e8, 6371.0, 5.972e24, 1.0, 1.0, 0.0167, 0x4A8F5C, 23.5);
        Planet moon = new Planet("moon", 384400, 1737.4, 7.342e22, 0.0748, 0.00257, 0.0549, 0xC0C0C0, 5.14);
        earth.addMoon(moon);
        list.add(earth);

        Planet mars = new Planet("mars", 2.279e8, 3389.5, 6.39e23, 1.88, 1.524, 0.0934, 0xE27B58, 25.19);
        Planet phobos = new Planet("phobos", 9376, 11.1, 1.0659e16, 8.0e-4, 6.0e-5, 0.0151, 0x8B7355, 1.09);
        mars.addMoon(phobos);
        Planet deimos = new Planet("deimos", 23463, 6.2, 1.4762e15, 0.003, 1.5e-4, 3.0e-4, 0xA09F8C, 1.79);
        mars.addMoon(deimos);
        list.add(mars);

        Planet jupiter = new Planet("jupiter", 7.785e8, 69911.0, 1.898e27, 11.86, 5.203, 0.0489, 0xD4A574, 3.13);
        Planet io = new Planet("io", 421700, 1821.6, 8.9319e22, 0.004, 0.0028, 0.0041, 0xFFFF99, 0.04);
        jupiter.addMoon(io);
        Planet europa = new Planet("europa", 670900, 1560.8, 4.7998e22, 0.009, 0.0045, 0.009, 0xC8B88C, 0.47);
        jupiter.addMoon(europa);
        Planet ganymede = new Planet("ganymede", 1070400, 2634.1, 1.4819e23, 0.02, 0.0072, 0.013, 0xB8B8B8, 0.33);
        jupiter.addMoon(ganymede);
        Planet callisto = new Planet("callisto", 1882700, 2410.3, 1.0759e23, 0.045, 0.0126, 0.007, 0x808080, 0.36);
        jupiter.addMoon(callisto);
        list.add(jupiter);

        Planet saturn = new Planet("saturn", 1.434e9, 58232.0, 5.683e26, 29.46, 9.537, 0.0565, 0xE8D5A3, 26.73);
        Planet titan = new Planet("titan", 1221870, 2574.7, 1.3452e23, 0.045, 0.0082, 0.029, 0xCC9966, 0.33);
        saturn.addMoon(titan);
        Planet enceladus = new Planet("enceladus", 237948, 252.1, 1.08e20, 0.003, 0.0016, 0.0047, 0xFFFFFF, 0.02);
        saturn.addMoon(enceladus);
        list.add(saturn);

        Planet uranus = new Planet("uranus", 2.871e9, 25362.0, 8.681e25, 84.01, 19.19, 0.0444, 0x7EC8E3, 97.77);
        list.add(uranus);

        Planet neptune = new Planet("neptune", 4.495e9, 24622.0, 1.024e26, 164.8, 30.07, 0.0112, 0x3355FF, 28.32);
        list.add(neptune);

        return list;
    }

    public void update(float deltaTime) {
        simulationTime += deltaTime;
        for (Planet planet : planets) {
            planet.update(deltaTime);
            for (Planet moon : planet.getMoons()) {
                moon.update(deltaTime);
            }
        }
    }

    public Star getSun() { return sun; }
    public List<Planet> getPlanets() { return planets; }
    public List<mods.hexagon.thrusted.space.body.CelestialBody> getAllBodies() { return allBodies; }
    public float getSimulationTime() { return simulationTime; }

    public mods.hexagon.thrusted.space.body.CelestialBody findBodyByName(String name) {
        if (sun.getName().equalsIgnoreCase(name)) return sun;
        for (Planet planet : planets) {
            if (planet.getName().equalsIgnoreCase(name)) return planet;
            for (Planet moon : planet.getMoons()) {
                if (moon.getName().equalsIgnoreCase(name)) return moon;
            }
        }
        return null;
    }
}
