package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PlayerFxPacket(int entityId, Kind kind, boolean value) {

    public enum Kind { LUMOS, SHIELD, SHIELD_HIT, DODGE, APPARATE }

    public static void encode(PlayerFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeEnum(packet.kind);
        buf.writeBoolean(packet.value);
    }

    public static PlayerFxPacket decode(FriendlyByteBuf buf) {
        return new PlayerFxPacket(buf.readVarInt(), buf.readEnum(Kind.class), buf.readBoolean());
    }

    public static void handle(PlayerFxPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePlayerFx(packet.entityId, packet.kind, packet.value));
        ctx.get().setPacketHandled(true);
    }
}
