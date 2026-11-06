package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.entity.BroomEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BroomDismountPacket() {

    public static void encode(BroomDismountPacket packet, FriendlyByteBuf buf) {
    }

    public static BroomDismountPacket decode(FriendlyByteBuf buf) {
        return new BroomDismountPacket();
    }

    public static void handle(BroomDismountPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.getVehicle() instanceof BroomEntity) {
            player.setShiftKeyDown(false);
            player.stopRiding();
        }
        ctx.get().setPacketHandled(true);
    }
}
