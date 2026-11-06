package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.zone.ZoneEditing;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ZoneEditPacket(Action action, String name, String rule, boolean value) {
    public static final int MAX_NAME = 24;

    public enum Action { CREATE, RULE, REDEFINE, REMOVE, CLEAR_SELECTION }

    public static void encode(ZoneEditPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeUtf(packet.name, 64);
        buf.writeUtf(packet.rule, 32);
        buf.writeBoolean(packet.value);
    }

    public static ZoneEditPacket decode(FriendlyByteBuf buf) {
        return new ZoneEditPacket(buf.readEnum(Action.class), buf.readUtf(64), buf.readUtf(32), buf.readBoolean());
    }

    public static void handle(ZoneEditPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ZoneEditing.handle(player, packet);
        }
        ctx.get().setPacketHandled(true);
    }
}
