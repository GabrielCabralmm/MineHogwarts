package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.shop.ShopManager;
import com.gabriel.hogwartsmod.shop.ShopNpcEntity;
import com.gabriel.hogwartsmod.shop.ShopOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record SaveShopNpcPacket(int entity, String name, String model, List<ShopOffer> offers) {

    public static void encode(SaveShopNpcPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.name, 64);
        buf.writeUtf(packet.model, 64);
        buf.writeCollection(packet.offers.subList(0, Math.min(packet.offers.size(), ShopNpcEntity.MAX_OFFERS)), (b, o) -> o.write(b));
    }

    public static SaveShopNpcPacket decode(FriendlyByteBuf buf) {
        return new SaveShopNpcPacket(buf.readVarInt(), buf.readUtf(64), buf.readUtf(64), buf.readList(ShopOffer::read));
    }

    public static void handle(SaveShopNpcPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ShopManager.save(player, packet.entity, packet.name, packet.model, packet.offers);
        }
        ctx.get().setPacketHandled(true);
    }
}
