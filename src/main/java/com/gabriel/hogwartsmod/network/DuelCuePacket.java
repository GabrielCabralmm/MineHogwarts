package com.gabriel.hogwartsmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DuelCuePacket(String cue, int number) {

    public static void encode(DuelCuePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.cue, 16);
        buf.writeVarInt(packet.number);
    }

    public static DuelCuePacket decode(FriendlyByteBuf buf) {
        return new DuelCuePacket(buf.readUtf(16), buf.readVarInt());
    }

    public static void handle(DuelCuePacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabriel.hogwartsmod.client.DuelCueHud.show(packet.cue, packet.number));
        ctx.get().setPacketHandled(true);
    }
}
