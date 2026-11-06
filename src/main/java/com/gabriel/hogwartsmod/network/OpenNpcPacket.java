package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenNpcPacket(int entity, String name, List<String> spells) {

    public static void encode(OpenNpcPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.name, 64);
        buf.writeCollection(packet.spells, (b, s) -> b.writeUtf(s, 64));
    }

    public static OpenNpcPacket decode(FriendlyByteBuf buf) {
        return new OpenNpcPacket(buf.readVarInt(), buf.readUtf(64), buf.readList(b -> b.readUtf(64)));
    }

    public static void handle(OpenNpcPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenNpc(packet.entity, packet.name, packet.spells));
        ctx.get().setPacketHandled(true);
    }
}
