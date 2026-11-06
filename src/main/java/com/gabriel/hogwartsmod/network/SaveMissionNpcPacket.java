package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.mission.Mission;
import com.gabriel.hogwartsmod.mission.MissionManager;
import com.gabriel.hogwartsmod.mission.MissionNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record SaveMissionNpcPacket(int entity, String name, String model, List<Mission> missions) {

    public static void encode(SaveMissionNpcPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.name, 64);
        buf.writeUtf(packet.model, 64);
        buf.writeCollection(packet.missions.subList(0, Math.min(packet.missions.size(), MissionNpcEntity.MAX_MISSIONS)), (b, m) -> m.write(b));
    }

    public static SaveMissionNpcPacket decode(FriendlyByteBuf buf) {
        return new SaveMissionNpcPacket(buf.readVarInt(), buf.readUtf(64), buf.readUtf(64), buf.readList(Mission::read));
    }

    public static void handle(SaveMissionNpcPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            MissionManager.save(player, packet.entity, packet.name, packet.model, packet.missions);
        }
        ctx.get().setPacketHandled(true);
    }
}
