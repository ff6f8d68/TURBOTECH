package mods.hexagon.thrusted.space.environment;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.SpaceDimensions;
import mods.hexagon.thrusted.space.api.AtmosphereData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Thrusted.MODID)
public class ReentryManager {

    private static final Map<UUID, ReentryState> REENTRY_STATES = new HashMap<>();
    private static final double REENTRY_SPEED_THRESHOLD = 0.8;
    private static final double HEAT_ACCUMULATION_RATE = 2.0;
    private static final double HEAT_DISSIPATION_RATE = 0.5;
    private static final double MAX_HEAT = 100.0;
    private static final double DAMAGE_HEAT_THRESHOLD = 60.0;
    private static final int REENTRY_ALTITUDE_START = 350;
    private static final int REENTRY_ALTITUDE_END = 250;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        Level level = player.level();
        var dimKey = level.dimension();

        if (!SpaceDimensions.isPlanetDimension(dimKey) && dimKey != Level.OVERWORLD) return;

        String planetName = dimKey == Level.OVERWORLD ? "earth" :
                dimKey.location().getPath().replace("planet_", "");

        CelestialBody body = CelestialBodyRegistry.get(capitalize(planetName));
        if (body == null) return;

        AtmosphereData atmo = body.getAtmosphere();
        if (!atmo.hasAtmosphere()) return;

        double altitude = player.getY();
        double speed = player.getDeltaMovement().length();
        UUID uuid = player.getUUID();

        ReentryState state = REENTRY_STATES.computeIfAbsent(uuid, k -> new ReentryState());

        if (altitude > REENTRY_ALTITUDE_END && altitude < REENTRY_ALTITUDE_START && speed > REENTRY_SPEED_THRESHOLD) {
            double altitudeFactor = 1.0 - (altitude - REENTRY_ALTITUDE_END) / (REENTRY_ALTITUDE_START - REENTRY_ALTITUDE_END);
            double heatGain = speed * HEAT_ACCUMULATION_RATE * altitudeFactor * atmo.pressureAtm();
            state.heatLevel += heatGain;
            state.heatLevel = Math.min(MAX_HEAT, state.heatLevel);
            state.isReentering = true;

            if (level instanceof ServerLevel serverLevel) {
                spawnReentryParticles(serverLevel, player);
            }

            if (state.heatLevel > DAMAGE_HEAT_THRESHOLD) {
                double damageRatio = (state.heatLevel - DAMAGE_HEAT_THRESHOLD) / (MAX_HEAT - DAMAGE_HEAT_THRESHOLD);
                double heatShielding = getHeatShielding(player);
                double effectiveDamage = damageRatio * (1.0 - heatShielding);

                if (effectiveDamage > 0.1) {
                    player.hurt(player.damageSources().onFire(), (float)(effectiveDamage * 4.0));
                }

                if (player.tickCount % 20 == 0) {
                    int heatPercent = (int)(state.heatLevel / MAX_HEAT * 100);
                    int color = heatPercent > 80 ? 0xFF3333 : heatPercent > 50 ? 0xFFAA00 : 0xFFDD00;
                    player.displayClientMessage(
                            Component.literal("\uD83D\uDD25 RE-ENTRY HEAT: " + heatPercent + "% | Speed: " + String.format("%.1f", speed * 20) + " m/s")
                                    .withStyle(s -> s.withColor(color)), true);
                }
            }
        } else {
            state.heatLevel -= HEAT_DISSIPATION_RATE;
            state.heatLevel = Math.max(0, state.heatLevel);
            if (state.heatLevel <= 0) {
                state.isReentering = false;
            }
        }
    }

    private static void spawnReentryParticles(ServerLevel level, ServerPlayer player) {
        Vec3 pos = player.position();
        Vec3 motion = player.getDeltaMovement();
        for (int i = 0; i < 5; i++) {
            double ox = (Math.random() - 0.5) * 2;
            double oy = (Math.random() - 0.5) * 2;
            double oz = (Math.random() - 0.5) * 2;
            level.sendParticles(ParticleTypes.FLAME,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    1, -motion.x * 0.5, -motion.y * 0.5, -motion.z * 0.5, 0.1);
        }
        for (int i = 0; i < 3; i++) {
            double ox = (Math.random() - 0.5) * 3;
            double oy = (Math.random() - 0.5) * 3;
            double oz = (Math.random() - 0.5) * 3;
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    1, -motion.x * 0.3, -motion.y * 0.3, -motion.z * 0.3, 0.05);
        }
    }

    private static double getHeatShielding(ServerPlayer player) {
        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (chest.isEmpty()) return 0;
        net.minecraft.world.item.component.CustomData customData = chest.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            if (tag.contains("SpaceSuit")) {
                return tag.getCompound("SpaceSuit").getDouble("HeatShielding");
            }
        }
        return 0;
    }

    public static ReentryState getState(UUID uuid) {
        return REENTRY_STATES.getOrDefault(uuid, new ReentryState());
    }

    public static boolean isReentering(UUID uuid) {
        ReentryState state = REENTRY_STATES.get(uuid);
        return state != null && state.isReentering;
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static class ReentryState {
        public double heatLevel = 0;
        public boolean isReentering = false;
    }
}
