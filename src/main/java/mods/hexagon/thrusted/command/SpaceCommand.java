package mods.hexagon.thrusted.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import mods.hexagon.thrusted.space.SpaceDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class SpaceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("space")
                .then(Commands.literal("go")
                        .requires(s -> s.hasPermission(2))
                        .executes(SpaceCommand::go)));
    }

    private static int go(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
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

        player.teleportTo(target, player.getX(), -60, player.getZ(), player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("Ascending to orbit..."), true);
        return Command.SINGLE_SUCCESS;
    }
}
