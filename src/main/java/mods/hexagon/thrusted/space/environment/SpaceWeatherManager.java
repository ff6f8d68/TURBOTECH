package mods.hexagon.thrusted.space.environment;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.SpaceDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = Thrusted.MODID)
public class SpaceWeatherManager {

    private static SpaceWeatherEvent currentEvent = SpaceWeatherEvent.NONE;
    private static int eventTicksRemaining = 0;
    private static int eventCooldown = 0;
    private static final Random RANDOM = new Random();

    private static final int MIN_EVENT_DURATION = 2400;
    private static final int MAX_EVENT_DURATION = 12000;
    private static final int MIN_COOLDOWN = 24000;
    private static final int MAX_COOLDOWN = 72000;
    private static final double EVENT_CHANCE_PER_CHECK = 0.02;

    public enum SpaceWeatherEvent {
        NONE("Clear", 0xFFFFFF, 0.0, 0.0),
        SOLAR_FLARE("Solar Flare", 0xFFAA00, 3.0, 0.0),
        RADIATION_STORM("Radiation Storm", 0xFF3333, 5.0, 0.0),
        METEOR_SHOWER("Meteor Shower", 0xAADDFF, 0.0, 1.0),
        MAGNETIC_STORM("Magnetic Storm", 0x8844FF, 1.5, 0.0),
        SOLAR_WIND_SURGE("Solar Wind Surge", 0xFFDD44, 0.5, 0.5),
        CME("Coronal Mass Ejection", 0xFF6600, 8.0, 0.0),
        COSMIC_RAY_BURST("Cosmic Ray Burst", 0xCC88FF, 4.0, 0.0);

        public final String displayName;
        public final int color;
        public final double radiationMultiplier;
        public final double debrisIntensity;

        SpaceWeatherEvent(String name, int color, double radiation, double debris) {
            this.displayName = name;
            this.color = color;
            this.radiationMultiplier = radiation;
            this.debrisIntensity = debris;
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        if (eventTicksRemaining > 0) {
            eventTicksRemaining--;
            applyEventEffects(server);
            if (eventTicksRemaining <= 0) {
                endEvent(server);
            }
        } else if (eventCooldown > 0) {
            eventCooldown--;
        } else {
            if (server.getTickCount() % 200 == 0) {
                if (RANDOM.nextDouble() < EVENT_CHANCE_PER_CHECK) {
                    startRandomEvent(server);
                }
            }
        }
    }

    private static void startRandomEvent(MinecraftServer server) {
        SpaceWeatherEvent[] events = SpaceWeatherEvent.values();
        SpaceWeatherEvent chosen = events[1 + RANDOM.nextInt(events.length - 1)];

        currentEvent = chosen;
        eventTicksRemaining = MIN_EVENT_DURATION + RANDOM.nextInt(MAX_EVENT_DURATION - MIN_EVENT_DURATION);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isInSpace(player)) {
                player.displayClientMessage(
                        Component.literal("\u26A0 SPACE WEATHER ALERT: " + chosen.displayName)
                                .withStyle(s -> s.withColor(chosen.color).withBold(true)), false);
                player.displayClientMessage(
                        Component.literal("  Duration: ~" + (eventTicksRemaining / 1200) + " minutes. Seek shelter!")
                                .withStyle(s -> s.withColor(0xAAAAAA)), false);
            }
        }

        Thrusted.LOGGER.info("Space weather event started: {} ({} ticks)", chosen.displayName, eventTicksRemaining);
    }

    private static void endEvent(MinecraftServer server) {
        SpaceWeatherEvent ended = currentEvent;
        currentEvent = SpaceWeatherEvent.NONE;
        eventCooldown = MIN_COOLDOWN + RANDOM.nextInt(MAX_COOLDOWN - MIN_COOLDOWN);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isInSpace(player)) {
                player.displayClientMessage(
                        Component.literal("\u2713 Space weather cleared: " + ended.displayName + " has passed.")
                                .withStyle(s -> s.withColor(0x55FF55)), false);
            }
        }
    }

    private static void applyEventEffects(MinecraftServer server) {
        if (currentEvent == SpaceWeatherEvent.NONE) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isInSpace(player)) continue;
            if (player.isCreative() || player.isSpectator()) continue;

            switch (currentEvent) {
                case SOLAR_FLARE, CME -> {
                    if (player.tickCount % 40 == 0) {
                        player.hurt(player.damageSources().onFire(), 1.0f + (float)(currentEvent.radiationMultiplier * 0.5));
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                    }
                }
                case RADIATION_STORM, COSMIC_RAY_BURST -> {
                    if (player.tickCount % 60 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                    }
                }
                case METEOR_SHOWER -> {
                    if (player.tickCount % 100 == 0 && RANDOM.nextDouble() < 0.3) {
                        Vec3 vel = new Vec3(
                                (RANDOM.nextDouble() - 0.5) * 0.5,
                                (RANDOM.nextDouble() - 0.5) * 0.5,
                                (RANDOM.nextDouble() - 0.5) * 0.5
                        );
                        player.push(vel.x, vel.y, vel.z);
                        player.displayClientMessage(
                                Component.literal("* Micro-meteorite impact!").withStyle(s -> s.withColor(0xFFAAAA)), true);
                        player.hurt(player.damageSources().flyIntoWall(), 2.0f);
                    }
                }
                case MAGNETIC_STORM -> {
                    if (player.tickCount % 80 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                    }
                }
                case SOLAR_WIND_SURGE -> {
                    if (player.tickCount % 20 == 0) {
                        double pushForce = 0.03;
                        player.push(pushForce * (RANDOM.nextDouble() - 0.5),
                                pushForce * (RANDOM.nextDouble() - 0.5),
                                pushForce * (RANDOM.nextDouble() - 0.5));
                    }
                }
                default -> {}
            }
        }
    }

    private static boolean isInSpace(Entity entity) {
        var dim = entity.level().dimension();
        return SpaceDimensions.isOrbitDimension(dim);
    }

    public static SpaceWeatherEvent getCurrentEvent() {
        return currentEvent;
    }

    public static int getEventTicksRemaining() {
        return eventTicksRemaining;
    }

    public static double getCurrentRadiationMultiplier() {
        return currentEvent.radiationMultiplier;
    }

    public static boolean isEventActive() {
        return currentEvent != SpaceWeatherEvent.NONE;
    }
}
