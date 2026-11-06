package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DodgePacket(float dx, float dz) {

    public static void encode(DodgePacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.dx);
        buf.writeFloat(packet.dz);
    }

    public static DodgePacket decode(FriendlyByteBuf buf) {
        return new DodgePacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(DodgePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && Float.isFinite(packet.dx) && Float.isFinite(packet.dz)) {
            Spells.ESQUIVA.tryDodge(player, packet.dx, packet.dz);
        }
        ctx.get().setPacketHandled(true);
    }
}
