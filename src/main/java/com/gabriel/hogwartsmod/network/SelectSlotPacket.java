package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SelectSlotPacket(int slot) {

    public static void encode(SelectSlotPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.slot);
    }

    public static SelectSlotPacket decode(FriendlyByteBuf buf) {
        return new SelectSlotPacket(buf.readVarInt());
    }

    public static void handle(SelectSlotPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && !player.isUsingItem()) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                data.setActiveSlot(packet.slot);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
