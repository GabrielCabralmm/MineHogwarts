package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import com.gabriel.hogwartsmod.shop.ShopOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenShopEditorPacket(int entity, String name, String model, List<ShopOffer> offers) {

    public static void encode(OpenShopEditorPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.name, 64);
        buf.writeUtf(packet.model, 64);
        buf.writeCollection(packet.offers, (b, o) -> o.write(b));
    }

    public static OpenShopEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenShopEditorPacket(buf.readVarInt(), buf.readUtf(64), buf.readUtf(64), buf.readList(ShopOffer::read));
    }

    public static void handle(OpenShopEditorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenShopEditor(packet));
        ctx.get().setPacketHandled(true);
    }
}
