package mods.hexagon.thrusted.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.CelestialBodyRegistry;
import mods.hexagon.thrusted.space.SpaceDimensions;
import mods.hexagon.thrusted.space.environment.LifeSupportManager;
import mods.hexagon.thrusted.space.environment.SpaceWeatherManager;
import mods.hexagon.thrusted.space.nav.TransferOrbitCalculator;
import mods.hexagon.thrusted.space.resource.SpaceResource;
import mods.hexagon.thrusted.space.resource.SpaceResourceRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.stream.Stream;

public class SpaceCommand {

    private static final SuggestionProvider<CommandSourceStack> PLANET_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    CelestialBodyRegistry.getAll().stream()
                            .filter(CelestialBody::isLandable)
                            .map(b -> b.getName().toLowerCase()),
                    builder);

    private static final SuggestionProvider<CommandSourceStack> ALL_BODY_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    CelestialBodyRegistry.getAll().stream().map(CelestialBody::getName),
                    builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("space")
                .then(Commands.literal("go")
                        .requires(s -> s.hasPermission(2))
                        .executes(SpaceCommand::goOrbit)
                        .then(Commands.argument("planet", StringArgumentType.word())
                                .suggests(PLANET_SUGGESTIONS)
                                .executes(SpaceCommand::goToPlanet)))
                .then(Commands.literal("land")
                        .requires(s -> s.hasPermission(2))
                        .executes(SpaceCommand::land)
                        .then(Commands.argument("planet", StringArgumentType.word())
                                .suggests(PLANET_SUGGESTIONS)
                                .executes(SpaceCommand::landOnPlanet)))
                .then(Commands.literal("info")
                        .then(Commands.argument("body", StringArgumentType.greedyString())
                                .suggests(ALL_BODY_SUGGESTIONS)
                                .executes(SpaceCommand::bodyInfo)))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("from", StringArgumentType.word())
                                .suggests(ALL_BODY_SUGGESTIONS)
                                .then(Commands.argument("to", StringArgumentType.word())
                                        .suggests(ALL_BODY_SUGGESTIONS)
                                        .executes(SpaceCommand::transfer))))
                .then(Commands.literal("resources")
                        .then(Commands.argument("body", StringArgumentType.word())
                                .suggests(ALL_BODY_SUGGESTIONS)
                                .executes(SpaceCommand::resources)))
                .then(Commands.literal("weather")
                        .executes(SpaceCommand::weather))
                .then(Commands.literal("status")
                        .executes(SpaceCommand::status))
                .then(Commands.literal("bodies")
                        .executes(SpaceCommand::listBodies))
        );
    }

    private static int goOrbit(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Level level = player.level();
        ResourceKey<Level> dim = level.dimension();

        if (SpaceDimensions.isOrbitDimension(dim)) {
            source.sendFailure(Component.literal("You are already in orbit!"));
            return 0;
        }

        var server = player.server;
        var target = SpaceDimensions.getOrCreateOrbit(server.getLevel(Level.OVERWORLD), "earth");
        if (target == null) {
            source.sendFailure(Component.literal("Orbit dimension not available!"));
            return 0;
        }

        player.teleportTo(target, player.getX(), 64, player.getZ(), player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("\u26A1 Ascending to Earth orbit...").withStyle(s -> s.withColor(0x55AAFF)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int goToPlanet(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String planetName = StringArgumentType.getString(ctx, "planet");

        CelestialBody body = CelestialBodyRegistry.get(capitalize(planetName));
        if (body == null) {
            source.sendFailure(Component.literal("Unknown celestial body: " + planetName));
            return 0;
        }

        if (!body.isLandable()) {
            source.sendFailure(Component.literal(body.getName() + " is not landable (gas giant or star)"));
            return 0;
        }

        var server = player.server;
        ServerLevel orbitLevel = SpaceDimensions.getOrCreateOrbit(server.getLevel(Level.OVERWORLD), planetName);
        if (orbitLevel == null) {
            source.sendFailure(Component.literal("Cannot reach " + body.getName() + " orbit!"));
            return 0;
        }

        player.teleportTo(orbitLevel, 0, 64, 0, player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("\u26A1 Traveling to " + body.getName() + " orbit...").withStyle(s -> s.withColor(0x55AAFF)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int land(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        var dimKey = player.level().dimension();

        if (!SpaceDimensions.isOrbitDimension(dimKey)) {
            source.sendFailure(Component.literal("You must be in orbit to land! Use /space go first."));
            return 0;
        }

        ResourceKey<Level> planetKey = SpaceDimensions.getPlanet(dimKey);
        if (planetKey == null) {
            source.sendFailure(Component.literal("No planet associated with this orbit!"));
            return 0;
        }

        ServerLevel planetLevel = player.server.getLevel(planetKey);
        if (planetLevel == null) {
            source.sendFailure(Component.literal("Planet dimension not loaded!"));
            return 0;
        }

        player.teleportTo(planetLevel, player.getX(), 200, player.getZ(), player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("\uD83C\uDF0D Initiating descent...").withStyle(s -> s.withColor(0x55FF55)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int landOnPlanet(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String planetName = StringArgumentType.getString(ctx, "planet");

        CelestialBody body = CelestialBodyRegistry.get(capitalize(planetName));
        if (body == null) {
            source.sendFailure(Component.literal("Unknown body: " + planetName));
            return 0;
        }
        if (!body.isLandable()) {
            source.sendFailure(Component.literal(body.getName() + " is not landable!"));
            return 0;
        }

        ResourceKey<Level> planetKey = SpaceDimensions.planetKey(planetName);
        ServerLevel planetLevel = player.server.getLevel(planetKey);
        if (planetLevel == null) {
            source.sendFailure(Component.literal("Planet dimension not available!"));
            return 0;
        }

        player.teleportTo(planetLevel, 0, 200, 0, player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("\uD83C\uDF0D Landing on " + body.getName() + "...").withStyle(s -> s.withColor(0x55FF55)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int bodyInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String bodyName = StringArgumentType.getString(ctx, "body");
        CelestialBody body = CelestialBodyRegistry.get(bodyName);
        if (body == null) {
            body = CelestialBodyRegistry.get(capitalize(bodyName));
        }
        if (body == null) {
            source.sendFailure(Component.literal("Unknown body: " + bodyName));
            return 0;
        }

        CelestialBody finalBody = body;
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("=== " + finalBody.getName() + " ===").withStyle(s -> s.withColor(finalBody.getColor()).withBold(true)), false);
        source.sendSuccess(() -> Component.literal("Type: " + finalBody.getType().name()), false);
        source.sendSuccess(() -> Component.literal("Description: " + finalBody.getDescription()).withStyle(s -> s.withColor(0xAAAAAA)), false);
        source.sendSuccess(() -> Component.literal("---"), false);
        source.sendSuccess(() -> Component.literal("Gravity: " + String.format("%.4fg", finalBody.getGravity().surfaceGravityG())).withStyle(s -> s.withColor(0xAAFFAA)), false);
        source.sendSuccess(() -> Component.literal("Escape Velocity: " + String.format("%.2f km/s", finalBody.getGravity().escapeVelocityKms())).withStyle(s -> s.withColor(0xAAFFAA)), false);
        source.sendSuccess(() -> Component.literal("Temperature: " + String.format("%.0fK (%.0f\u00B0C)", finalBody.getAtmosphere().surfaceTemperatureK(), finalBody.getAtmosphere().surfaceTemperatureK() - 273.15)).withStyle(s -> s.withColor(0xFFAAAA)), false);
        source.sendSuccess(() -> Component.literal("Pressure: " + String.format("%.4f atm", finalBody.getAtmosphere().pressureAtm())).withStyle(s -> s.withColor(0xAAAAFF)), false);
        source.sendSuccess(() -> Component.literal("Radiation: " + String.format("%.2f", finalBody.getAtmosphere().radiationLevel())).withStyle(s -> s.withColor(0xFFFF88)), false);
        source.sendSuccess(() -> Component.literal("Breathable: " + (finalBody.getAtmosphere().breathable() ? "\u2713 Yes" : "\u2717 No")), false);
        source.sendSuccess(() -> Component.literal("Landable: " + (finalBody.isLandable() ? "\u2713 Yes" : "\u2717 No")), false);

        if (finalBody.getParent() != null) {
            CelestialBody parent = finalBody.getParent();
            source.sendSuccess(() -> Component.literal("Orbits: " + parent.getName()), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int transfer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String from = StringArgumentType.getString(ctx, "from");
        String to = StringArgumentType.getString(ctx, "to");

        var result = TransferOrbitCalculator.calculateTransfer(capitalize(from), capitalize(to));
        if (result == null) {
            source.sendFailure(Component.literal("Cannot calculate transfer between " + from + " and " + to));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== Transfer: " + from + " -> " + to + " ===").withStyle(s -> s.withBold(true)), false);
        source.sendSuccess(() -> Component.literal("Total Delta-V: " + String.format("%.4f", result.totalDeltaV())).withStyle(s -> s.withColor(0xAAFFAA)), false);
        source.sendSuccess(() -> Component.literal("  Departure burn: " + String.format("%.4f", result.deltaV1())), false);
        source.sendSuccess(() -> Component.literal("  Arrival burn: " + String.format("%.4f", result.deltaV2())), false);
        source.sendSuccess(() -> Component.literal("Transfer time: " + String.format("%.1f Minecraft days", result.transferTimeTicks() / 24000.0)).withStyle(s -> s.withColor(0xAAAAFF)), false);
        source.sendSuccess(() -> Component.literal(result.description()).withStyle(s -> s.withColor(0x888888)), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int resources(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String bodyName = capitalize(StringArgumentType.getString(ctx, "body"));

        CelestialBody body = CelestialBodyRegistry.get(bodyName);
        if (body == null) {
            source.sendFailure(Component.literal("Unknown body: " + bodyName));
            return 0;
        }

        List<SpaceResource> resources = SpaceResourceRegistry.getResourcesFor(bodyName);
        source.sendSuccess(() -> Component.literal("=== Resources on " + bodyName + " ===").withStyle(s -> s.withColor(body.getColor()).withBold(true)), false);

        if (resources.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No known resources.").withStyle(s -> s.withColor(0x888888)), false);
        } else {
            for (SpaceResource res : resources) {
                String rarity = res.baseRarity() > 0.5 ? "Common" : res.baseRarity() > 0.1 ? "Uncommon" : "Rare";
                source.sendSuccess(() -> Component.literal("  \u2022 " + res.displayName() + " [" + rarity + "] - " + res.category().name())
                        .withStyle(s -> s.withColor(res.color())), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int weather(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var event = SpaceWeatherManager.getCurrentEvent();

        if (SpaceWeatherManager.isEventActive()) {
            source.sendSuccess(() -> Component.literal("\u26A0 ACTIVE SPACE WEATHER: " + event.displayName)
                    .withStyle(s -> s.withColor(event.color).withBold(true)), false);
            int remaining = SpaceWeatherManager.getEventTicksRemaining();
            source.sendSuccess(() -> Component.literal("  Duration remaining: " + remaining / 20 + "s")
                    .withStyle(s -> s.withColor(0xAAAAAA)), false);
            source.sendSuccess(() -> Component.literal("  Radiation multiplier: x" + event.radiationMultiplier)
                    .withStyle(s -> s.withColor(0xFFFF88)), false);
        } else {
            source.sendSuccess(() -> Component.literal("\u2713 Space weather: CLEAR")
                    .withStyle(s -> s.withColor(0x55FF55)), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        var dimKey = player.level().dimension();
        String dimName = dimKey.location().toString();
        boolean inOrbit = SpaceDimensions.isOrbitDimension(dimKey);
        boolean onPlanet = SpaceDimensions.isPlanetDimension(dimKey);

        source.sendSuccess(() -> Component.literal("=== Space Status ===").withStyle(s -> s.withBold(true)), false);
        source.sendSuccess(() -> Component.literal("Location: " + dimName), false);
        source.sendSuccess(() -> Component.literal("In orbit: " + (inOrbit ? "Yes" : "No")), false);
        source.sendSuccess(() -> Component.literal("On planet: " + (onPlanet ? "Yes" : "No")), false);
        source.sendSuccess(() -> Component.literal(String.format("Position: %.0f, %.0f, %.0f",
                player.getX(), player.getY(), player.getZ())), false);

        var lifeSupport = LifeSupportManager.getPlayerData(player.getUUID());
        source.sendSuccess(() -> Component.literal(String.format("O2: %.0f/1200 | Radiation: %.1f/100",
                lifeSupport.oxygenLevel, lifeSupport.radiationLevel)).withStyle(s -> s.withColor(0x88FF88)), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int listBodies(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("=== Solar System Bodies ===").withStyle(s -> s.withBold(true)), false);

        for (CelestialBody body : CelestialBodyRegistry.getAll()) {
            String prefix = body.getParent() != null ? "  " : "";
            if (body.getParent() != null && body.getParent().getParent() != null) prefix = "    ";
            String landable = body.isLandable() ? " \u2713" : "";
            String finalPrefix = prefix;
            source.sendSuccess(() -> Component.literal(finalPrefix + body.getName() + " [" + body.getType().name() + "]" + landable)
                    .withStyle(s -> s.withColor(body.getColor())), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
