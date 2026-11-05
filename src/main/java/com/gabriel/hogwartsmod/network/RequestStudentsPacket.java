package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.teach.TeachingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestStudentsPacket(String spell) {

    public static void encode(RequestStudentsPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.spell, 64);
    }

    public static RequestStudentsPacket decode(FriendlyByteBuf buf) {
        return new RequestStudentsPacket(buf.readUtf(64));
    }

    public static void handle(RequestStudentsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        Spell spell = Spells.get(packet.spell);
        if (player != null && spell != null) {
            TeachingManager.sendNearby(player, spell);
        }
        ctx.get().setPacketHandled(true);
    }
}
