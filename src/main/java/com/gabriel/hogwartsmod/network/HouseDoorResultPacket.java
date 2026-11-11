package com.gabriel.hogwartsmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HouseDoorResultPacket(int result) {

    public static void encode(HouseDoorResultPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.result);
    }

    public static HouseDoorResultPacket decode(FriendlyByteBuf buf) {
        return new HouseDoorResultPacket(buf.readVarInt());
    }

    public static void handle(HouseDoorResultPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.gabriel.hogwartsmod.client.gui.HouseDoorScreens.result(packet.result)));
        ctx.get().setPacketHandled(true);
    }
}
