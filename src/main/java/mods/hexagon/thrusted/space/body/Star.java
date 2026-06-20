package mods.hexagon.thrusted.space.body;

public class Star extends CelestialBody {
    private final double surfaceTemperature;
    private final double luminosity;

    public Star(String name, double posX, double posY, double posZ, double radiusKm,
                double massKg, double surfaceTemperature, int color) {
        super(name, posX, posY, posZ, radiusKm, massKg, color, 0.001);
        this.surfaceTemperature = surfaceTemperature;
        this.luminosity = calculateLuminosity(radiusKm, surfaceTemperature);
    }

    private double calculateLuminosity(double radiusKm, double tempK) {
        double rMeters = radiusKm * 1000.0;
        double sigma = 5.670374419e-8;
        return 4.0 * Math.PI * rMeters * rMeters * sigma * tempK * tempK * tempK * tempK;
    }

    @Override
    public void update(float deltaTime) {
        rotationAngle += rotationSpeed;
    }

    public double getSurfaceTemperature() { return surfaceTemperature; }
    public double getLuminosity() { return luminosity; }
}
