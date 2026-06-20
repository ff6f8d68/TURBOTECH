package mods.hexagon.thrusted.space.body;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class Planet extends CelestialBody {
    public static final double AU_SCALE = 1000.0;

    private final double orbitalPeriod;
    private final double semiMajorAxis;
    private final double eccentricity;
    private final double axialTilt;
    private final List<Planet> moons;
    private CelestialBody parent;
    private double orbitalAngle;

    public Planet(String name, double orbitalRadiusKm, double radiusKm, double massKg,
                  double orbitalPeriodYears, double semiMajorAxisAU, double eccentricity,
                  int color, double axialTilt) {
        super(name, orbitalRadiusKm, 0, 0, radiusKm, massKg, color, 0.01);
        this.orbitalPeriod = orbitalPeriodYears;
        this.semiMajorAxis = semiMajorAxisAU;
        this.eccentricity = eccentricity;
        this.axialTilt = axialTilt;
        this.moons = new ArrayList<>();
        this.parent = null;
        this.orbitalAngle = Math.random() * 2.0 * Math.PI;
        updateOrbitalPosition(0);
    }

    public Planet(String name, double orbitalRadiusKm, double radiusKm, double massKg,
                  double orbitalPeriodYears, double semiMajorAxisAU, double eccentricity,
                  int color, double axialTilt, CelestialBody parent) {
        super(name, orbitalRadiusKm, 0, 0, radiusKm, massKg, color, 0.02);
        this.orbitalPeriod = orbitalPeriodYears;
        this.semiMajorAxis = semiMajorAxisAU;
        this.eccentricity = eccentricity;
        this.axialTilt = axialTilt;
        this.moons = new ArrayList<>();
        this.parent = parent;
        this.orbitalAngle = Math.random() * 2.0 * Math.PI;
    }

    public void setParent(CelestialBody parent) {
        this.parent = parent;
    }

    @Override
    public void update(float deltaTime) {
        updateOrbitalPosition(deltaTime);
        rotationAngle += rotationSpeed;
    }

    private void updateOrbitalPosition(float deltaTime) {
        double speed = (parent == null) ? 0.05 : 0.5;
        orbitalAngle += deltaTime * speed / Math.max(orbitalPeriod, 0.001);
        double rAu = semiMajorAxis * (1 - eccentricity * eccentricity) / (1 + eccentricity * Math.cos(orbitalAngle));
        double r = rAu * AU_SCALE;
        double x = r * Math.cos(orbitalAngle);
        double z = r * Math.sin(orbitalAngle);
        if (parent == null) {
            this.position = new Vec3(x, 0, z);
        } else {
            this.position = new Vec3(parent.getPosition().x + x, 0, parent.getPosition().z + z);
        }
    }

    public void addMoon(Planet moon) {
        moons.add(moon);
        moon.setParent(this);
    }

    public List<Planet> getMoons() { return moons; }
    public double getOrbitalPeriod() { return orbitalPeriod; }
    public double getSemiMajorAxis() { return semiMajorAxis; }
    public double getEccentricity() { return eccentricity; }
    public double getAxialTilt() { return axialTilt; }
    public double getOrbitalAngle() { return orbitalAngle; }
    public CelestialBody getParent() { return parent; }
    public boolean isMoon() { return parent != null; }
}
