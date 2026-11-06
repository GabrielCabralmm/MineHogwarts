package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.entity.ProfessorNpcEntity;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.teach.TeachingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NpcLearnPacket(int entity, String spell) {

    public static void encode(NpcLearnPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entity);
        buf.writeUtf(packet.spell, 64);
    }

    public static NpcLearnPacket decode(FriendlyByteBuf buf) {
        return new NpcLearnPacket(buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(NpcLearnPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        Spell spell = Spells.get(packet.spell);
        if (player != null && spell != null && player.level().getEntity(packet.entity) instanceof ProfessorNpcEntity npc
                && npc.distanceTo(player) <= 8) {
            TeachingManager.startNpcLesson(player, npc, spell);
        }
        ctx.get().setPacketHandled(true);
    }
}
