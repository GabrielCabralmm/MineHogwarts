package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.house.door.HouseDoors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HouseDoorAttemptPacket(BlockPos pos, String text, int[] values, int extra) {

    public static void encode(HouseDoorAttemptPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.text, 128);
        buf.writeVarIntArray(packet.values);
        buf.writeVarInt(packet.extra);
    }

    public static HouseDoorAttemptPacket decode(FriendlyByteBuf buf) {
        return new HouseDoorAttemptPacket(buf.readBlockPos(), buf.readUtf(128), buf.readVarIntArray(16), buf.readVarInt());
    }

    public static void handle(HouseDoorAttemptPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            HouseDoors.attempt(player, packet.pos, packet.text, packet.values, packet.extra);
        }
        ctx.get().setPacketHandled(true);
    }
}
