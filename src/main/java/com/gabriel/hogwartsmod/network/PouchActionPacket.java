package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.money.Coin;
import com.gabriel.hogwartsmod.money.PouchActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PouchActionPacket(InteractionHand hand, PouchActions.Action action, Coin coin, int amount) {

    public static void encode(PouchActionPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.hand);
        buf.writeEnum(packet.action);
        buf.writeEnum(packet.coin);
        buf.writeVarInt(packet.amount);
    }

    public static PouchActionPacket decode(FriendlyByteBuf buf) {
        return new PouchActionPacket(buf.readEnum(InteractionHand.class), buf.readEnum(PouchActions.Action.class), buf.readEnum(Coin.class), buf.readVarInt());
    }

    public static void handle(PouchActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            PouchActions.handle(player, packet.hand, packet.action, packet.coin, packet.amount);
        }
        ctx.get().setPacketHandled(true);
    }
}
