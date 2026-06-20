package mods.hexagon.thrusted.space;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.space.body.CelestialBody;
import mods.hexagon.thrusted.space.body.Planet;
import mods.hexagon.thrusted.space.dimension.PlanetDimensionManager;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class SpaceCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("space")
                .then(literal("info")
                        .executes(SpaceCommands::spaceInfo))
                .then(literal("go")
                        .executes(SpaceCommands::teleportToSpace))
                .then(literal("gospecial")
                        .then(argument("body", StringArgumentType.word())
                                .executes(SpaceCommands::teleportToBody)))
                .then(literal("orbit")
                        .executes(SpaceCommands::showOrbit))
                .then(literal("planets")
                        .executes(SpaceCommands::listPlanets))
        );
    }

    private static int spaceInfo(CommandContext<CommandSourceStack> ctx) {
        SpaceEngine engine = SpaceEngine.getInstance();
        var solarSystem = engine.getSolarSystem();
        ctx.getSource().sendSuccess(() ->
                Component.literal("§b=== Space Navigator ===§r\n" +
                        "Sun: " + solarSystem.getSun().getName() + "\n" +
                        "Planets: " + solarSystem.getPlanets().size() + "\n" +
                        "Simulation Time: " + String.format("%.1f", solarSystem.getSimulationTime()) + "s"),
                false);
        return 1;
    }

    private static int teleportToSpace(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlanetDimensionManager.transferToSpace(player);
        ctx.getSource().sendSuccess(() -> Component.literal("§7Teleported to space!"), false);
        return 1;
    }

    private static int showOrbit(CommandContext<CommandSourceStack> ctx) {
        SpaceEngine engine = SpaceEngine.getInstance();
        var planets = engine.getSolarSystem().getPlanets();
        StringBuilder sb = new StringBuilder("§7=== Orbital Data ===§r\n");
        for (Planet p : planets) {
            if (p.isMoon()) continue;
            sb.append("§e").append(p.getName()).append("§r\n");
            sb.append("  Orbital Period: ").append(String.format("%.2f", p.getOrbitalPeriod())).append(" years\n");
            sb.append("  Semi-Major Axis: ").append(String.format("%.3f", p.getSemiMajorAxis())).append(" AU\n");
            sb.append("  Eccentricity: ").append(String.format("%.4f", p.getEccentricity())).append("\n");
            sb.append("  Moons: ").append(p.getMoons().size()).append("\n");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int listPlanets(CommandContext<CommandSourceStack> ctx) {
        SpaceEngine engine = SpaceEngine.getInstance();
        var planets = engine.getSolarSystem().getPlanets();
        StringBuilder sb = new StringBuilder("§7=== Planet List ===§r\n");
        for (Planet p : planets) {
            if (p.isMoon()) continue;
            sb.append("§a").append(p.getName()).append("§r");
            if (!p.getMoons().isEmpty()) {
                sb.append(" §7[");
                for (var moon : p.getMoons()) {
                    sb.append(moon.getName()).append(" ");
                }
                sb.append("]§r");
            }
            sb.append("\n");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int teleportToBody(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String bodyName = StringArgumentType.getString(ctx, "body");

        SpaceEngine engine = SpaceEngine.getInstance();
        CelestialBody body = engine.getBodyByName(bodyName);

        if (body == null) {
            ctx.getSource().sendFailure(Component.literal("§cCelestial body '" + bodyName + "' not found!"));
            return 0;
        }

        // First ensure we're in space dimension
        if (!PlanetDimensionManager.isSpaceDimension(player.level())) {
            PlanetDimensionManager.transferToSpace(player);
        }

        // Calculate orbit position: just outside the sphere in block units
        Vec3 bodyPos = body.getPosition();
        double orbitRadius = Math.max(body.getRadius() * 0.005, 30.0);
        Vec3 orbitPos = new Vec3(bodyPos.x + orbitRadius, bodyPos.y + 50, bodyPos.z);

        ServerLevel spaceLevel = player.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "space")));
        if (spaceLevel != null) {
            player.teleportTo(spaceLevel, orbitPos.x, orbitPos.y, orbitPos.z, 0, 0);
        } else {
            player.teleportTo(orbitPos.x, orbitPos.y, orbitPos.z);
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§7Orbiting §a" + body.getName() + "§7 at §e" +
                        String.format("%.0f", orbitRadius) + "m§7 altitude"), false);
        return 1;
    }
}
