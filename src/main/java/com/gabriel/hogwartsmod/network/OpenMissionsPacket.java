package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import com.gabriel.hogwartsmod.mission.MissionView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenMissionsPacket(int entity, String name, List<MissionView> missions, int activeCount) {

    public static void encode(OpenMissionsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.name, 64);
        buf.writeCollection(packet.missions, (b, v) -> v.write(b));
        buf.writeVarInt(packet.activeCount);
    }

    public static OpenMissionsPacket decode(FriendlyByteBuf buf) {
        return new OpenMissionsPacket(buf.readVarInt(), buf.readUtf(64), buf.readList(MissionView::read), buf.readVarInt());
    }

    public static void handle(OpenMissionsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenMissions(packet));
        ctx.get().setPacketHandled(true);
    }
}
