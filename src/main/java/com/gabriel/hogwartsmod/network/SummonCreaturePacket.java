package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.creature.CreatureManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SummonCreaturePacket(String species) {

    public static void encode(SummonCreaturePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.species, 32);
    }

    public static SummonCreaturePacket decode(FriendlyByteBuf buf) {
        return new SummonCreaturePacket(buf.readUtf(32));
    }

    public static void handle(SummonCreaturePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.getMainHandItem().getItem() instanceof com.gabriel.hogwartsmod.creature.CreatureBagItem
                || player != null && player.getOffhandItem().getItem() instanceof com.gabriel.hogwartsmod.creature.CreatureBagItem) {
            CreatureManager.summon(player, packet.species);
        }
        ctx.get().setPacketHandled(true);
    }
}
