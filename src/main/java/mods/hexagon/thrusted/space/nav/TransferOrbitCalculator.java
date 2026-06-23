package mods.hexagon.thrusted.space.nav;

import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.api.OrbitData;
import org.joml.Vector3d;

public class TransferOrbitCalculator {

    public record TransferResult(
            double deltaV1,
            double deltaV2,
            double totalDeltaV,
            long transferTimeTicks,
            double departureAngle,
            double arrivalAngle,
            String description
    ) {}

    public record HohmannTransfer(
            double deltaV1,
            double deltaV2,
            double totalDeltaV,
            long transferTimeTicks,
            double transferSemiMajor
    ) {}

    public static HohmannTransfer calculateHohmann(CelestialBody from, CelestialBody to) {
        if (from.getParent() == null || to.getParent() == null) return null;
        if (from.getParent() != to.getParent()) return null;

        double r1 = from.getOrbitData().semiMajorAxis();
        double r2 = to.getOrbitData().semiMajorAxis();

        if (r1 <= 0 || r2 <= 0) return null;

        double mu = 1.0;
        double aTransfer = (r1 + r2) / 2.0;

        double v1Circular = Math.sqrt(mu / r1);
        double v1Transfer = Math.sqrt(mu * (2.0 / r1 - 1.0 / aTransfer));
        double deltaV1 = Math.abs(v1Transfer - v1Circular);

        double v2Transfer = Math.sqrt(mu * (2.0 / r2 - 1.0 / aTransfer));
        double v2Circular = Math.sqrt(mu / r2);
        double deltaV2 = Math.abs(v2Circular - v2Transfer);

        double totalDeltaV = deltaV1 + deltaV2;
        double transferPeriod = 2.0 * Math.PI * Math.sqrt(aTransfer * aTransfer * aTransfer / mu);
        long transferTimeTicks = (long)(transferPeriod / 2.0);

        return new HohmannTransfer(deltaV1, deltaV2, totalDeltaV, transferTimeTicks, aTransfer);
    }

    public static TransferResult calculateTransfer(String fromName, String toName) {
        CelestialBody from = CelestialBodyRegistry.get(fromName);
        CelestialBody to = CelestialBodyRegistry.get(toName);
        if (from == null || to == null) return null;

        if (from.getParent() == to.getParent() && from.getParent() != null) {
            HohmannTransfer hohmann = calculateHohmann(from, to);
            if (hohmann == null) return null;
            return new TransferResult(
                    hohmann.deltaV1, hohmann.deltaV2, hohmann.totalDeltaV,
                    hohmann.transferTimeTicks,
                    calculateDepartureAngle(from, to),
                    0.0,
                    "Hohmann transfer from " + fromName + " to " + toName
            );
        }

        if (from.getParent() != null && from.getParent().getParent() == to.getParent()) {
            double escapeDV = from.getGravity().escapeVelocityKms() * 0.1;
            TransferResult parentTransfer = calculateTransfer(from.getParent().getName(), toName);
            if (parentTransfer == null) return null;
            return new TransferResult(
                    escapeDV + parentTransfer.deltaV1(),
                    parentTransfer.deltaV2(),
                    escapeDV + parentTransfer.totalDeltaV(),
                    parentTransfer.transferTimeTicks() + 2000L,
                    parentTransfer.departureAngle(),
                    parentTransfer.arrivalAngle(),
                    "Escape " + fromName + " -> " + parentTransfer.description()
            );
        }

        double dist = from.getDistanceTo(to);
        double approxDV = Math.sqrt(dist) * 0.5;
        long approxTime = (long)(dist * 10);
        return new TransferResult(
                approxDV * 0.6, approxDV * 0.4, approxDV,
                approxTime, 0, 0,
                "Approximate transfer from " + fromName + " to " + toName
        );
    }

    public static double calculateDepartureAngle(CelestialBody from, CelestialBody to) {
        double r1 = from.getOrbitData().semiMajorAxis();
        double r2 = to.getOrbitData().semiMajorAxis();
        double aTransfer = (r1 + r2) / 2.0;

        double transferHalfPeriod = Math.PI * Math.sqrt(aTransfer * aTransfer * aTransfer);
        double targetAngularVelocity = 2.0 * Math.PI / to.getOrbitData().orbitalPeriodTicks();
        double angleTraversed = targetAngularVelocity * transferHalfPeriod;

        return Math.PI - angleTraversed;
    }

    public static long getOptimalLaunchWindow(CelestialBody from, CelestialBody to, long currentTime) {
        double departureAngle = calculateDepartureAngle(from, to);
        Vector3d fromPos = from.getCurrentPosition();
        Vector3d toPos = to.getCurrentPosition();

        if (from.getParent() != null) {
            Vector3d parentPos = from.getParent().getCurrentPosition();
            fromPos = new Vector3d(fromPos).sub(parentPos);
        }
        if (to.getParent() != null && to.getParent() == from.getParent()) {
            Vector3d parentPos = to.getParent().getCurrentPosition();
            toPos = new Vector3d(toPos).sub(parentPos);
        }

        double currentAngle = Math.atan2(toPos.z - fromPos.z, toPos.x - fromPos.x)
                - Math.atan2(fromPos.z, fromPos.x);

        double angleDiff = departureAngle - currentAngle;
        while (angleDiff < 0) angleDiff += 2 * Math.PI;

        double synodicPeriod = calculateSynodicPeriod(from, to);
        long ticksUntilWindow = (long)(angleDiff / (2 * Math.PI) * synodicPeriod);

        return currentTime + ticksUntilWindow;
    }

    private static double calculateSynodicPeriod(CelestialBody from, CelestialBody to) {
        double p1 = from.getOrbitData().orbitalPeriodTicks();
        double p2 = to.getOrbitData().orbitalPeriodTicks();
        if (p1 == p2) return Double.MAX_VALUE;
        return Math.abs(p1 * p2 / (p2 - p1));
    }

    public static double estimateFuelRequired(double deltaV, double shipMassKg, double exhaustVelocity) {
        double massRatio = Math.exp(deltaV / exhaustVelocity);
        return shipMassKg * (massRatio - 1.0);
    }
}
