package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.mission.MissionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MissionActionPacket(int entity, String mission, MissionManager.Action action) {

    public static void encode(MissionActionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.mission, 16);
        buf.writeEnum(packet.action);
    }

    public static MissionActionPacket decode(FriendlyByteBuf buf) {
        return new MissionActionPacket(buf.readVarInt(), buf.readUtf(16), buf.readEnum(MissionManager.Action.class));
    }

    public static void handle(MissionActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            MissionManager.action(player, packet.entity, packet.mission, packet.action);
        }
        ctx.get().setPacketHandled(true);
    }
}
