package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import com.gabriel.hogwartsmod.mission.Mission;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenMissionEditorPacket(int entity, String name, String model, List<Mission> missions) {

    public static void encode(OpenMissionEditorPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.name, 64);
        buf.writeUtf(packet.model, 64);
        buf.writeCollection(packet.missions, (b, m) -> m.write(b));
    }

    public static OpenMissionEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenMissionEditorPacket(buf.readVarInt(), buf.readUtf(64), buf.readUtf(64), buf.readList(Mission::read));
    }

    public static void handle(OpenMissionEditorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenMissionEditor(packet));
        ctx.get().setPacketHandled(true);
    }
}
