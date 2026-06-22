package mods.hexagon.thrusted.space.api;

import net.minecraft.util.Mth;
import org.joml.Vector3d;

public record OrbitData(
        double semiMajorAxis,
        double eccentricity,
        double inclinationRad,
        double longitudeOfAscendingNodeRad,
        double argumentOfPeriapsisRad,
        double meanAnomalyAtEpochRad,
        double orbitalPeriodTicks,
        double rotationPeriodTicks,
        double axialTiltRad,
        double radius
) {
    public static final double TICKS_PER_ORBIT = 24000.0;

    public static Builder builder() {
        return new Builder();
    }

    public Vector3d getPosition(long gameTime) {
        double elapsedTicks = gameTime % orbitalPeriodTicks;
        double M = meanAnomalyAtEpochRad + 2.0 * Math.PI * (elapsedTicks / orbitalPeriodTicks);
        M = M % (2.0 * Math.PI);
        double E = solveKepler(M, eccentricity);
        double xOrb = semiMajorAxis * (Math.cos(E) - eccentricity);
        double yOrb = semiMajorAxis * Math.sqrt(1.0 - eccentricity * eccentricity) * Math.sin(E);

        double cosO = Math.cos(longitudeOfAscendingNodeRad);
        double sinO = Math.sin(longitudeOfAscendingNodeRad);
        double cosW = Math.cos(argumentOfPeriapsisRad);
        double sinW = Math.sin(argumentOfPeriapsisRad);
        double cosI = Math.cos(inclinationRad);
        double sinI = Math.sin(inclinationRad);

        double xEcl = (cosO * cosW - sinO * sinW * cosI) * xOrb + (-cosO * sinW - sinO * cosW * cosI) * yOrb;
        double yEcl = (sinO * cosW + cosO * sinW * cosI) * xOrb + (-sinO * sinW + cosO * cosW * cosI) * yOrb;
        double zEcl = (sinW * sinI) * xOrb + (cosW * sinI) * yOrb;

        return new Vector3d(xEcl, yEcl, zEcl);
    }

    public float getRotationAngle(long gameTime) {
        if (rotationPeriodTicks <= 0) return 0.0f;
        double elapsed = gameTime % rotationPeriodTicks;
        return (float) (2.0 * Math.PI * (elapsed / rotationPeriodTicks));
    }

    private static double solveKepler(double M, double e) {
        double E = M;
        for (int i = 0; i < 32; i++) {
            double dE = (M - E + e * Math.sin(E)) / (1.0 - e * Math.cos(E));
            E += dE;
            if (Math.abs(dE) < 1e-12) break;
        }
        return E;
    }

    public static class Builder {
        private double semiMajorAxis = 100.0;
        private double eccentricity = 0.0;
        private double inclinationRad = 0.0;
        private double longitudeOfAscendingNodeRad = 0.0;
        private double argumentOfPeriapsisRad = 0.0;
        private double meanAnomalyAtEpochRad = 0.0;
        private double orbitalPeriodTicks = TICKS_PER_ORBIT;
        private double rotationPeriodTicks = TICKS_PER_ORBIT;
        private double axialTiltRad = 0.0;
        private double radius = 1.0;

        public Builder semiMajorAxis(double v) { this.semiMajorAxis = v; return this; }
        public Builder eccentricity(double v) { this.eccentricity = v; return this; }
        public Builder inclinationDeg(double deg) { this.inclinationRad = Math.toRadians(deg); return this; }
        public Builder longitudeOfAscendingNodeDeg(double deg) { this.longitudeOfAscendingNodeRad = Math.toRadians(deg); return this; }
        public Builder argumentOfPeriapsisDeg(double deg) { this.argumentOfPeriapsisRad = Math.toRadians(deg); return this; }
        public Builder meanAnomalyAtEpochDeg(double deg) { this.meanAnomalyAtEpochRad = Math.toRadians(deg); return this; }
        public Builder orbitalPeriodTicks(double v) { this.orbitalPeriodTicks = v; return this; }
        public Builder rotationPeriodTicks(double v) { this.rotationPeriodTicks = v; return this; }
        public Builder axialTiltDeg(double deg) { this.axialTiltRad = Math.toRadians(deg); return this; }
        public Builder radius(double v) { this.radius = v; return this; }

        public OrbitData build() {
            return new OrbitData(semiMajorAxis, eccentricity, inclinationRad, longitudeOfAscendingNodeRad,
                    argumentOfPeriapsisRad, meanAnomalyAtEpochRad, orbitalPeriodTicks, rotationPeriodTicks,
                    axialTiltRad, radius);
        }
    }
}
