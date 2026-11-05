package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CastAnimationPacket(int entityId, String animation) {

    public static void encode(CastAnimationPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeUtf(packet.animation);
    }

    public static CastAnimationPacket decode(FriendlyByteBuf buf) {
        return new CastAnimationPacket(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(CastAnimationPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCastAnimation(packet.entityId, packet.animation));
        ctx.get().setPacketHandled(true);
    }
}
