package mods.hexagon.thrusted.space;

import mods.hexagon.thrusted.Thrusted;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Thrusted.MODID)
public class PlayerHeightListener {

    private static final int TELEPORT_COOLDOWN_TICKS = 40;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        var dim = event.getLevel().dimension();
        boolean inOrbit = SpaceDimensions.isOrbitDimension(dim);
        entity.setNoGravity(inOrbit);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.tickCount % 5 != 0) return;

        Level level = player.level();
        ResourceKey<Level> dimKey = level.dimension();

        if (SpaceDimensions.isPlanetDimension(dimKey)) {
            if (player.getY() > SpaceDimensions.PLANET_HEIGHT) {
                ResourceKey<Level> orbitKey = SpaceDimensions.getOrbit(dimKey);
                if (orbitKey != null) {
                    teleportToDimension(player, orbitKey, player.getX(), -60, player.getZ());
                }
            }
        } else if (SpaceDimensions.isOrbitDimension(dimKey)) {
            if (player.getY() < SpaceDimensions.ORBIT_RETURN_HEIGHT) {
                ResourceKey<Level> planetKey = SpaceDimensions.getPlanet(dimKey);
                if (planetKey != null) {
                    ServerLevel planetLevel = player.server.getLevel(planetKey);
                    if (planetLevel != null) {
                        teleportToDimension(player, planetKey, player.getX(), SpaceDimensions.PLANET_HEIGHT - 10, player.getZ());
                    }
                }
            }
        }
    }

    private static void teleportToDimension(ServerPlayer player, ResourceKey<Level> targetDim,
                                              double x, double y, double z) {
        ServerLevel targetLevel = player.server.getLevel(targetDim);
        if (targetLevel == null) return;

        player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
        Thrusted.LOGGER.info("Teleported {} to {} @ [{}, {}, {}]",
                player.getName().getString(), targetDim.location(), x, y, z);
    }
}
