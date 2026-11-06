package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.item.ZoneWandItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ZoneMarkPacket(BlockPos pos, boolean cut) {

    public static void encode(ZoneMarkPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeBoolean(packet.cut);
    }

    public static ZoneMarkPacket decode(FriendlyByteBuf buf) {
        return new ZoneMarkPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(ZoneMarkPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.getMainHandItem().getItem() instanceof ZoneWandItem
                && player.distanceToSqr(packet.pos.getCenter()) < 64 * 64) {
            ZoneWandItem.markFirst(player, packet.pos, packet.cut);
        }
        ctx.get().setPacketHandled(true);
    }
}
