package mods.hexagon.thrusted.space.api;

public record GravityData(
        double surfaceGravityG,
        double escapeVelocityKms,
        double hillSphereRadius,
        double soiRadius
) {
    public static final GravityData ZERO = new GravityData(0.0, 0.0, 0.0, 0.0);
    public static final GravityData EARTH = new GravityData(1.0, 11.2, 1500000.0, 929000.0);

    public double getGravityAcceleration() {
        return surfaceGravityG * 9.81;
    }

    public double getMinecraftGravityMultiplier() {
        return surfaceGravityG;
    }

    public boolean canEscape(double velocityKms) {
        return velocityKms >= escapeVelocityKms;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double surfaceGravityG = 0.0;
        private double escapeVelocityKms = 0.0;
        private double hillSphereRadius = 0.0;
        private double soiRadius = 0.0;

        public Builder gravity(double g) { this.surfaceGravityG = g; return this; }
        public Builder escapeVelocity(double kms) { this.escapeVelocityKms = kms; return this; }
        public Builder hillSphere(double r) { this.hillSphereRadius = r; return this; }
        public Builder soi(double r) { this.soiRadius = r; return this; }

        public GravityData build() {
            return new GravityData(surfaceGravityG, escapeVelocityKms, hillSphereRadius, soiRadius);
        }
    }
}
