package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateSlotsPacket(String[] slots) {

    public static void encode(UpdateSlotsPacket packet, FriendlyByteBuf buf) {
        for (int i = 0; i < MagicData.SLOT_COUNT; i++) {
            buf.writeUtf(packet.slots[i] == null ? "" : packet.slots[i]);
        }
    }

    public static UpdateSlotsPacket decode(FriendlyByteBuf buf) {
        String[] slots = new String[MagicData.SLOT_COUNT];
        for (int i = 0; i < MagicData.SLOT_COUNT; i++) {
            String id = buf.readUtf(64);
            slots[i] = id.isEmpty() ? null : id;
        }
        return new UpdateSlotsPacket(slots);
    }

    public static void handle(UpdateSlotsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && !player.isUsingItem()) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                for (int i = 0; i < MagicData.SLOT_COUNT; i++) {
                    Spell spell = Spells.get(packet.slots[i]);
                    data.setSlot(i, spell != null && data.knows(spell) ? spell : null);
                }
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
