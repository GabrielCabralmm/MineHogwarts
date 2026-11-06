package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.npc.HogwartsNpc;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RemoveNpcPacket(int entity) {

    public static void encode(RemoveNpcPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
    }

    public static RemoveNpcPacket decode(FriendlyByteBuf buf) {
        return new RemoveNpcPacket(buf.readVarInt());
    }

    public static void handle(RemoveNpcPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.hasPermissions(2) && player.level().getEntity(packet.entity) instanceof HogwartsNpc npc
                && npc.distanceTo(player) < 16) {
            String name = npc.getNpcName();
            npc.discard();
            player.displayClientMessage(Component.translatable("command.hogwartsmod.npc_removed", name).withStyle(ChatFormatting.GOLD), true);
        }
        ctx.get().setPacketHandled(true);
    }
}
