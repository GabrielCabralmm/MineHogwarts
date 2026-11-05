package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenTracePacket(String spell, String teacher, int attempts) {

    public static void encode(OpenTracePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.spell, 64);
        buf.writeUtf(packet.teacher, 64);
        buf.writeVarInt(packet.attempts);
    }

    public static OpenTracePacket decode(FriendlyByteBuf buf) {
        return new OpenTracePacket(buf.readUtf(64), buf.readUtf(64), buf.readVarInt());
    }

    public static void handle(OpenTracePacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenTrace(packet.spell, packet.teacher, packet.attempts));
        ctx.get().setPacketHandled(true);
    }
}
