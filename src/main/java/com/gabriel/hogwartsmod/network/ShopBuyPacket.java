package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.shop.ShopManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ShopBuyPacket(int entity, String offer, int times) {

    public static void encode(ShopBuyPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.offer, 16);
        buf.writeVarInt(packet.times);
    }

    public static ShopBuyPacket decode(FriendlyByteBuf buf) {
        return new ShopBuyPacket(buf.readVarInt(), buf.readUtf(16), buf.readVarInt());
    }

    public static void handle(ShopBuyPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ShopManager.buy(player, packet.entity, packet.offer, packet.times);
        }
        ctx.get().setPacketHandled(true);
    }
}
