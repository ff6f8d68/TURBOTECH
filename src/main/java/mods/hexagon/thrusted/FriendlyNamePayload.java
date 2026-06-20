package mods.hexagon.thrusted;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FriendlyNamePayload(BlockPos pos, String name, boolean add) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FriendlyNamePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, "friendly_name"));

    public static final StreamCodec<FriendlyByteBuf, FriendlyNamePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, FriendlyNamePayload::pos,
                    ByteBufCodecs.STRING_UTF8, FriendlyNamePayload::name,
                    ByteBufCodecs.BOOL, FriendlyNamePayload::add,
                    FriendlyNamePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FriendlyNamePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player == null) return;
            var level = player.level();
            if (level.getBlockEntity(payload.pos) instanceof ShieldGeneratorBlockEntity be) {
                if (payload.add) {
                    be.addFriendlyPlayerName(payload.name);
                } else {
                    be.removeFriendlyPlayerName(payload.name);
                }
            }
        });
    }
}
