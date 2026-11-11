package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import com.gabriel.hogwartsmod.shop.ShopOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenShopPacket(int entity, String name, int kind, List<ShopOffer> offers) {

    public static void encode(OpenShopPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.name, 64);
        buf.writeVarInt(packet.kind);
        buf.writeCollection(packet.offers, (b, o) -> o.write(b));
    }

    public static OpenShopPacket decode(FriendlyByteBuf buf) {
        return new OpenShopPacket(buf.readVarInt(), buf.readUtf(64), buf.readVarInt(), buf.readList(ShopOffer::read));
    }

    public static void handle(OpenShopPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenShop(packet));
        ctx.get().setPacketHandled(true);
    }
}
