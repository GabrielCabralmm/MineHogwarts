package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.entity.BroomEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BroomBoostPacket(boolean boosting) {

    public static void encode(BroomBoostPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.boosting);
    }

    public static BroomBoostPacket decode(FriendlyByteBuf buf) {
        return new BroomBoostPacket(buf.readBoolean());
    }

    public static void handle(BroomBoostPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.getVehicle() instanceof BroomEntity broom && broom.getControllingPassenger() == player) {
            broom.setBoostSynced(packet.boosting);
        }
        ctx.get().setPacketHandled(true);
    }
}
