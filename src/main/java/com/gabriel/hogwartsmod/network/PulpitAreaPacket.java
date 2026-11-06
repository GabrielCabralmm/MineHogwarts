package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.block.PulpitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PulpitAreaPacket(BlockPos pos, int[] extents, boolean show) {

    public static void encode(PulpitAreaPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarIntArray(packet.extents);
        buf.writeBoolean(packet.show);
    }

    public static PulpitAreaPacket decode(FriendlyByteBuf buf) {
        return new PulpitAreaPacket(buf.readBlockPos(), buf.readVarIntArray(6), buf.readBoolean());
    }

    public static void handle(PulpitAreaPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.level().isLoaded(packet.pos) && player.distanceToSqr(packet.pos.getCenter()) < 64 * 64
                && player.level().getBlockEntity(packet.pos) instanceof PulpitBlockEntity pulpit && pulpit.canEdit(player)) {
            pulpit.setExtents(packet.extents, packet.show);
        }
        ctx.get().setPacketHandled(true);
    }
}
