package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.spell.impl.AparatacaoSpell;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ApparatePacket(AparatacaoSpell.Mode mode, int index, String name) {

    public static void encode(ApparatePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.mode);
        buf.writeVarInt(packet.index);
        buf.writeUtf(packet.name, 64);
    }

    public static ApparatePacket decode(FriendlyByteBuf buf) {
        return new ApparatePacket(buf.readEnum(AparatacaoSpell.Mode.class), buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(ApparatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            Spells.APARATACAO.request(player, packet.mode, packet.index, packet.name);
        }
        ctx.get().setPacketHandled(true);
    }
}
