package mods.hexagon.thrusted.space.environment;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.SpaceDimensions;
import mods.hexagon.thrusted.space.api.AtmosphereData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Thrusted.MODID)
public class LifeSupportManager {

    private static final Map<UUID, PlayerLifeSupport> PLAYER_DATA = new HashMap<>();

    private static final double MAX_OXYGEN = 1200.0;
    private static final double OXYGEN_USE_RATE = 1.0;
    private static final double RADIATION_ACCUMULATE_RATE = 0.5;
    private static final double RADIATION_DECAY_RATE = 0.1;
    private static final double MAX_RADIATION = 100.0;
    private static final double RADIATION_DAMAGE_THRESHOLD = 60.0;
    private static final double TEMPERATURE_DAMAGE_THRESHOLD_LOW = 150.0;
    private static final double TEMPERATURE_DAMAGE_THRESHOLD_HIGH = 350.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        Level level = player.level();
        var dimKey = level.dimension();

        if (!SpaceDimensions.isOrbitDimension(dimKey) && !SpaceDimensions.isPlanetDimension(dimKey)) {
            if (dimKey == Level.OVERWORLD) return;
            return;
        }

        AtmosphereData atmosphere = getAtmosphereForDimension(dimKey.location().getPath());
        PlayerLifeSupport data = PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new PlayerLifeSupport());

        if (player.isCreative() || player.isSpectator()) return;

        processOxygen(player, atmosphere, data);
        processRadiation(player, atmosphere, data);
        processTemperature(player, atmosphere, data);
        processPressure(player, atmosphere, data);
    }

    private static void processOxygen(ServerPlayer player, AtmosphereData atmo, PlayerLifeSupport data) {
        if (atmo.requiresOxygen()) {
            double suitOxygen = getSuitOxygenSupply(player);
            if (suitOxygen > 0) {
                data.oxygenLevel = Math.min(MAX_OXYGEN, data.oxygenLevel + suitOxygen);
            }
            data.oxygenLevel -= OXYGEN_USE_RATE;

            if (data.oxygenLevel <= 0) {
                data.oxygenLevel = 0;
                player.hurt(player.damageSources().drown(), 2.0f);
                if (data.warningCooldown <= 0) {
                    player.displayClientMessage(
                            Component.literal("\u26A0 OXYGEN CRITICAL - SUFFOCATING").withStyle(s -> s.withColor(0xFF3333)), true);
                    data.warningCooldown = 60;
                }
            } else if (data.oxygenLevel < MAX_OXYGEN * 0.2 && data.warningCooldown <= 0) {
                player.displayClientMessage(
                        Component.literal("\u26A0 Oxygen Low: " + (int)(data.oxygenLevel / MAX_OXYGEN * 100) + "%")
                                .withStyle(s -> s.withColor(0xFFAA00)), true);
                data.warningCooldown = 100;
            }
        } else {
            data.oxygenLevel = MAX_OXYGEN;
        }
        data.warningCooldown = Math.max(0, data.warningCooldown - 1);
    }

    private static void processRadiation(ServerPlayer player, AtmosphereData atmo, PlayerLifeSupport data) {
        double radiationExposure = atmo.radiationLevel();
        double shielding = getRadiationShielding(player);
        double netRadiation = radiationExposure * (1.0 - shielding);

        data.radiationLevel += netRadiation * RADIATION_ACCUMULATE_RATE;
        data.radiationLevel -= RADIATION_DECAY_RATE;
        data.radiationLevel = Math.max(0, Math.min(MAX_RADIATION, data.radiationLevel));

        if (data.radiationLevel > RADIATION_DAMAGE_THRESHOLD) {
            double severity = (data.radiationLevel - RADIATION_DAMAGE_THRESHOLD) / (MAX_RADIATION - RADIATION_DAMAGE_THRESHOLD);
            if (severity > 0.3) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, (int)(severity * 2)));
            }
            if (severity > 0.6) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
            }
            if (severity > 0.9) {
                player.hurt(player.damageSources().magic(), 3.0f);
            }
        }
    }

    private static void processTemperature(ServerPlayer player, AtmosphereData atmo, PlayerLifeSupport data) {
        double temp = atmo.surfaceTemperatureK();
        double thermalProtection = getThermalProtection(player);

        double effectiveTemp = temp + (288.0 - temp) * thermalProtection;

        if (effectiveTemp < TEMPERATURE_DAMAGE_THRESHOLD_LOW) {
            double severity = (TEMPERATURE_DAMAGE_THRESHOLD_LOW - effectiveTemp) / TEMPERATURE_DAMAGE_THRESHOLD_LOW;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, (int)(severity * 2)));
            if (severity > 0.5) {
                player.hurt(player.damageSources().freeze(), 1.5f * (float)severity);
            }
        } else if (effectiveTemp > TEMPERATURE_DAMAGE_THRESHOLD_HIGH) {
            double severity = (effectiveTemp - TEMPERATURE_DAMAGE_THRESHOLD_HIGH) / 500.0;
            if (severity > 0.3) {
                player.hurt(player.damageSources().onFire(), 2.0f * (float)severity);
            }
        }
    }

    private static void processPressure(ServerPlayer player, AtmosphereData atmo, PlayerLifeSupport data) {
        double pressure = atmo.pressureAtm();
        double suitProtection = getPressureProtection(player);

        if (pressure < 0.01 && suitProtection < 0.5) {
            player.hurt(player.damageSources().magic(), 1.0f);
        }
        if (pressure > 10.0 && suitProtection < 0.8) {
            double crushForce = (pressure - 10.0) / 100.0;
            player.hurt(player.damageSources().cramming(), (float)(crushForce * 4.0));
        }
    }

    private static double getSuitOxygenSupply(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return 0;
        CustomData customData = chest.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("SpaceSuit")) {
                return tag.getCompound("SpaceSuit").getDouble("OxygenSupplyRate");
            }
        }
        return 0;
    }

    private static double getRadiationShielding(ServerPlayer player) {
        double total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("SpaceSuit")) {
                    total += tag.getCompound("SpaceSuit").getDouble("RadiationShielding");
                }
            }
        }
        return Math.min(1.0, total);
    }

    private static double getThermalProtection(ServerPlayer player) {
        double total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("SpaceSuit")) {
                    total += tag.getCompound("SpaceSuit").getDouble("ThermalProtection");
                }
            }
        }
        return Math.min(1.0, total);
    }

    private static double getPressureProtection(ServerPlayer player) {
        double total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("SpaceSuit")) {
                    total += tag.getCompound("SpaceSuit").getDouble("PressureProtection");
                }
            }
        }
        return Math.min(1.0, total);
    }

    private static AtmosphereData getAtmosphereForDimension(String dimPath) {
        String bodyName = dimPath.replace("planet_", "").replace("orbit_", "");
        if (dimPath.startsWith("orbit_")) {
            return AtmosphereData.VACUUM;
        }
        CelestialBody body = CelestialBodyRegistry.get(capitalize(bodyName));
        if (body != null) return body.getAtmosphere();
        return AtmosphereData.VACUUM;
    }

    public static PlayerLifeSupport getPlayerData(UUID uuid) {
        return PLAYER_DATA.computeIfAbsent(uuid, k -> new PlayerLifeSupport());
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static class PlayerLifeSupport {
        public double oxygenLevel = MAX_OXYGEN;
        public double radiationLevel = 0;
        public int warningCooldown = 0;
    }
}
