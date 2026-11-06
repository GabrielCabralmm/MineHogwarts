package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DodgeMovePacket(float dx, float dy, float dz, float distance, int ticks) {

    public static void encode(DodgeMovePacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.dx);
        buf.writeFloat(packet.dy);
        buf.writeFloat(packet.dz);
        buf.writeFloat(packet.distance);
        buf.writeVarInt(packet.ticks);
    }

    public static DodgeMovePacket decode(FriendlyByteBuf buf) {
        return new DodgeMovePacket(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(DodgeMovePacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDodgeMove(packet.dx, packet.dy, packet.dz, packet.distance, packet.ticks));
        ctx.get().setPacketHandled(true);
    }
}
