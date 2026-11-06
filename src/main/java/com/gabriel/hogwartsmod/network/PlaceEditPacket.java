package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.apparition.ApparitionPoint;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.spell.impl.AparatacaoSpell;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PlaceEditPacket(Action action, int index, String name) {
    public enum Action { MEMORIZE, RENAME, FORGET }

    public static final int MAX_NAME = 24;

    public static void encode(PlaceEditPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeVarInt(packet.index);
        buf.writeUtf(packet.name, 64);
    }

    public static PlaceEditPacket decode(FriendlyByteBuf buf) {
        return new PlaceEditPacket(buf.readEnum(Action.class), buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(PlaceEditPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        MagicData data = player == null ? null : MagicData.get(player);
        if (data != null && data.knows(Spells.APARATACAO)) {
            String name = packet.name.strip();
            if (name.length() > MAX_NAME) {
                name = name.substring(0, MAX_NAME);
            }
            switch (packet.action) {
                case MEMORIZE -> {
                    if (name.isEmpty()) {
                        break;
                    }
                    if (data.getPlaces().size() >= Spells.APARATACAO.getPlaceLimit(data)) {
                        player.displayClientMessage(Component.translatable("message.hogwartsmod.places_full"), true);
                        break;
                    }
                    data.addPlace(new ApparitionPoint(name, AparatacaoSpell.dimensionOf(player), player.getX(), player.getY(), player.getZ(), player.getYRot()));
                    player.displayClientMessage(Component.translatable("message.hogwartsmod.place_saved", name), true);
                }
                case RENAME -> {
                    if (!name.isEmpty()) {
                        data.renamePlace(packet.index, name);
                    }
                }
                case FORGET -> data.removePlace(packet.index);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
