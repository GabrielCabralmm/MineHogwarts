package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.mission.MissionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record MissionJournalPacket(UUID npc, String mission, MissionManager.JournalAction action) {

    public static void encode(MissionJournalPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.npc);
        buf.writeUtf(packet.mission, 16);
        buf.writeEnum(packet.action);
    }

    public static MissionJournalPacket decode(FriendlyByteBuf buf) {
        return new MissionJournalPacket(buf.readUUID(), buf.readUtf(16), buf.readEnum(MissionManager.JournalAction.class));
    }

    public static void handle(MissionJournalPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            MissionManager.journal(player, packet.npc, packet.mission, packet.action);
        }
        ctx.get().setPacketHandled(true);
    }
}
