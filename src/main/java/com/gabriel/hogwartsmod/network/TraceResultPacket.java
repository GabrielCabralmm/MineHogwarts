package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.teach.TeachingManager;
import com.gabriel.hogwartsmod.teach.TraceResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TraceResultPacket(String spell, TraceResult result) {

    public static void encode(TraceResultPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.spell, 64);
        buf.writeEnum(packet.result);
    }

    public static TraceResultPacket decode(FriendlyByteBuf buf) {
        return new TraceResultPacket(buf.readUtf(64), buf.readEnum(TraceResult.class));
    }

    public static void handle(TraceResultPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            TeachingManager.finish(player, packet.spell, packet.result);
        }
        ctx.get().setPacketHandled(true);
    }
}
