package mods.hexagon.thrusted.space.api;

import java.util.Map;

public record AtmosphereData(
        double pressureAtm,
        double surfaceTemperatureK,
        double radiationLevel,
        double windSpeedMs,
        boolean breathable,
        boolean corrosive,
        boolean toxic,
        Map<String, Double> composition
) {
    public static final AtmosphereData VACUUM = new AtmosphereData(
            0.0, 2.7, 1.0, 0.0, false, false, false, Map.of()
    );

    public static final AtmosphereData EARTH_LIKE = new AtmosphereData(
            1.0, 288.0, 0.0, 5.0, true, false, false,
            Map.of("nitrogen", 0.78, "oxygen", 0.21, "argon", 0.01)
    );

    public boolean hasAtmosphere() {
        return pressureAtm > 0.001;
    }

    public boolean requiresOxygen() {
        return !breathable || pressureAtm < 0.16;
    }

    public boolean requiresRadiationShielding() {
        return radiationLevel > 0.5;
    }

    public boolean requiresThermalProtection() {
        return surfaceTemperatureK < 200.0 || surfaceTemperatureK > 330.0;
    }

    public double getOxygenFraction() {
        return composition.getOrDefault("oxygen", 0.0);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double pressureAtm = 0.0;
        private double surfaceTemperatureK = 2.7;
        private double radiationLevel = 1.0;
        private double windSpeedMs = 0.0;
        private boolean breathable = false;
        private boolean corrosive = false;
        private boolean toxic = false;
        private Map<String, Double> composition = Map.of();

        public Builder pressure(double atm) { this.pressureAtm = atm; return this; }
        public Builder temperature(double kelvin) { this.surfaceTemperatureK = kelvin; return this; }
        public Builder radiation(double level) { this.radiationLevel = level; return this; }
        public Builder wind(double ms) { this.windSpeedMs = ms; return this; }
        public Builder breathable(boolean b) { this.breathable = b; return this; }
        public Builder corrosive(boolean b) { this.corrosive = b; return this; }
        public Builder toxic(boolean b) { this.toxic = b; return this; }
        public Builder composition(Map<String, Double> c) { this.composition = c; return this; }

        public AtmosphereData build() {
            return new AtmosphereData(pressureAtm, surfaceTemperatureK, radiationLevel,
                    windSpeedMs, breathable, corrosive, toxic, composition);
        }
    }
}
