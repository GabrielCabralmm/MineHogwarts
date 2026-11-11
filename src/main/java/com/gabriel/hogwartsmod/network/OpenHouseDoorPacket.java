package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.house.House;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenHouseDoorPacket(BlockPos pos, House house, int a, int b, String text) {

    public static void encode(OpenHouseDoorPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeEnum(packet.house);
        buf.writeVarInt(packet.a);
        buf.writeVarInt(packet.b);
        buf.writeUtf(packet.text, 512);
    }

    public static OpenHouseDoorPacket decode(FriendlyByteBuf buf) {
        return new OpenHouseDoorPacket(buf.readBlockPos(), buf.readEnum(House.class), buf.readVarInt(), buf.readVarInt(), buf.readUtf(512));
    }

    public static void handle(OpenHouseDoorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.gabriel.hogwartsmod.client.gui.HouseDoorScreens.open(packet.pos, packet.house, packet.a, packet.b, packet.text)));
        ctx.get().setPacketHandled(true);
    }
}
